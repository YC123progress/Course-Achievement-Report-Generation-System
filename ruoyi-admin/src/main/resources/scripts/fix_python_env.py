#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Python环境诊断和修复脚本
专门用于解决课程目标达成评价系统的运行问题
"""

import sys
import os
import subprocess
import platform
from pathlib import Path

def print_divider():
    print("=" * 60)

def print_section(title):
    print_divider()
    print(f" {title}")
    print_divider()

def run_command(command):
    """安全地运行命令并返回结果"""
    try:
        result = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=30)
        return result.returncode == 0, result.stdout, result.stderr
    except subprocess.TimeoutExpired:
        return False, "", "命令执行超时"
    except Exception as e:
        return False, "", str(e)

def check_python_basic():
    """检查Python基本信息"""
    print_section("Python基本信息检查")
    
    print(f"Python版本: {sys.version}")
    print(f"Python可执行文件: {sys.executable}")
    print(f"当前工作目录: {os.getcwd()}")
    print(f"操作系统: {platform.system()} {platform.release()}")
    print(f"架构: {platform.architecture()}")
    
    # 检查Python位数
    print(f"Python位数: {platform.architecture()[0]}")
    
    return True

def check_packages():
    """检查和安装必需的Python包"""
    print_section("Python包依赖检查")
    
    required_packages = {
        'pandas': 'pandas>=1.3.0',
        'matplotlib': 'matplotlib>=3.5.0', 
        'numpy': 'numpy>=1.21.0'
    }
    
    missing_packages = []
    
    for package, install_name in required_packages.items():
        try:
            __import__(package)
            # 获取包版本
            try:
                if package == 'pandas':
                    import pandas as pd
                    version = pd.__version__
                elif package == 'matplotlib':
                    import matplotlib
                    version = matplotlib.__version__
                elif package == 'numpy':
                    import numpy as np
                    version = np.__version__
                print(f"[OK] {package} v{version} 已安装")
            except:
                print(f"[OK] {package} 已安装（无法获取版本）")
        except ImportError:
            print(f"[ERROR] {package} 未安装")
            missing_packages.append(install_name)
    
    if missing_packages:
        print(f"\n缺少以下包，正在尝试安装...")
        for package in missing_packages:
            print(f"安装 {package}...")
            success, stdout, stderr = run_command(f"{sys.executable} -m pip install {package}")
            if success:
                print(f"[OK] {package} 安装成功")
            else:
                print(f"[ERROR] {package} 安装失败: {stderr}")
                print("请手动运行以下命令:")
                print(f"  {sys.executable} -m pip install {package}")
    
    return len(missing_packages) == 0

def check_matplotlib_backend():
    """检查matplotlib后端配置"""
    print_section("Matplotlib后端检查")
    
    try:
        import matplotlib
        print(f"Matplotlib版本: {matplotlib.__version__}")
        
        # 设置为非交互式后端
        matplotlib.use('Agg')
        print("[OK] 设置matplotlib后端为Agg（非交互式）")
        
        import matplotlib.pyplot as plt
        print("[OK] matplotlib.pyplot 导入成功")
        
        return True
    except Exception as e:
        print(f"[ERROR] matplotlib配置失败: {e}")
        return False

def check_font_support():
    """检查和配置中文字体支持"""
    print_section("中文字体支持检查")
    
    try:
        import matplotlib
        matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        from matplotlib import font_manager
        import matplotlib.font_manager as fm
        
        print("检查系统字体...")
        
        # 获取所有可用字体
        fonts = [f.name for f in font_manager.fontManager.ttflist]
        
        # 检查常见中文字体
        chinese_fonts = ['SimHei', 'Microsoft YaHei', 'SimSun', 'KaiTi', 'FangSong', 'DejaVu Sans']
        available_chinese_fonts = []
        
        for font in chinese_fonts:
            if font in fonts:
                available_chinese_fonts.append(font)
                print(f"[OK] 找到字体: {font}")
        
        if not available_chinese_fonts:
            print("[ERROR] 未找到合适的中文字体")
            print("正在尝试解决方案...")
            
            # 尝试方案1：使用DejaVu Sans（通常总是可用）
            try:
                plt.rcParams['font.sans-serif'] = ['DejaVu Sans']
                plt.rcParams['axes.unicode_minus'] = False
                print("[OK] 使用DejaVu Sans字体作为备选")
                return True
            except:
                pass
            
            # 尝试方案2：下载并配置字体
            try:
                font_path = download_chinese_font()
                if font_path:
                    configure_font(font_path)
                    return True
            except Exception as e:
                print(f"字体下载配置失败: {e}")
        else:
            # 配置找到的第一个中文字体
            selected_font = available_chinese_fonts[0]
            plt.rcParams['font.sans-serif'] = [selected_font]
            plt.rcParams['axes.unicode_minus'] = False
            print(f"[OK] 配置字体: {selected_font}")
            
            # 测试字体是否正常工作
            try:
                test_chinese_display()
                return True
            except Exception as e:
                print(f"字体测试失败: {e}")
                return False
        
        return len(available_chinese_fonts) > 0
        
    except Exception as e:
        print(f"[ERROR] 字体检查失败: {e}")
        return False

def download_chinese_font():
    """下载中文字体文件"""
    print("尝试下载SimHei字体...")
    
    # 创建字体目录
    font_dir = Path.home() / '.matplotlib' / 'fonts'
    font_dir.mkdir(parents=True, exist_ok=True)
    
    font_file = font_dir / 'SimHei.ttf'
    
    if font_file.exists():
        print(f"[OK] 字体文件已存在: {font_file}")
        return str(font_file)
    
    # 这里可以添加下载逻辑，或者提示用户手动下载
    print("请手动下载SimHei.ttf字体文件到以下目录:")
    print(f"  {font_dir}")
    print("字体下载地址: https://github.com/adobe-fonts/source-han-sans/releases")
    
    return None

def configure_font(font_path):
    """配置自定义字体"""
    try:
        import matplotlib.font_manager as fm
        import matplotlib.pyplot as plt
        
        # 添加字体到matplotlib
        fm.fontManager.addfont(font_path)
        
        # 获取字体名称
        font_prop = fm.FontProperties(fname=font_path)
        font_name = font_prop.get_name()
        
        # 配置matplotlib使用该字体
        plt.rcParams['font.sans-serif'] = [font_name]
        plt.rcParams['axes.unicode_minus'] = False
        
        print(f"[OK] 配置自定义字体: {font_name}")
        return True
    except Exception as e:
        print(f"字体配置失败: {e}")
        return False

def test_chinese_display():
    """测试中文显示"""
    print("测试中文字体显示...")
    
    try:
        import matplotlib
        matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        import numpy as np
        
        # 创建测试图表
        fig, ax = plt.subplots(figsize=(8, 6))
        
        # 测试数据
        x = ['课程目标1', '课程目标2', '课程目标3', '课程目标4']
        y = [0.85, 0.78, 0.82, 0.88]
        
        bars = ax.bar(x, y, color=['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728'])
        ax.set_title('课程目标达成度测试图', fontsize=16)
        ax.set_ylabel('达成度', fontsize=12)
        ax.axhline(y=0.6, color='red', linestyle='--', label='达成标准=0.6')
        
        # 添加数值标签
        for bar, value in zip(bars, y):
            ax.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.01, 
                   f'{value:.2f}', ha='center', va='bottom', fontsize=10)
        
        ax.legend()
        plt.tight_layout()
        
        # 保存测试图片
        test_file = 'font_test_chart.png'
        plt.savefig(test_file, dpi=300, bbox_inches='tight')
        plt.close()
        
        if os.path.exists(test_file):
            print(f"[OK] 中文字体测试成功，测试图片已保存: {test_file}")
            return True
        else:
            print("[ERROR] 测试图片未生成")
            return False
            
    except Exception as e:
        print(f"[ERROR] 中文字体测试失败: {e}")
        return False

def create_requirements_file():
    """创建requirements.txt文件"""
    print_section("创建依赖文件")
    
    requirements = """# 课程目标达成评价系统依赖包
