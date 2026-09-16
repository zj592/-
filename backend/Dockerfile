# syntax=docker/dockerfile:1
# 两阶段构建：Maven 镜像里编译，只把 jar 拷进 JRE 镜像
# 单独构建：docker build -t campus-lost-found:1.0.0 .

# 阶段 1：构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 国内环境走阿里云镜像，否则拉依赖容易超时
COPY maven-settings.xml /root/.m2/settings.xml

# 先单独拷 pom 预热依赖：只改 Java 代码时这一层可以命中缓存
COPY pom.xml .
RUN mvn -B -s /root/.m2/settings.xml -DskipTests dependency:go-offline \
    || echo "依赖预热未完全成功，改为打包阶段一起拉取（不影响结果）"

COPY src ./src
RUN mvn -B -s /root/.m2/settings.xml clean package -DskipTests

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:17-jre

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai"

WORKDIR /app

COPY --from=builder /build/target/lost-found-backend-1.0.0.jar /app/app.jar

# 上传目录，由 compose 挂成数据卷，容器重建不丢图片
RUN mkdir -p /app/uploads

EXPOSE 8080

# 用 bash 的 /dev/tcp 做探活，不依赖 curl/wget
HEALTHCHECK --interval=10s --timeout=3s --start-period=45s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
