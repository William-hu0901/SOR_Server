# 多阶段构建 Docker 镜像
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

# 复制 pom.xml 文件
COPY pom.xml .
COPY sor-core/pom.xml sor-core/
COPY sor-disruptor/pom.xml sor-disruptor/
COPY sor-network/pom.xml sor-network/
COPY sor-marketdata/pom.xml sor-marketdata/
COPY sor-exchange/pom.xml sor-exchange/
COPY sor-risk/pom.xml sor-risk/
COPY sor-monitor/pom.xml sor-monitor/
COPY sor-benchmark/pom.xml sor-benchmark/

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源代码
COPY sor-core/src sor-core/src
COPY sor-disruptor/src sor-disruptor/src
COPY sor-network/src sor-network/src
COPY sor-marketdata/src sor-marketdata/src
COPY sor-exchange/src sor-exchange/src
COPY sor-risk/src sor-risk/src
COPY sor-monitor/src sor-monitor/src
COPY sor-benchmark/src sor-benchmark/src

# 编译项目
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

# 从构建阶段复制 jar 包
COPY --from=builder /app/sor-core/target/*.jar /app/sor-core.jar

# JVM 参数（针对低延迟优化）
ENV JAVA_OPTS="-server \
-XX:+UseZGC \
-XX:+ZGenerational \
-Xms4g -Xmx4g \
-XX:MaxDirectMemorySize=2g \
-Djava.awt.headless=true"

# 暴露端口（FIX 协议默认端口 9876）
EXPOSE 9876

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD java -cp /app/sor-core.jar com.sor.monitor.HealthCheck || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/sor-core.jar"]
