# ====================================================
# Stage 1: Build
# ====================================================
FROM azul/zulu-openjdk:25 AS builder

WORKDIR /build
COPY pom.xml .
COPY .mvn .mvn

# 先下载依赖，利用 Docker 层缓存
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -B

# ====================================================
# Stage 2: Runtime
# ====================================================
FROM azul/zulu-openjdk-alpine:25-jre AS runtime

LABEL maintainer="WainZeng"
LABEL description="Bolt RTB Ad Bidding Engine"

# 非 root 运行
RUN addgroup -S bolt && adduser -S bolt -G bolt

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

RUN chown -R bolt:bolt /app
USER bolt

EXPOSE 9292

# JVM 参数：容器感知内存 + GC 调优
ENV JAVA_OPTS="-XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
