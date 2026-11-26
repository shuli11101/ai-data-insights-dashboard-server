package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.mapper.ai.AiMarketStatMapper;
import com.aidata.insights.vo.ai.AiCategoryCountVO;
import com.aidata.insights.vo.ai.AiMarketSnapshot;
import com.aidata.insights.vo.ai.AiSalaryQuartileVO;
import com.aidata.insights.vo.ai.AiSkillHeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 行情数据注入：把 job_positions_v2 的聚合结果拼成一段事实文本，交给模型只做转述
 *
 * <p>注入口径按四级兜底，口径名见 {@link AiMarketSnapshot} 的 LEVEL_* 常量：
 * 城市+方向 → 全国+方向 → 仅城市 → 不注入。</p>
 *
 * <p>之所以需要「全国+方向」这一级：库内只有 15 个城市且各城市抓取的方向极不均衡，
 * 例如深圳 329 条岗位里数据方向只有 2 条、北京 360 条里前端只有 1 条，
 * 只做「城市+方向」会让大量提问拿不到数据。</p>
 *
 * <p>统计结果按「城市 + 方向」缓存，用户维度的信息（已掌握技能、期望薪资、职级）
 * 不进缓存，每次请求实时拼接。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMarketContextService {

    /** 岗位数阈值，低于该值不出数字结论 */
    private static final int MIN_SAMPLE = 30;

    /** 薪资样本阈值，低于该值不输出分位数 */
    private static final int MIN_SALARY_SAMPLE = 10;

    /** 热门技能取前多少个 */
    private static final int SKILL_LIMIT = 12;

    /** 城市岗位结构取前多少个 */
    private static final int CATEGORY_LIMIT = 8;

    /** 行情快照缓存有效期 */
    private static final Duration CACHE_TTL = Duration.ofSeconds(RedisKeyConstant.AI_MARKET_TTL);

    /** 城市词典缓存有效期（毫秒），12 小时 */
    private static final long CITY_CACHE_TTL_MILLIS = 12 * 60 * 60 * 1000L;

    /**
     * 只有这些方向才注入热门技能
     *
     * <p>非技术岗的 skill_tags 抓的是「品类 + 渠道 + 岗位细分」（如家用电器、3c数码、
     * 店铺站内推广），靠黑名单永远补不完；而 skill 场景本身是「技术栈评估」，
     * 与其给一堆行业词，不如不给，让模型用常识补。</p>
     */
    private static final Set<String> TECH_CATEGORIES = Set.of(
            "后端开发", "前端/移动开发", "数据", "测试",
            "人工智能", "运维/技术支持", "电子/硬件开发", "高端技术职位");

    /**
     * 方向别名词典：key 为别名，value 必须是 job_positions_v2.l2 的真实取值
     *
     * <p>按 key 长度降序匹配，保证「自动化测试」优先于「自动化」、「javascript」优先于「java」、
     * 「产品运营」优先于「产品」这类长短词冲突。只收录 ≥30 条的 21 个分类。</p>
     */
    private static final Map<String, String> CATEGORY_ALIASES = buildAliases();

    /**
     * 技能组合反推方向：skills 全部出现在输入中才算命中
     *
     * <p>组合比单技能准得多：python 单独出现时 64.4% 落在后端，但 python+sql 有 83.3%
     * 是数据岗、python+django 有 89.8% 是后端岗。每条规则的注释是 job_positions_v2
     * 的实测占比，数据量增长后建议复测。</p>
     */
    private static final List<ComboRule> COMBO_RULES = buildComboRules();

    /**
     * 单技能映射：每个技能取其占比最大的方向，只收 ≥60% 的技能
     *
     * <p>用于组合规则没命中时的兜底。命中多个技能且方向不一致时不反推，
     * 宁可落回 CITY_ONLY 也不猜。</p>
     */
    private static final Map<String, String> SKILL_CATEGORY = buildSkillCategories();

    /**
     * 技能展示名：数据里统一存小写（便于去重统计），拼注入文本时还原成标准写法
     *
     * <p>未收录的技能按原样输出，不影响功能。后续技能独立成表后，这份映射会由
     * job_skill.name 取代。</p>
     */
    private static final Map<String, String> SKILL_DISPLAY_NAMES = buildDisplayNames();

    /** 无可用数据时的固定文本：明确告诉模型不许编数字 */
    private static final String NO_DATA_TEXT = """
            行情数据：平台暂无可用的样本（未识别到城市或岗位方向，或样本不足 30 条）。
            请基于行业常识给出方向性判断，不要给出精确薪资数字，也不要声称这是平台统计结果。""";

    private final AiMarketStatMapper aiMarketStatMapper;

    private final RedisService redisService;

    /** 城市词典：库内实际有岗位的城市，不写死，爬虫扩城后自动生效 */
    private volatile List<String> cityCache = List.of();

    /** 城市词典上次加载时间（毫秒） */
    private volatile long cityCacheLoadedAt = 0L;

    /**
     * 根据用户输入生成注入文本
     *
     * @param userText 本轮用户输入原文，skill 场景即输入框内容
     * @return 可直接拼到用户消息末尾的「补充信息」，永不为 null
     */
    public String build(String userText) {
        if (!StringUtils.hasText(userText)) {
            return NO_DATA_TEXT;
        }

        String city = extractCity(userText);
        CategoryMatch match = resolveCategory(userText);
        String category = match.category();
        String key = RedisKeyConstant.aiMarket(city, category);

        AiMarketSnapshot snapshot = redisService.get(key, AiMarketSnapshot.class).orElse(null);
        boolean cacheHit = snapshot != null;
        if (!cacheHit) {
            snapshot = load(city, category);
            try {
                redisService.set(key, snapshot, CACHE_TTL);
            } catch (RuntimeException e) {
                // 缓存只是优化，写失败不影响本轮注入
                log.warn("行情快照缓存写入失败, 跳过缓存: key={}", key, e);
            }
        }

        String text = render(snapshot, userText);
        log.info("行情注入 city={} category={} source={} level={} sample={} cache={}",
                city, category, match.source(), snapshot.getLevel(), snapshot.getSampleSize(),
                cacheHit ? "hit" : "miss");
        return text;
    }

    /**
     * 按四级兜底取数，返回纯统计数据（不含用户信息）
     */
    private AiMarketSnapshot load(String city, String category) {
        // 本地「城市+方向」的样本数，L2 时用于提示本地不足
        Long localSample = null;

        if (city != null && category != null) {
            localSample = aiMarketStatMapper.countJobs(city, category);
            if (localSample >= MIN_SAMPLE) {
                return snapshot(AiMarketSnapshot.LEVEL_CITY_DIRECTION, city, category, localSample, localSample);
            }
        }
        if (category != null) {
            long nationSample = aiMarketStatMapper.countJobs(null, category);
            if (nationSample >= MIN_SAMPLE) {
                return snapshot(AiMarketSnapshot.LEVEL_NATION_DIRECTION, city, category, nationSample, localSample);
            }
        }
        if (city != null) {
            long citySample = aiMarketStatMapper.countJobs(city, null);
            if (citySample >= MIN_SAMPLE) {
                return snapshot(AiMarketSnapshot.LEVEL_CITY_ONLY, city, null, citySample, citySample);
            }
        }
        return snapshot(AiMarketSnapshot.LEVEL_NONE, city, category, 0L, localSample);
    }

    /**
     * 按口径补齐薪资、技能与城市结构
     *
     * <p>注意 NATION_DIRECTION 的薪资与技能也必须取全国口径（city 传 null），
     * 否则会出现「样本是全国 215 条，数字取自本地 2 条」的自相矛盾。</p>
     *
     * <p>技能只在「方向是技术类」时注入：CITY_ONLY 没有方向，非技术方向又只有
     * 品类词，两者都不适合输出技能清单。</p>
     */
    private AiMarketSnapshot snapshot(String level, String city, String category,
                                      long sampleSize, Long localSample) {
        AiMarketSnapshot snapshot = new AiMarketSnapshot();
        snapshot.setLevel(level);
        snapshot.setCity(city);
        snapshot.setCategory(category);
        snapshot.setSampleSize(sampleSize);
        snapshot.setCitySampleSize(localSample);

        boolean withSkills = category != null && TECH_CATEGORIES.contains(category);
        if (AiMarketSnapshot.LEVEL_CITY_DIRECTION.equals(level)) {
            snapshot.setSalaryQuartiles(aiMarketStatMapper.selectSalaryQuartiles(city, category));
            if (withSkills) {
                snapshot.setSkills(loadSkills(city, category));
            }
        } else if (AiMarketSnapshot.LEVEL_NATION_DIRECTION.equals(level)) {
            snapshot.setSalaryQuartiles(aiMarketStatMapper.selectSalaryQuartiles(null, category));
            if (withSkills) {
                snapshot.setSkills(loadSkills(null, category));
            }
        } else if (AiMarketSnapshot.LEVEL_CITY_ONLY.equals(level)) {
            snapshot.setSalaryQuartiles(aiMarketStatMapper.selectSalaryQuartiles(city, null));
            snapshot.setCategories(aiMarketStatMapper.selectCityCategories(city, CATEGORY_LIMIT));
        }
        return snapshot;
    }

    /**
     * 拼成注入文本：统计部分来自快照，用户部分每次实时计算
     */
    private String render(AiMarketSnapshot snapshot, String userText) {
        if (AiMarketSnapshot.LEVEL_NONE.equals(snapshot.getLevel())) {
            return NO_DATA_TEXT;
        }

        StringBuilder sb = new StringBuilder("行情数据（只允许引用以下数字，不得换算、不得补充未提供的统计量）：\n");

        if (AiMarketSnapshot.LEVEL_CITY_DIRECTION.equals(snapshot.getLevel())) {
            sb.append("- 口径：").append(snapshot.getCity()).append(" · ").append(snapshot.getCategory())
                    .append("，平台在库岗位 ").append(snapshot.getSampleSize()).append(" 条\n");
        } else if (AiMarketSnapshot.LEVEL_NATION_DIRECTION.equals(snapshot.getLevel())) {
            sb.append("- 口径：全国 · ").append(snapshot.getCategory())
                    .append("，平台在库岗位 ").append(snapshot.getSampleSize()).append(" 条\n");
            if (snapshot.getCity() != null) {
                long local = snapshot.getCitySampleSize() == null ? 0L : snapshot.getCitySampleSize();
                sb.append("- 注意：").append(snapshot.getCity()).append("本地该方向样本仅 ")
                        .append(local).append(" 条，不足以下结论，以下为全国口径\n");
            }
        } else {
            sb.append("- 口径：").append(snapshot.getCity()).append("整体，平台在库岗位 ")
                    .append(snapshot.getSampleSize())
                    .append(" 条；未识别到明确的岗位方向，以下为该城市整体情况\n");
            if (snapshot.getCategories() != null && !snapshot.getCategories().isEmpty()) {
                sb.append("- 岗位结构（括号内为岗位数）：").append(joinCategories(snapshot.getCategories())).append('\n');
            }
        }

        String salary = renderSalary(snapshot.getSalaryQuartiles());
        if (salary != null) {
            sb.append("- 薪资：").append(salary).append('\n');
        }
        if (snapshot.getSkills() != null && !snapshot.getSkills().isEmpty()) {
            sb.append("- 热门技能（括号内为要求该技能的岗位数）：")
                    .append(joinSkills(snapshot.getSkills())).append('\n');
        }

        // 方向不明时技能比对没有意义，只在有明确方向时追加用户维度
        if (!AiMarketSnapshot.LEVEL_CITY_ONLY.equals(snapshot.getLevel()) && snapshot.getSkills() != null) {
            appendUserPart(sb, snapshot.getSkills(), userText);
        }
        return sb.toString().trim();
    }

    /**
     * 追加用户维度信息：已掌握技能、缺口技能、期望薪资、职级参考
     */
    private void appendUserPart(StringBuilder sb, List<AiSkillHeatVO> skills, String userText) {
        String lower = userText.toLowerCase(Locale.ROOT);
        List<String> owned = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (AiSkillHeatVO skill : skills) {
            if (matches(lower, skill.getSkillTag())) {
                owned.add(displayName(skill.getSkillTag()));
            } else {
                missing.add(displayName(skill.getSkillTag()));
            }
        }
        if (!owned.isEmpty()) {
            sb.append("- 用户已掌握（与高频技能重合）：").append(String.join("、", owned)).append('\n');
        }
        if (!missing.isEmpty()) {
            sb.append("- 用户未提及的高频技能：").append(String.join("、", missing)).append('\n');
        }
        Integer expect = extractExpectSalary(userText);
        if (expect != null) {
            sb.append("- 用户期望薪资：").append(expect).append("K\n");
        }
        sb.append("- 用户职级参考：").append(guessLevel(userText)).append('\n');
    }

    /**
     * 薪资描述：中位数 + 中间 50% 区间；样本不足时返回 null，让模型不给数字
     *
     * <p>MySQL 没有 PERCENTILE_CONT，这里用 NTILE(4) 的箱边界近似：
     * P25 取第 1 箱上限，P75 取第 3 箱上限与第 4 箱下限的中点。</p>
     */
    private String renderSalary(List<AiSalaryQuartileVO> quartiles) {
        if (quartiles == null || quartiles.size() < 4) {
            return null;
        }
        long total = quartiles.stream()
                .mapToLong(q -> q.getSampleCount() == null ? 0L : q.getSampleCount())
                .sum();
        if (total < MIN_SALARY_SAMPLE) {
            return null;
        }
        AiSalaryQuartileVO q1 = quartiles.get(0);
        AiSalaryQuartileVO q2 = quartiles.get(1);
        AiSalaryQuartileVO q3 = quartiles.get(2);
        AiSalaryQuartileVO q4 = quartiles.get(3);

        double p25 = round(q1.getMaxMid());
        double p50 = round((safe(q2.getMaxMid()) + safe(q3.getMinMid())) / 2);
        double p75 = round((safe(q3.getMaxMid()) + safe(q4.getMinMid())) / 2);

        return String.format("中位数约 %.1fK，中间 50%% 区间 %.1fK~%.1fK（已排除实习与应届，样本 %d 条）",
                p50, p25, p75, total);
    }

    private String joinSkills(List<AiSkillHeatVO> skills) {
        List<String> parts = new ArrayList<>();
        for (AiSkillHeatVO skill : skills) {
            parts.add(displayName(skill.getSkillTag()) + "(" + skill.getHotCount() + ")");
        }
        return String.join("、", parts);
    }

    private String joinCategories(List<AiCategoryCountVO> categories) {
        List<String> parts = new ArrayList<>();
        for (AiCategoryCountVO category : categories) {
            parts.add(category.getCategory() + "(" + category.getJobCount() + ")");
        }
        return String.join("、", parts);
    }

    /**
     * 技能是否出现在用户输入中：英文按词边界匹配，避免 java 命中 javascript；中文直接包含
     */
    private boolean matches(String lowerText, String tag) {
        if (!StringUtils.hasText(tag)) {
            return false;
        }
        // 字典版返回的是标准名（Java / Power BI），统一转小写后再比对
        String lowerTag = tag.toLowerCase(Locale.ROOT);
        if (lowerTag.chars().anyMatch(c -> c > 127)) {
            return lowerText.contains(lowerTag);
        }
        Pattern pattern = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(lowerTag) + "(?![a-z0-9])");
        return pattern.matcher(lowerText).find();
    }

    /**
     * 抽取城市：取输入中出现位置最靠前的那个，同一位置取更长的城市名
     */
    private String extractCity(String text) {
        String matched = null;
        int earliest = Integer.MAX_VALUE;
        for (String city : cities()) {
            int index = text.indexOf(city);
            if (index < 0) {
                continue;
            }
            if (index < earliest || (index == earliest && matched != null && city.length() > matched.length())) {
                earliest = index;
                matched = city;
            }
        }
        return matched;
    }

    /**
     * 城市词典：取库内实际有岗位的城市，进程内缓存 5 分钟
     *
     * <p>不写死城市名单，爬虫扩城后无需改代码；查询失败时沿用上一次的结果，
     * 避免数据库抖动导致所有城市都识别不出来、整轮退化到不注入。</p>
     */
    private List<String> cities() {
        long now = System.currentTimeMillis();
        if (!cityCache.isEmpty() && now - cityCacheLoadedAt < CITY_CACHE_TTL_MILLIS) {
            return cityCache;
        }
        try {
            List<String> fresh = aiMarketStatMapper.selectCities().stream()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
            if (!fresh.isEmpty()) {
                cityCache = fresh;
                cityCacheLoadedAt = now;
            }
        } catch (RuntimeException e) {
            log.warn("城市词典加载失败, 沿用上一次结果: size={}", cityCache.size(), e);
        }
        return cityCache;
    }

    /**
     * 抽取岗位方向：按别名长度降序首次命中即返回，长词优先
     */
    private String extractCategory(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : CATEGORY_ALIASES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 判定方向：词典 → 技能组合 → 单技能，全部不中返回 null（落回 CITY_ONLY）
     */
    private CategoryMatch resolveCategory(String text) {
        String dict = extractCategory(text);
        if (dict != null) {
            return new CategoryMatch(dict, "dict");
        }

        String lower = text.toLowerCase(Locale.ROOT);
        for (ComboRule rule : COMBO_RULES) {
            if (rule.skills().stream().allMatch(skill -> matches(lower, skill))) {
                return new CategoryMatch(rule.category(),
                        "combo[" + String.join("+", rule.skills()) + "]");
            }
        }

        Set<String> categories = new LinkedHashSet<>();
        String firstSkill = null;
        for (Map.Entry<String, String> entry : SKILL_CATEGORY.entrySet()) {
            if (matches(lower, entry.getKey())) {
                categories.add(entry.getValue());
                if (firstSkill == null) {
                    firstSkill = entry.getKey();
                }
            }
        }
        if (categories.size() == 1) {
            return new CategoryMatch(categories.iterator().next(), "skill[" + firstSkill + "]");
        }
        return new CategoryMatch(null, categories.isEmpty() ? "none" : "conflict");
    }

    /**
     * 技能热度：优先走 skill_dict / tag_skill_map，映射表未就绪时回退到标签统计
     *
     * <p>两条路径的区别：字典版会把「MySQL数据库」「熟悉MySQL」归一到 MySQL、
     * 并自动排除不在映射表里的噪声标签；标签版靠黑名单正则过滤，是历史实现。</p>
     */
    private List<AiSkillHeatVO> loadSkills(String city, String l2) {
        List<AiSkillHeatVO> skills = aiMarketStatMapper.selectSkillHeatFromDict(city, l2, SKILL_LIMIT);
        if (skills == null || skills.isEmpty()) {
            log.warn("技能字典无可用映射, 回退到标签统计: city={} l2={}", city, l2);
            return aiMarketStatMapper.selectSkillHeat(city, l2, SKILL_LIMIT);
        }
        return skills;
    }

    /**
     * 技能展示名，未收录的按原样返回
     */
    private String displayName(String tag) {
        return SKILL_DISPLAY_NAMES.getOrDefault(tag, tag);
    }

    /**
     * 组合规则表，技能多的规则优先，避免被短规则抢先命中
     */
    private static List<ComboRule> buildComboRules() {
        List<ComboRule> rules = new ArrayList<>();
        rules.add(new ComboRule("数据", List.of("python", "sql")));
        rules.add(new ComboRule("数据", List.of("python", "excel")));
        rules.add(new ComboRule("数据", List.of("python", "tableau")));
        rules.add(new ComboRule("数据", List.of("python", "power bi")));
        rules.add(new ComboRule("数据", List.of("sql", "excel")));
        rules.add(new ComboRule("数据", List.of("excel", "tableau")));
        rules.add(new ComboRule("后端开发", List.of("python", "django")));
        rules.add(new ComboRule("后端开发", List.of("python", "flask")));
        rules.add(new ComboRule("后端开发", List.of("java", "mysql")));
        rules.add(new ComboRule("后端开发", List.of("java", "spring")));
        rules.add(new ComboRule("前端/移动开发", List.of("vue", "javascript")));
        rules.add(new ComboRule("前端/移动开发", List.of("react", "javascript")));
        rules.sort(Comparator.comparingInt((ComboRule rule) -> rule.skills().size()).reversed());
        return List.copyOf(rules);
    }

    /**
     * 单技能映射表，注释为实测主导方向占比（数据快照：2026-11-25，共 4581 条岗位）
     *
     * <p>注意：java / spring / vue / react / javascript / django 在 CATEGORY_ALIASES 里
     * 已有同向条目，而词典优先级更高，所以正常输入会先命中词典；这几个条目保留作为防御，
     * 真正靠这里生效的是 python / sql / excel / tableau / power bi / mysql
     * 这些词典里没有的歧义技能。</p>
     *
     * <p>python 是弱信号（从 64.4% 降到 62% 以下就会翻转），复测时优先复核这一条。</p>
     */
    private static Map<String, String> buildSkillCategories() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("excel", "数据");              // 100%
        map.put("tableau", "数据");            // 100%
        map.put("power bi", "数据");           // 93.3%
        map.put("sql", "数据");                // 69.2%
        map.put("spring", "后端开发");          // 91.0%
        map.put("django", "后端开发");          // 89.8%
        map.put("mysql", "后端开发");           // 85.8%
        map.put("java", "后端开发");            // 80.8%
        map.put("python", "后端开发");          // 64.4%，弱信号
        map.put("react", "前端/移动开发");       // 88.4%
        map.put("javascript", "前端/移动开发");  // 81.5%
        map.put("vue", "前端/移动开发");         // 69.6%
        return map;
    }

    /**
     * 技能展示名映射，覆盖高频技能与常见技术栈；未收录的保持原样
     */
    private static Map<String, String> buildDisplayNames() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("sql", "SQL");
        map.put("mysql", "MySQL");
        map.put("python", "Python");
        map.put("java", "Java");
        map.put("javascript", "JavaScript");
        map.put("typescript", "TypeScript");
        map.put("spring", "Spring");
        map.put("springcloud", "Spring Cloud");
        map.put("redis", "Redis");
        map.put("vue", "Vue");
        map.put("react", "React");
        map.put("html", "HTML");
        map.put("html5", "HTML5");
        map.put("css", "CSS");
        map.put("node.js", "Node.js");
        map.put("power bi", "Power BI");
        map.put("tableau", "Tableau");
        map.put("excel", "Excel");
        map.put("hive", "Hive");
        map.put("spss", "SPSS");
        map.put("matlab", "MATLAB");
        map.put("r", "R");
        map.put("docker", "Docker");
        map.put("kafka", "Kafka");
        map.put("postgresql", "PostgreSQL");
        map.put("oracle", "Oracle");
        map.put("mybatis", "MyBatis");
        map.put("django", "Django");
        map.put("flask", "Flask");
        map.put("c++", "C++");
        map.put("git", "Git");
        map.put("linux", "Linux");
        map.put("autocad", "AutoCAD");
        map.put("solidworks", "SolidWorks");
        map.put("photoshop", "Photoshop");
        return map;
    }

    /**
     * 组合规则：skills 全部出现在输入中才算命中
     */
    private record ComboRule(String category, List<String> skills) {
    }

    /**
     * 方向判定结果，category 为 null 表示没判定出来
     */
    private record CategoryMatch(String category, String source) {
    }

    /**
     * 抽取期望薪资，识别 15k / 15K 这类写法
     */
    private Integer extractExpectSalary(String text) {
        Matcher matcher = Pattern.compile("(\\d{1,3})\\s*[kK]").matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /**
     * 职级参考：由输入中的年限粗判，与 skill 小节「城市·职级 方向」的写法对应
     */
    private String guessLevel(String text) {
        if (text.contains("实习") || text.contains("应届") || text.contains("在校")) {
            return "初级";
        }
        Matcher matcher = Pattern.compile("(\\d{1,2})\\s*年").matcher(text);
        if (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            if (years <= 2) {
                return "初级";
            }
            if (years <= 4) {
                return "中级";
            }
            return "高级";
        }
        return "中级";
    }

    private double safe(Double value) {
        return value == null ? 0D : value;
    }

    private double round(Double value) {
        return BigDecimal.valueOf(safe(value)).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 方向别名词典，构建时按 key 长度降序，保证长词优先匹配
     *
     * <p>刻意收录若干复合词来解决歧义：javascript 之于 java、自动化测试 之于 自动化、
     * 产品运营 之于 产品。这类词必须比短词先命中，否则方向会判错。</p>
     */
    private static Map<String, String> buildAliases() {
        Map<String, String> raw = new LinkedHashMap<>();

        // 后端
        raw.put("后端开发", "后端开发");
        raw.put("服务端", "后端开发");
        raw.put("全栈", "后端开发");
        raw.put("golang", "后端开发");
        raw.put("java", "后端开发");
        raw.put("spring", "后端开发");
        raw.put("c++", "后端开发");

        // 前端 / 移动
        raw.put("前端/移动开发", "前端/移动开发");
        raw.put("前端开发", "前端/移动开发");
        raw.put("前端", "前端/移动开发");
        raw.put("移动开发", "前端/移动开发");
        raw.put("javascript", "前端/移动开发");
        raw.put("typescript", "前端/移动开发");
        raw.put("vue", "前端/移动开发");
        raw.put("react", "前端/移动开发");
        raw.put("android", "前端/移动开发");
        raw.put("小程序", "前端/移动开发");
        raw.put("ios", "前端/移动开发");

        // 数据
        raw.put("数据分析师", "数据");
        raw.put("数据分析", "数据");
        raw.put("数据挖掘", "数据");
        raw.put("数据开发", "数据");
        raw.put("数据仓库", "数据");
        raw.put("数据治理", "数据");
        raw.put("大数据", "数据");
        raw.put("power bi", "数据");
        raw.put("etl", "数据");

        // 测试
        raw.put("自动化测试", "测试");
        raw.put("软件测试", "测试");
        raw.put("测试开发", "测试");
        raw.put("测试", "测试");

        // 运营
        raw.put("产品运营", "运营");
        raw.put("用户运营", "运营");
        raw.put("内容运营", "运营");
        raw.put("电商运营", "运营");
        raw.put("活动运营", "运营");
        raw.put("新媒体运营", "运营");
        raw.put("数据运营", "运营");
        raw.put("运营", "运营");

        // 产品
        raw.put("产品经理", "产品经理");
        raw.put("产品", "产品经理");

        // 设计
        raw.put("视觉/交互设计", "视觉/交互设计");
        raw.put("视觉设计", "视觉/交互设计");
        raw.put("交互设计", "视觉/交互设计");
        raw.put("ui设计", "视觉/交互设计");
        raw.put("ui", "视觉/交互设计");
        raw.put("ux", "视觉/交互设计");
        raw.put("设计", "视觉/交互设计");

        // 人力 / 财务 / 行政
        raw.put("人力资源", "人力资源");
        raw.put("招聘", "人力资源");
        raw.put("人事", "人力资源");
        raw.put("hr", "人力资源");
        raw.put("财务", "财务");
        raw.put("会计", "财务");
        raw.put("出纳", "财务");
        raw.put("行政", "行政");

        // 教育 / 销售 / 客服
        raw.put("教师", "教师");
        raw.put("老师", "教师");
        raw.put("讲师", "教师");
        raw.put("医疗销售", "医疗销售");
        raw.put("销售", "销售");
        raw.put("客服", "客服");

        // 供应链 / 市场 / 制造
        raw.put("采购", "采购");
        raw.put("物流", "物流");
        raw.put("供应链", "物流");
        raw.put("市场营销", "市场营销");
        raw.put("市场", "市场营销");
        raw.put("推广/投放", "推广/投放");
        raw.put("推广", "推广/投放");
        raw.put("投放", "推广/投放");
        raw.put("机械设计/制造", "机械设计/制造");
        raw.put("机械设计", "机械设计/制造");
        raw.put("机械", "机械设计/制造");
        raw.put("电气/自动化", "电气/自动化");
        raw.put("电气", "电气/自动化");
        raw.put("自动化", "电气/自动化");
        raw.put("游戏设计", "游戏设计");

        Map<String, String> sorted = new LinkedHashMap<>();
        raw.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey(
                        Comparator.comparingInt(String::length).reversed()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        return sorted;
    }
}