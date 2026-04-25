@echo off
echo 正在启动RuoYi后端服务（UTF-8编码）...
echo =====================================

rem 设置控制台编码为UTF-8
chcp 65001 >nul

rem 设置JAVA_OPTS环境变量
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Duser.language=zh -Duser.country=CN -Dconsole.encoding=UTF-8 -Djava.awt.headless=true

rem 启动Spring Boot应用
mvn spring-boot:run -Dspring-boot.run.jvmArguments="%JAVA_OPTS%"

pause 