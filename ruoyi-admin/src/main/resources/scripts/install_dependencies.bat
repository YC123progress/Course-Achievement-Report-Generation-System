@echo off
echo 课程目标达成评价系统 - Python依赖安装脚本
echo =====================================================

echo 检查Python安装...
python --version
if errorlevel 1 (
    echo [错误] Python未安装或未添加到系统PATH
    echo 请先安装Python 3.8或更高版本
    echo 下载地址: https://www.python.org/downloads/
    pause
    exit /b 1
)

echo.
echo 升级pip...
python -m pip install --upgrade pip

echo.
echo 安装必需的Python包...
python -m pip install pandas>=1.3.0
python -m pip install matplotlib>=3.5.0
python -m pip install numpy>=1.21.0

echo.
echo 安装字体支持包...
python -m pip install fonttools

echo.
echo 验证安装...
python -c "import pandas; print('pandas版本:', pandas.__version__)"
python -c "import matplotlib; print('matplotlib版本:', matplotlib.__version__)"
python -c "import numpy; print('numpy版本:', numpy.__version__)"

echo.
echo [成功] 所有依赖包安装完成！
echo.
echo 接下来的步骤：
echo 1. 重启RuoYi后端服务
echo 2. 测试课程目标达成评价功能
echo.
pause 