# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 国内构建走阿里云镜像，避免 Maven Central 超时卡住整个构建
RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0">' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>aliyun</id>' \
    '      <mirrorOf>central</mirrorOf>' \
    '      <url>https://maven.aliyun.com/repository/public</url>' \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' > /root/.m2/settings.xml

# 先只拷 pom：依赖没变时复用缓存，避免每次改代码都重新下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

WORKDIR /app

# 非 root 运行；logs 目录提前建好并授权，避免挂载卷后因权限写不了日志
# curl 用于容器健康检查，eclipse-temurin 基础镜像默认不带
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/logs \
    && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/ai-data-insights-dashboard-server.jar app.jar

USER app

EXPOSE 8081

# 用 sh -c 包一层，让 JAVA_OPTS 能在运行时通过环境变量覆盖
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]