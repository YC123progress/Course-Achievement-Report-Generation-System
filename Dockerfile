FROM maven:3.8-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY ruoyi-admin/ ruoyi-admin/
COPY ruoyi-common/ ruoyi-common/
COPY ruoyi-framework/ ruoyi-framework/
COPY ruoyi-generator/ ruoyi-generator/
COPY ruoyi-quartz/ ruoyi-quartz/
COPY ruoyi-system/ ruoyi-system/
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/ruoyi-admin/target/ruoyi-admin.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080} --spring.datasource.druid.url=${DB_URL} --spring.datasource.druid.username=${DB_USERNAME} --spring.datasource.druid.password=${DB_PASSWORD} --spring.redis.host=${REDIS_HOST:-redis.railway.internal} --spring.redis.port=${REDIS_PORT:-6379} --spring.redis.password=${REDIS_PASSWORD}"]