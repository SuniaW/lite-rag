# --- 第一阶段：编译阶段 (Build Stage) ---
# 使用 Maven + JDK 21 进行编译
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# 设置工作目录
WORKDIR /app

# 1. 利用 Docker 缓存机制：先只复制 pom.xml 下载依赖
# 只要 pom.xml 没变，这一步就不会重新下载依赖，极大加快构建速度
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 复制源码并进行打包
COPY src ./src
RUN mvn clean package -DskipTests

# --- 第二阶段：运行阶段 (Run Stage) ---
# 使用轻量级的 JRE 21 Alpine 镜像，体积仅为 JDK 的 1/3
FROM eclipse-temurin:21-jre-alpine

# 设置工作目录
WORKDIR /app

# 1. 安装必要的系统工具（可选，用于排查问题）
RUN apk add --no-cache curl

# 2. 设置系统时区为上海（解决日志时间不对的问题）
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 3. 从编译阶段复制生成的 JAR 包
# 注意：生成的 jar 名字根据 pom.xml 定义，这里使用通配符确保匹配
COPY --from=build /app/target/*.jar app.jar

# 4. 设置环境变量默认值（可在 docker-compose 中覆盖）
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_THREADS_VIRTUAL_ENABLED=true

# 5. 暴露后端端口 (与你 deploy.sh 中的 8081 一致)
EXPOSE 8081

# 6. JVM 极致优化参数说明：
# -Xmx256m: 堆内存上限（建议设置为容器内存限制的 50%-60%）。
# -Xms256m: 初始堆内存，与最大值一致可减少内存伸缩带来的抖动。
# -XX:+UseSerialGC: 核心配置！在低内存（小于 1GB）下，串行 GC 占用的额外内存开销最小。
# -XX:MaxMetaspaceSize=128m: 限制元空间，防止 Spring AI 扫描过多类导致内存溢出。
# -Xss256k: 减小单个线程栈大小，Java 21 配合虚拟线程可以完美运行。
# -XX:+ExitOnOutOfMemoryError: 内存溢出时立即退出，交给 Docker 重启容器。
# -Djava.security.egd=file:/dev/./urandom: 加速启动过程中的随机数生成。

ENTRYPOINT ["java", \
            "-Xmx256m", \
            "-Xms256m", \
            "-XX:MaxMetaspaceSize=128m", \
            "-Xss256k", \
            "-XX:+UseSerialGC", \
            "-XX:+ExitOnOutOfMemoryError", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Djdk.virtualThreadScheduler.parallelism=1", \
            "-jar", "app.jar"]