#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Python环境测试脚本
用于检查Python环境和必需包是否正确安装
"""

import sys
import os

def test_python_basic():
    """测试Python基本功能"""
    print("=== Python环境测试 ===")
    print("Python版本:", sys.version)
    print("Python可执行文件路径:", sys.executable)
    print("当前工作目录:", os.getcwd())
    return True

def test_required_packages():
    """测试必需的Python包"""
    print("=== Python包依赖检查 ===")
    
    required_packages = ['pandas', 'matplotlib', 'numpy']
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package)
            print(f"[OK] {package} 已安装")
        except ImportError:
            print(f"[ERROR] {package} 未安装")
            missing_packages.append(package)
    
    if missing_packages:
        print("\n缺少以下包，请运行以下命令安装:")
        print(f"pip install {' '.join(missing_packages)}")
        return False
    
    return True

def test_matplotlib_backend():
    """测试matplotlib后端"""
    print("=== Matplotlib后端检查 ===")
    try:
        import matplotlib
        matplotlib.use('Agg')  # 使用非交互式后端
        import matplotlib.pyplot as plt
        print("[OK] matplotlib后端配置正确")
        return True
    except Exception as e:
        print(f"[ERROR] matplotlib后端配置失败: {e}")
        return False

def main():
    """主函数"""
    print("课程目标达成评价系统 - Python环境测试")
    print("=" * 50)
    
    # 测试基本功能
    basic_ok = test_python_basic()
    print()
    
    # 测试必需包
    packages_ok = test_required_packages()
    print()
    
    # 测试matplotlib
    matplotlib_ok = test_matplotlib_backend()
    print()
    
    # 总结
    if basic_ok and packages_ok and matplotlib_ok:
        print("[OK] Python环境测试通过！")
        sys.exit(0)
    else:
        print("[ERROR] Python环境测试失败！")
        sys.exit(1)

if __name__ == "__main__":
    main() 