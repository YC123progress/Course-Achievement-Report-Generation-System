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

# 改为 Debian 基础镜像，避免 Alpine 的 AWT 兼容问题
FROM eclipse-temurin:17-jre

# Debian 下安装 Python3、中文字体
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip fontconfig fonts-wqy-zenhei && \
    fc-cache -f && \
    pip3 install --no-cache-dir --break-system-packages \
        contourpy==1.3.3 \
        cycler==0.12.1 \
        fonttools==4.62.1 \
        kiwisolver==1.5.0 \
        matplotlib==3.10.8 \
        numpy==2.4.4 \
        packaging==26.0 \
        pandas==3.0.2 \
        pillow==12.2.0 \
        pyparsing==3.3.2 \
        python-dateutil==2.9.0.post0 \
        six==1.17.0 \
        tzdata==2026.1 \
    && rm -rf /var/lib/apt/lists/*

ENV PYTHON_EXECUTABLE=python3 \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

RUN mkdir -p /app/uploadPath/config && chmod -R 777 /app/uploadPath
WORKDIR /app
COPY --from=builder /app/ruoyi-admin/target/ruoyi-admin.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]