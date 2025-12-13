# 多阶段构建 - 构建阶段
FROM eclipse-temurin:8-jdk AS builder

WORKDIR /build

# 复制Maven配置文件
COPY pom.xml .
COPY src/ ./src/

# 构建应用
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar

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

# 暴露Web服务端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 设置JVM参数和运行应用
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
