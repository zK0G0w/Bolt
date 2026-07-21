# ====================================================
# Stage 1: Build
# ====================================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B

# ====================================================
# Stage 2: Runtime
# ====================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

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