pandas>=1.3.0
matplotlib>=3.5.0
numpy>=1.21.0

# 可选：用于更好的中文支持
fonttools>=4.0.0
"""
    
    try:
        with open('requirements.txt', 'w', encoding='utf-8') as f:
            f.write(requirements)
        print("[OK] requirements.txt 文件已创建")
        print("可以运行以下命令安装所有依赖:")
        print(f"  {sys.executable} -m pip install -r requirements.txt")
        return True
    except Exception as e:
        print(f"[ERROR] 创建requirements.txt失败: {e}")
        return False

def main():
    """主函数"""
    print("课程目标达成评价系统 - Python环境诊断工具")
    print("=" * 60)
    
    results = []
    
    # 1. 基本信息检查
    results.append(("Python基本信息", check_python_basic()))
    
    # 2. 包依赖检查
    results.append(("Python包依赖", check_packages()))
    
    # 3. matplotlib后端检查
    results.append(("Matplotlib后端", check_matplotlib_backend()))
    
    # 4. 字体支持检查
    results.append(("中文字体支持", check_font_support()))
    
    # 5. 创建依赖文件
    results.append(("创建依赖文件", create_requirements_file()))
    
    # 总结报告
    print_section("诊断总结")
    
    all_passed = True
    for test_name, passed in results:
        status = "[OK] 通过" if passed else "[ERROR] 失败"
        print(f"{test_name:<20} {status}")
        if not passed:
            all_passed = False
    
    print_divider()
    
    if all_passed:
        print("[OK] 所有检查都通过了！系统应该可以正常运行。")
        print("\n接下来的步骤:")
        print("1. 确保在RuoYi系统中配置正确的Python路径")
        print("2. 重启RuoYi后端服务")
        print("3. 测试课程目标达成评价功能")
    else:
        print("[WARNING] 发现问题需要解决:")
        print("\n建议的解决步骤:")
        print("1. 安装缺少的Python包:")
        print(f"   {sys.executable} -m pip install pandas matplotlib numpy")
        print("2. 如果仍有字体问题，下载SimHei.ttf字体到系统字体目录")
        print("3. 检查Python路径配置是否正确")
        print("4. 查看详细错误日志")
    
    print("\n如果问题持续存在，请:")
    print("1. 检查RuoYi后端日志")
    print("2. 确认Python可执行文件路径正确")
    print("3. 确认系统防火墙和权限设置")

if __name__ == "__main__":
    main() 