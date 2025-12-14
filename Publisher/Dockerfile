# 多阶段构建 - 构建阶段
FROM eclipse-temurin:8-jdk AS builder

WORKDIR /build

# 复制Maven配置文件
COPY pom.xml .
COPY src/ ./src/

# 构建应用
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    mv target/*-jar-with-dependencies.jar app.jar

# 运行阶段
FROM eclipse-temurin:8-jre-alpine

# 创建非root用户
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# 复制构建的JAR文件
COPY --from=builder /build/app.jar .

# 设置权限
RUN chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD ps aux | grep java || exit 1

# 暴露端口
EXPOSE 8081

# 设置JVM参数和运行应用
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
