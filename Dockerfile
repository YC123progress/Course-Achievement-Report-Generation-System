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

FROM eclipse-temurin:17-jre

# 安装系统依赖：Python3、字体、Liberation字体（替代Arial）、DejaVu字体
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip \
    fontconfig \
    fonts-wqy-zenhei \
    fonts-liberation \
    fonts-dejavu-core \
    && fc-cache -fv \
    && mkdir -p /opt/java/openjdk/lib/fonts \
    && cp /usr/share/fonts/truetype/liberation/*.ttf /opt/java/openjdk/lib/fonts/ \
    && pip3 install --no-cache-dir --break-system-packages \
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

# 环境变量：强制 matplotlib 使用非交互后端，Java 使用 headless 模式
ENV PYTHON_EXECUTABLE=python3 \
    MPLBACKEND=Agg \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

RUN mkdir -p /app/uploadPath/config && chmod -R 777 /app/uploadPath
WORKDIR /app
COPY --from=builder /app/ruoyi-admin/target/ruoyi-admin.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]