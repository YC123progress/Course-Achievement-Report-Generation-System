# 课程目标达成评价系统 - Python环境配置

## ✅ 已修复问题

### 主要修复内容：
1. **Unicode编码问题** - 将所有特殊字符（✓、✗等）替换为普通字符（[OK]、[ERROR]等）
2. **Python脚本输出编码** - 在Java中设置PYTHONIOENCODING=utf-8
3. **字体配置优化** - 改进中文字体检测和回退机制
4. **错误处理增强** - 添加详细的错误日志和调试信息

## 快速解决方案

### 方法1：运行自动安装脚本（推荐）
```bash
# 在scripts目录下运行
install_dependencies.bat
```

### 方法2：手动安装
```bash
# 安装Python包
python -m pip install pandas matplotlib numpy fonttools

# 验证安装
python -c "import pandas, matplotlib, numpy; print('All packages installed')"
```

## 系统要求

- **Python版本**: 3.8+ （已测试：3.13.1）
- **必需包**: pandas, matplotlib, numpy
- **可选包**: fonttools (用于字体支持)
- **操作系统**: Windows 10/11

## 修复后的特点

### 1. 改进的编码处理
- 移除了所有Unicode特殊字符
- 使用标准ASCII字符输出日志
- 设置了PYTHONIOENCODING=utf-8环境变量

### 2. 智能字体配置
- 自动检测Windows系统字体
- 多级字体回退机制
- 优雅处理字体缺失情况

### 3. 详细的错误诊断
- 完整的环境检查日志
- 文件生成验证
- 清晰的错误消息

## 验证方法

运行以下命令验证环境：

```bash
# 检查Python版本
python --version

# 检查包安装
python -c "import pandas, matplotlib, numpy; print('Success')"

# 测试matplotlib
python -c "import matplotlib; matplotlib.use('Agg'); print('matplotlib OK')"
```

## 配置说明

在 `application.yml` 中：
```yaml
python:
  executable: python  # 推荐使用系统PATH中的python
  # 或使用绝对路径: C:/Python313/python.exe
```

## 生成的文件

修复后系统正常生成以下文件：
- `grade_distribution_chart.png` (成绩分布图)
- `achievement_bar_chart.png` (达成度柱状图)  
- `*_achievement_scatter_chart.png` (各课程目标散点图)
- `quantitative_evaluation_split_scores.csv` (定量评价数据)
- `overall_achievement_table.csv` (整体达成情况表)
- `statistics_summary.json` (统计数据)

## 常见问题解决

### Q: 图片仍然无法加载？
A: 检查后端日志，确认Python脚本是否成功执行，无"UnicodeEncodeError"错误

### Q: 中文显示为方块？
A: 系统会自动处理，现在使用英文标签和数值显示，不影响功能

### Q: Python脚本超时？
A: 正常情况下30秒内完成，如果超时请检查数据量和系统性能

## 技术支持

如果仍有问题：
1. 查看RuoYi后端控制台日志
2. 确认Python环境PATH配置
3. 检查文件权限和磁盘空间
4. 重启RuoYi后端服务

---
**状态**: ✅ 已修复并测试通过  
**最后更新**: 2024年12月  
**Python版本**: 3.13.1 兼容 