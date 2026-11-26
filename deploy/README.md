# 生产部署说明

整套服务用 Docker Compose 编排：`nginx`（入口）→ `server`（业务）+ `ai-service`（AI 推理，仅内网），
外加 `redis`（缓存）和 `minio`（头像存储）。数据库使用外部云 RDS，不在 compose 内。

## 一、目录准备

服务器上保持这个结构（compose 里 `build.context` 是相对路径，位置不能变）：

```
/opt/ai-insights/
├── server/                 # 后端代码
│   └── deploy/
│       ├── docker-compose.prod.yml
│       ├── nginx/default.conf
│       ├── .env.prod       # 由 .env.prod.example 复制而来
│       ├── html/           # 前端 dist 内容
│       ├── logs/           # 后端日志落盘
│       └── sql/
│           ├── initdb/     # 容器首次启动自动执行
│           └── 01_schema.sql  # 已有库升级用的幂等增量脚本
└── ai-service/             # AI 服务代码
```

```bash
cd /opt/ai-insights
git clone <server仓库> server
git clone <ai-service仓库> ai-service
mkdir -p server/deploy/html server/deploy/logs
```

## 二、数据库

### 方式一：用 compose 自带的 MySQL（默认，推荐先跑通）

不需要额外配置。mysql 容器首次启动（数据卷为空）时会自动执行 `sql/initdb/` 下的脚本：

| 文件 | 内容 | 是否入库 |
|---|---|---|
| `00_schema.sql` | 全库表结构（19 张表 + 1 个视图），18KB | 是 |
| `01_data.sql` | 业务数据（岗位、分类、技能字典等 10 张表），约 42MB | **否** |

`01_data.sql` 体积大且含真实岗位数据，被 `.gitignore` 排除，部署时手动上传：

```bash
scp deploy/sql/initdb/01_data.sql root@<服务器>:/opt/ai-insights/server/deploy/sql/initdb/
```

注意：initdb 只在**数据卷为空**时执行一次。改了脚本要重新初始化，先删卷：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
docker volume rm ai-data-insights_mysql-data
```

### 方式二：改用云 RDS

1. `docker-compose.prod.yml` 里删掉 `mysql` 服务，以及 `server` 的 `mysql` depends_on
2. `.env.prod` 的 `MYSQL_HOST` 改成 RDS 内网地址，账号密码对应修改
3. 在 RDS 上执行初始化（与 initdb 两个文件相同）：

```bash
mysql -h <RDS地址> -u <用户> -p -e "CREATE DATABASE IF NOT EXISTS recruitment_dashboard DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
mysql -h <RDS地址> -u <用户> -p recruitment_dashboard < deploy/sql/initdb/00_schema.sql
mysql -h <RDS地址> -u <用户> -p recruitment_dashboard < deploy/sql/initdb/01_data.sql
```

> `00_schema.sql` 含 `DROP TABLE IF EXISTS`，**只能用于空库初始化**。
> 已有数据的库升级，用 `deploy/sql/01_schema.sql`（幂等增量脚本，只加索引和技能表）。

## 三、环境变量

```bash
cd /opt/ai-insights/server/deploy
cp .env.prod.example .env.prod
vi .env.prod
```

必填项与坑位：

| 变量 | 说明 |
|---|---|
| `MYSQL_HOST` | 用容器 MySQL 保持 `mysql`；用 RDS 则填内网地址 |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | 容器 MySQL 会自动创建这个账号并授权到 `MYSQL_DATABASE` |
| `MYSQL_ROOT_PASSWORD` | 仅容器 MySQL 使用（健康检查 + 初始化）；用 RDS 时可留空 |
| `JWT_SECRET` | **必填**，缺失直接启动失败。生成：`openssl rand -base64 48` |
| `CORS_ALLOWED_ORIGINS` | 同域部署可留空；前端分开部署必须填域名 |
| `REDIS_PASSWORD` | compose 用它做 `requirepass`，容器之间自动取用 |
| `AI_INTERNAL_TOKEN` | **必须与 ai-service 一致**，不一致时 AI 对话全 401，但服务看起来是正常的 |
| `AI_DEEPSEEK_API_KEY` | 真实 key |
| `S3_*` | prod 的 `storage-type` 默认 s3，**缺配置启动失败**；`S3_ENDPOINT` 保持 `http://minio:9000`，`S3_PUBLIC_BASE_URL` 填公网地址 |

## 四、前端

```bash
# 本地或 CI 构建
cd dataDashboard && npm run build

# 产物拷到服务器
scp -r dist/* root@<服务器>:/opt/ai-insights/server/deploy/html/
```

`dist` 内容直接铺在 `html/` 下（`index.html` 要在 `html/index.html`）。

## 五、启动

```bash
cd /opt/ai-insights/server/deploy
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

首次会构建两个镜像（maven 阶段要下依赖，比较慢）。之后改代码只需重建对应服务：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build server
```

## 六、验证

```bash
# 容器状态，server 和 redis 应显示 healthy
docker compose --env-file .env.prod -f docker-compose.prod.yml ps

# 后端存活（只监听本机 18081，不对外）
curl http://127.0.0.1:18081/api/actuator/health

# AI 服务存活
docker exec ai-insights-ai python -c "import urllib.request;print(urllib.request.urlopen('http://127.0.0.1:8000/healthz').read().decode())"

# 首页
curl -I http://<服务器IP>/

# 接口连通（会返回 401 属正常，说明 nginx→server 通了）
curl -i http://<服务器IP>/api/ai/conversations
```

浏览器打开 `http://<服务器IP>/`，登录后问一句 AI 对话，确认是**逐字输出**而不是等半天一次刷出——后者说明 `proxy_buffering off` 没生效。

## 七、常见问题

| 现象 | 原因 |
|---|---|
| 刷新子路由 404 | nginx 缺 `try_files $uri $uri/ /index.html` |
| 接口全 404 | `proxy_pass` 结尾多了 `/`，把 `/api` 前缀吃掉 |
| AI 回复一次性刷出 | nginx 或前面还有一层代理开了缓冲 |
| 启动失败并提示 s3 相关 | `S3_*` 没配全 |
| 启动失败并提示 secret 相关 | `JWT_SECRET` 没配 |
| AI 对话报错但服务正常 | `AI_INTERNAL_TOKEN` 两边不一致 |
| 头像 404 | `S3_PUBLIC_BASE_URL` 指的公网地址不通，或 bucket 未创建 |

## 八、日志

```bash
tail -f /opt/ai-insights/server/deploy/logs/server/ai-data-insights-server.log
docker logs -f ai-insights-ai
```

## 九、后续可优化

- MinIO 的 9000 端口目前直接暴露公网（为了头像直链），正式对外建议换成 OSS/COS + CDN，或由 nginx 反代 `/uploads`
- 加上 HTTPS：nginx 配置里补 443 块并挂载证书，80 跳 443
- 日志轮转：容器内日志已落盘，建议在宿主机配 logrotate