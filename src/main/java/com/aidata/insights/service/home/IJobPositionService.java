package com.aidata.insights.service.home;

import com.aidata.insights.dto.home.JobQuery;
import com.aidata.insights.vo.home.AreaJobCountVO;
import com.aidata.insights.vo.home.CityOptionVO;
import com.aidata.insights.vo.home.ExperienceSalaryVO;
import com.aidata.insights.vo.home.JobDetailVO;
import com.aidata.insights.vo.home.JobListVO;
import com.aidata.insights.vo.home.SalaryRangeVO;
import com.aidata.insights.vo.home.SkillTagVO;

import java.util.List;

public interface IJobPositionService {

    /**
     * 分页查询岗位列表
     */
    JobListVO pageJobs(JobQuery query);

    /**
     * 获取岗位详情，优先读缓存
     */
    JobDetailVO getJobDetail(String id);

    /**
     * 按薪资区间统计岗位数量
     */
    List<SalaryRangeVO> salaryRange();

    /**
     * 技术栈热度 Top 10
     */
    List<SkillTagVO> hotSkillTags();

    /**
     * 城市岗位数量（前 8 + 其他）
     */
    List<AreaJobCountVO> districtJobCounts();

    /**
     * 按工作经验分档统计平均薪资
     */
    List<ExperienceSalaryVO> experienceSalary();

    /**
     * 城市下拉选项
     */
    List<CityOptionVO> cityOptions();
}
