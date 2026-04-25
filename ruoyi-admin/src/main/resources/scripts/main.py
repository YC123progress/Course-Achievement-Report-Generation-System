#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json
import pandas as pd
import os
import sys
import numpy as np
import re
import base64
import io
from pathlib import Path

def convert_sheets_to_csv(sheets_data):
    """将 sheets_data 转换为三个 CSV 文件（期末/平时/上机）"""
    for sheet in sheets_data:
        sheet_name = sheet.get('name', '')
        data_rows = sheet.get('data', [])
        if not data_rows:
            continue
        
        # 将数据转换为 DataFrame（第一行为表头）
        if len(data_rows) < 1:
            continue
        headers = data_rows[0]
        rows = data_rows[1:] if len(data_rows) > 1 else []
        df = pd.DataFrame(rows, columns=headers)
        
        # 根据 sheet 名称决定文件名
        if '期末' in sheet_name or 'final' in sheet_name.lower():
            filename = 'final_exam_scores_template.csv'
        elif '平时' in sheet_name or 'regular' in sheet_name.lower():
            filename = 'regular_scores_template.csv'
        elif '上机' in sheet_name or 'lab' in sheet_name.lower():
            filename = 'lab_scores_template.csv'
        else:
            # 默认按原名称
            filename = f"{sheet_name}.csv"
        
        # 保存 CSV（覆盖）
        df.to_csv(filename, index=False, encoding='utf-8-sig')
        print(f"[INFO] 已保存 CSV: {filename} (行数: {len(rows)})")

# 改进的字体配置函数
def configure_matplotlib():
    """配置matplotlib和字体"""
    try:
        import matplotlib
        matplotlib.use('Agg')  # 设置为非交互式后端
        
        import matplotlib.pyplot as plt
        from matplotlib import font_manager
        import matplotlib.font_manager as fm
        
        print("正在配置matplotlib...")
        
        # 清除字体缓存
        try:
            fm.fontManager.__init__()
        except:
            pass
        
        # 尝试配置中文字体，按优先级排序
        font_candidates = [
            'SimHei',           # Windows黑体
            'Microsoft YaHei',  # 微软雅黑
            'SimSun',           # 宋体
            'KaiTi',            # 楷体
            'FangSong',         # 仿宋
            'DejaVu Sans',      # 备选字体（Linux/Mac）
            'Arial Unicode MS', # Mac字体
            'Noto Sans CJK SC', # Linux字体
            'WenQuanYi Micro Hei', # Linux字体
        ]
        
        # 获取系统可用字体
        try:
            available_fonts = [f.name for f in font_manager.fontManager.ttflist]
            print(f"系统共有 {len(available_fonts)} 个字体")
            
            # 查找中文字体
            chinese_fonts = []
            for font in font_candidates:
                if font in available_fonts:
                    chinese_fonts.append(font)
                    print(f"[OK] 找到字体: {font}")
            
            print(f"找到中文字体: {chinese_fonts}")
        except Exception as e:
            print(f"获取系统字体列表失败: {e}")
            available_fonts = []
            chinese_fonts = []
        
        # 选择字体
        selected_font = None
        if chinese_fonts:
            selected_font = chinese_fonts[0]
        else:
            # 尝试Windows默认字体路径
            import os
            import platform
            
            if platform.system() == 'Windows':
                print("Windows系统，尝试查找系统字体...")
                windows_font_paths = [
                    r'C:\Windows\Fonts\simhei.ttf',
                    r'C:\Windows\Fonts\msyh.ttc',
                    r'C:\Windows\Fonts\simsun.ttc',
                    r'C:\Windows\Fonts\kaiti.ttf',
                ]
                
                for font_path in windows_font_paths:
                    if os.path.exists(font_path):
                        try:
                            # 添加字体到matplotlib
                            fm.fontManager.addfont(font_path)
                            # 获取字体名称
                            prop = fm.FontProperties(fname=font_path)
                            font_name = prop.get_name()
                            selected_font = font_name
                            print(f"[OK] 从路径加载字体: {font_path} -> {font_name}")
                            break
                        except Exception as e:
                            print(f"加载字体失败 {font_path}: {e}")
        
        # 设置字体
        if selected_font:
            plt.rcParams['font.sans-serif'] = [selected_font, 'DejaVu Sans']
            print(f"[OK] 使用字体: {selected_font}")
        else:
            # 使用matplotlib默认字体，禁用中文显示但保证不报错
            plt.rcParams['font.sans-serif'] = ['DejaVu Sans', 'Arial', 'sans-serif']
            print("[WARNING] 未找到中文字体，使用默认字体（中文可能显示为方块）")
        
        plt.rcParams['axes.unicode_minus'] = False
        
        # 测试字体配置
        try:
            fig, ax = plt.subplots(figsize=(1, 1))
            ax.text(0.5, 0.5, '测试', fontsize=12, ha='center', va='center')
            plt.close(fig)
            print("[OK] 字体配置测试通过")
        except Exception as e:
            print(f"[WARNING] 字体配置测试失败: {e}")
        
        return True
        
    except Exception as e:
        print(f"[ERROR] matplotlib配置失败: {e}")
        import traceback
        traceback.print_exc()
        return False

# 在导入matplotlib之前先配置
if not configure_matplotlib():
    print("matplotlib配置失败，程序可能无法正常生成图表")
    sys.exit(1)

import matplotlib.pyplot as plt

class NpEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        if isinstance(obj, np.floating):
            return float(obj)
        if isinstance(obj, np.ndarray):
            return obj.tolist()
        return super(NpEncoder, self).default(obj)

# 读取 JSON 配置文件
def read_config():
    try:
        with open('exam_config.json', 'r', encoding='utf-8') as f:
            config = json.load(f)
            print("[OK] 配置文件读取成功")
            return config
    except FileNotFoundError:
        print("[ERROR] 找不到 exam_config.json 文件")
        return None
    except json.JSONDecodeError as e:
        print(f"[ERROR] exam_config.json 文件格式错误: {e}")
        return None

def safe_read_csv(filename, encoding='utf-8'):
    """安全读取CSV文件，支持多种编码"""
    encodings = ['utf-8', 'utf-8-sig', 'gbk', 'gb2312', 'cp1252']
    
    for enc in encodings:
        try:
            df = pd.read_csv(filename, encoding=enc)
            print(f"[OK] 成功读取 {filename} (编码: {enc})")
            return df
        except Exception as e:
            if enc == encodings[-1]:  # 最后一个编码也失败
                print(f"[ERROR] 无法读取 {filename}: {e}")
                return None
            continue

# 读取所有必要的 CSV 文件
def read_all_csv(config):
    dfs = []

    # 读取平时成绩 CSV
    regular_df = None
    if config['regularGrade'] <= 0:
        regular_df = pd.DataFrame(columns=['学号', '姓名', '平时成绩总分'])
    else:
        regular_df = safe_read_csv('regular_scores_template.csv')
        if regular_df is not None:
            regular_df = regular_df.dropna(how='all')  # 删除全空行
            regular_df = drop_empty_columns(regular_df)
            regular_df['学号'] = regular_df['学号'].astype(str)
            if '平时成绩总分' not in regular_df.columns:
                regular_df['平时成绩总分'] = 0
            regular_df = regular_df[['学号', '姓名', '平时成绩总分']].fillna(0)
    dfs.append(regular_df)

    # 读取上机成绩 CSV
    lab_df = None
    if config['labGrade'] <= 0:
        lab_df = pd.DataFrame(columns=['学号', '姓名', '上机成绩总分'])
    else:
        lab_df = safe_read_csv('lab_scores_template.csv')
        if lab_df is not None:
            lab_df = lab_df.dropna(how='all')  # 删除全空行
            lab_df = drop_empty_columns(lab_df)
            lab_df['学号'] = lab_df['学号'].astype(str)
            if '上机成绩总分' not in lab_df.columns:
                lab_df['上机成绩总分'] = 0
            lab_df = lab_df[['学号', '姓名', '上机成绩总分']].fillna(0)
    dfs.append(lab_df)

    # 读取期末考核 CSV
    final_df = safe_read_csv('final_exam_scores_template.csv')
    if final_df is not None:
        final_df = final_df.dropna(how='all')  # 删除全空行
        final_df = drop_empty_columns(final_df)
        final_df['学号'] = final_df['学号'].astype(str)
        final_df = final_df.fillna(0)
        dfs.append(final_df)
    else:
        print("[ERROR] 找不到期末考试成绩文件")
        return None

    # 合并所有 DataFrame，以学号为主键进行外连接
    merged_df = dfs[0]
    for i, df in enumerate(dfs[1:], 1):
        if not df.empty:
            merged_df = pd.merge(merged_df, df, on=['学号', '姓名'], how='outer')
            print(f"[INFO] 合并第{i+1}个数据框，当前记录数: {len(merged_df)}")

    # 填充所有 NaN 值为 0，以确保计算不受缺失值影响
    merged_df = merged_df.fillna(0)

    # 确保必需的列存在
    if '平时成绩总分' not in merged_df.columns:
        merged_df['平时成绩总分'] = 0
        print("[WARNING] 合并后缺少'平时成绩总分'列，已添加默认值0")

    if '上机成绩总分' not in merged_df.columns:
        merged_df['上机成绩总分'] = 0
        print("[WARNING] 合并后缺少'上机成绩总分'列，已添加默认值0")

    print(f"[OK] 成功合并数据，共 {len(merged_df)} 条记录")
    print(f"[INFO] 数据列: {list(merged_df.columns)}")
    return merged_df

# 计算每个课程目标和考核方式的采分项满分值
def calculate_full_scores(config):
    """
    计算每个课程目标在各考核方式中的满分值
    
    根据规范要求：
    - courseTargetProportions[].regularGrade 是总成绩贡献值（已乘以考核方式占比）
    - 例如：regularGrade=18 表示该课程目标从平时成绩获得总成绩的18%
    - 如果平时成绩占总成绩30%，则该课程目标在平时成绩中的占比为：18/30*100 = 60%
    - 因此该课程目标在平时成绩中的满分值为：regularTotalScore * 60%
    
    计算公式：
    full_score = assessment_max_score * (contribution / assessment_grade_ratio)
    其中：
    - contribution: 课程目标在该考核方式的总成绩贡献值（如18）
    - assessment_grade_ratio: 该考核方式占总成绩的百分比（如30）
    - assessment_max_score: 该考核方式的卷面总分（如100）
    """
    course_target_full_scores = {}

    # 获取所有考核方式的满分值（卷面总分）
    regular_max_score = config.get('regularTotalScore', 0)
    lab_max_score = config.get('labTotalScore', 0)
    final_max_score = config.get('finalTotalScore', 0)

    # 获取考核方式在总成绩中的比例（%）
    regular_grade_ratio = config.get('regularGrade', 0)
    lab_grade_ratio = config.get('labGrade', 0)
    final_exam_ratio = config.get('finalExam', 0)

    # 遍历 courseTargetProportions 获取每个课程目标的信息
    for target in config['courseTargetProportions']:
        target_name = target['courseTarget']
        
        course_target_full_scores[target_name] = {}

        # 计算平时成绩的采分项满分值
        # contribution = target['regularGrade'] 是该课程目标从平时获得的总成绩贡献值
        # 例如：regularGrade=18, regular_grade_ratio=30 → 在平时中占比60% → 满分=100*0.6=60分
        if target.get('regularGrade', 0) > 0 and regular_grade_ratio > 0:
            contribution = target['regularGrade']  # 总成绩贡献值
            course_target_full_scores[target_name]['平时成绩'] = (
                regular_max_score * (contribution / regular_grade_ratio)
            )

        # 计算上机成绩的采分项满分值
        if target.get('lab', 0) > 0 and lab_grade_ratio > 0:
            contribution = target['lab']  # 总成绩贡献值
            course_target_full_scores[target_name]['上机成绩'] = (
                lab_max_score * (contribution / lab_grade_ratio)
            )

        # 计算期末考试的采分项满分值
        if target.get('finalExam', 0) > 0 and final_exam_ratio > 0:
            contribution = target['finalExam']  # 总成绩贡献值
            course_target_full_scores[target_name]['期末考试'] = (
                final_max_score * (contribution / final_exam_ratio)
            )
    
    return course_target_full_scores

# 生成"定量评价数据成绩拆分表"
def generate_split_score_table(config, df, full_scores):
    result_df = df[['学号', '姓名']].copy()
    
    # 确保必需的列存在，如果不存在则创建默认值为0的列
    if '平时成绩总分' not in df.columns:
        df['平时成绩总分'] = 0
        print("[WARNING] 缺少'平时成绩总分'列，已创建默认值为0的列")
    
    if '上机成绩总分' not in df.columns:
        df['上机成绩总分'] = 0
        print("[WARNING] 缺少'上机成绩总分'列，已创建默认值为0的列")

    # 遍历 courseTargetProportions 获取每个课程目标的信息
    for target in config['courseTargetProportions']:
        target_name = target['courseTarget']

        # 处理平时成绩
        if target.get('regularGrade', 0) > 0 and '平时成绩' in full_scores[target_name]:
            max_score = full_scores[target_name]['平时成绩']
            regular_total = config.get('regularTotalScore', 1)  # 避免除以0
            if regular_total > 0:
                result_df[f'{target_name}_平时成绩'] = df['平时成绩总分'] * (max_score / regular_total)
            else:
                result_df[f'{target_name}_平时成绩'] = 0

        # 处理上机成绩
        if target.get('lab', 0) > 0 and '上机成绩' in full_scores[target_name]:
            max_score = full_scores[target_name]['上机成绩']
            lab_total = config.get('labTotalScore', 1)  # 避免除以0
            if lab_total > 0:
                result_df[f'{target_name}_上机成绩'] = df['上机成绩总分'] * (max_score / lab_total)
            else:
                result_df[f'{target_name}_上机成绩'] = 0

        # 处理期末考试
        if target.get('finalExam', 0) > 0:
            # 生成期望的题目列名
            expected_questions = [f"{q['title']}.{sq['questionNumber']}（{sq['score']}分）" 
                                for q in config['examPaper'] 
                                for sq in q['questions'] 
                                if sq['target'] == target_name]
            
            # 更灵活的列名匹配：支持不同的标点符号
            existing_questions = []
            for expected_q in expected_questions:
                # 直接匹配
                if expected_q in df.columns:
                    existing_questions.append(expected_q)
                    continue
                
                # 尝试不同的标点符号组合
                variations = [
                    expected_q,
                    expected_q.replace('（', '(').replace('）', ')'),  # 全角转半角
                    expected_q.replace('(', '（').replace(')', '）'),  # 半角转全角
                    expected_q.replace('.0分', '分'),  # 移除.0
                    expected_q.replace('分', '.0分'),  # 添加.0
                ]
                
                for variation in variations:
                    if variation in df.columns:
                        existing_questions.append(variation)
                        break
                else:
                    # 如果都找不到，尝试模糊匹配
                    # 提取题目编号信息，如"1.1"
                    match = re.match(r'(\d+)\.(\d+)', expected_q)
                    if match:
                        title, number = match.groups()
                        # 在所有列中寻找包含相同题目编号的列
                        pattern = rf'{title}\.{number}.*分.*'
                        for col in df.columns:
                            if re.search(pattern, str(col)):
                                existing_questions.append(col)
                                print(f"[INFO] 找到匹配列: '{expected_q}' -> '{col}'")
                                break
            
            if existing_questions:
                print(f"[INFO] 课程目标{target_name}找到{len(existing_questions)}个期末考试题目列")
                # 转换期末考试列为数值类型，无法转换的部分会被设置为 NaN，然后再填充为0
                df[existing_questions] = df[existing_questions].apply(pd.to_numeric, errors='coerce').fillna(0)
                result_df[f'{target_name}_期末考试'] = df[existing_questions].sum(axis=1)
            else:
                result_df[f'{target_name}_期末考试'] = 0
                print(f"[WARNING] 未找到课程目标{target_name}的期末考试题目列")
                print(f"[DEBUG] 期望的列名: {expected_questions}")
                print(f"[DEBUG] 数据中的相关列: {[col for col in df.columns if '分）' in str(col) or '.1' in str(col) or '.2' in str(col)]}")

    # 添加平时考核总成绩、上机实验总成绩、期末考核总成绩
    result_df['平时考核总成绩'] = df['平时成绩总分']
    result_df['上机实验总成绩'] = df['上机成绩总分']
    
    # 计算期末考核总成绩
    exam_columns = [col for col in df.columns if '（' in col and '分）' in col]
    if exam_columns:
        result_df['期末考核总成绩'] = df[exam_columns].apply(pd.to_numeric, errors='coerce').fillna(0).sum(axis=1)
    else:
        result_df['期末考核总成绩'] = 0
        print("[WARNING] 未找到期末考试题目列")

    result_df['总成绩'] = (
        result_df['平时考核总成绩'] * config.get('regularGrade', 0) +
        result_df['上机实验总成绩'] * config.get('labGrade', 0) + 
        result_df['期末考核总成绩'] * config.get('finalExam', 0)
    ) / 100
    
    # 调试日志：打印总成绩计算的详细信息
    print("\n========== 总成绩计算调试信息 ==========")
    print(f"regularGrade: {config.get('regularGrade', 0)}")
    print(f"labGrade: {config.get('labGrade', 0)}")
    print(f"finalExam: {config.get('finalExam', 0)}")
    print(f"平时考核总成绩范围: {result_df['平时考核总成绩'].min():.2f} - {result_df['平时考核总成绩'].max():.2f}")
    print(f"上机实验总成绩范围: {result_df['上机实验总成绩'].min():.2f} - {result_df['上机实验总成绩'].max():.2f}")
    print(f"期末考核总成绩范围: {result_df['期末考核总成绩'].min():.2f} - {result_df['期末考核总成绩'].max():.2f}")
    print(f"总成绩范围: {result_df['总成绩'].min():.2f} - {result_df['总成绩'].max():.2f}")
    print(f"总成绩平均值: {result_df['总成绩'].mean():.2f}")
    print(f"总成绩样本（前5个学生）: {result_df['总成绩'].head().tolist()}")
    print("========================================\n")

    # 计算课程目标的个人达成度
    method_mapping = {
        '平时成绩': 'regularGrade',
        '上机成绩': 'lab',
        '期末考试': 'finalExam'
    }

    for target in config['courseTargetProportions']:
        target_name = target['courseTarget']
        total_ratio = target['total']

        achievement_sum = 0
        for method, target_key in method_mapping.items():
            col_name = f'{target_name}_{method}'
            if col_name in result_df.columns and target.get(target_key, 0) > 0:
                if method in full_scores[target_name]:
                    score = result_df[col_name]
                    full_score = full_scores[target_name][method]
                    ratio = target[target_key]
                    if full_score > 0:  # 避免除以0
                        achievement_sum += (score / full_score) * (ratio / total_ratio)

        result_df[f'{target_name}_个人达成度'] = achievement_sum

    # 保存为 CSV 文件，使用 utf-8-sig 编码确保在 Excel 中正常显示
    result_df.to_csv('quantitative_evaluation_split_scores.csv', index=False, encoding='utf-8-sig')
    print("[OK] 定量评价数据成绩拆分表已保存到 quantitative_evaluation_split_scores.csv 文件中")

    return result_df

# 生成统计数据
def generate_statistics(config, split_score_df, full_scores):
    statistics = {}

    # 计算每个课程目标和考核方式的平均值和达成度
    course_targets = config['courseTargets']
    for target_name in course_targets:  # 这里不再是字典，而是直接使用字符串列表
        statistics[target_name] = {}

        for method in ['平时成绩', '上机成绩', '期末考试']:
            col_name = f'{target_name}_{method}'
            if col_name in split_score_df.columns:
                # 忽略 0 值后取平均值
                non_zero_values = split_score_df[split_score_df[col_name] > 0][col_name]
                if not non_zero_values.empty:
                    avg_value = non_zero_values.mean()
                    statistics[target_name][f'{method}_平均值'] = avg_value
                    # 计算达成度
                    full_score = full_scores[target_name][method]
                    statistics[target_name][f'{method}_达成度'] = avg_value / full_score
                else:
                    statistics[target_name][f'{method}_平均值'] = 0
                    statistics[target_name][f'{method}_达成度'] = 0

    # 总成绩的统计数据
    final_exam_scores = split_score_df[split_score_df['总成绩'] > 0]['总成绩']
    if not final_exam_scores.empty:
        statistics['平均总成绩'] = final_exam_scores.mean()
        statistics['中位数'] = final_exam_scores.median()
        statistics['标准差'] = final_exam_scores.std()
        statistics['最高分'] = final_exam_scores.max()
        statistics['最低分'] = final_exam_scores.min()
    else:
        statistics['平均总成绩'] = 0
        statistics['中位数'] = 0
        statistics['标准差'] = 0
        statistics['最高分'] = 0
        statistics['最低分'] = 0

    # 计算总人数，未参加考试人数和参加考试人数
    total_students = split_score_df.shape[0]
    not_participated = split_score_df[split_score_df['期末考核总成绩'] == 0].shape[0]
    participated = total_students - not_participated

    statistics['学生总数'] = total_students
    statistics['未参加考试人数'] = not_participated
    statistics['参加考试人数'] = participated

    # 计算成绩分布
    grades = ['优秀', '良好', '中等', '及格', '不及格']
    grade_boundaries = [90, 80, 70, 60, 0]

    grade_counts = {}
    for i, grade in enumerate(grades):
        if i == 0:
            condition = split_score_df['总成绩'] >= grade_boundaries[i]
        else:
            condition = (split_score_df['总成绩'] < grade_boundaries[i - 1]) & (split_score_df['总成绩'] >= grade_boundaries[i])

        count = split_score_df[condition & (split_score_df['总成绩'] > 0)].shape[0]
        grade_counts[grade] = {
            '人数': count,
            '占比': (count / total_students) * 100 if total_students > 0 else 0
        }
    
    # 调试日志：打印成绩分布详情
    print("\n========== 成绩分布调试信息 ==========")
    print(f"总学生数: {total_students}")
    print(f"总成绩列是否存在: {'总成绩' in split_score_df.columns}")
    if '总成绩' in split_score_df.columns:
        print(f"总成绩非零学生数: {split_score_df[split_score_df['总成绩'] > 0].shape[0]}")
        print(f"总成绩为零学生数: {split_score_df[split_score_df['总成绩'] == 0].shape[0]}")
    print("成绩分布详情:")
    for grade, info in grade_counts.items():
        print(f"  {grade}: {info['人数']}人 ({info['占比']:.2f}%)")
    print("========================================\n")

    statistics['成绩分布'] = grade_counts

    return statistics

def generate_achievement_table(config, full_scores, statistics):
    # 定义列名
    columns = [
        '课程目标',
        '考试_额定值', '考试_分值', '考试_达成度',
        '课堂作业_额定值', '课堂作业_分值', '课堂作业_达成度',
        '实验报告_额定值', '实验报告_分值', '实验报告_达成度',
        '达成度（标准= 0.6）'
    ]

    # 初始化表格
    result_df = pd.DataFrame(columns=columns)

    # 定义方法到键的映射
    method_mapping = {
        '平时成绩': 'regularGrade',
        '上机成绩': 'lab',
        '期末考试': 'finalExam'
    }

    # 遍历课程目标，生成每一行数据
    for target_name in config['courseTargets']:
        # 查找 courseTargetProportions 中对应的课程目标
        target_info = next((item for item in config['courseTargetProportions'] if item['courseTarget'] == target_name), None)

        if target_info is None:
            continue  # 如果没有找到对应的课程目标，跳过

        row = {'课程目标': target_name}

        # 填充"考试"列信息
        if target_info.get('finalExam', 0) > 0:
            final_exam_full_score = full_scores[target_name].get('期末考试', 0)
            final_exam_avg_score = statistics[target_name].get('期末考试_平均值', 0)
            final_exam_achievement = statistics[target_name].get('期末考试_达成度', 0)

            row['考试_额定值'] = final_exam_full_score
            row['考试_分值'] = final_exam_avg_score
            row['考试_达成度'] = final_exam_achievement
        else:
            row['考试_额定值'] = row['考试_分值'] = row['考试_达成度'] = '-'

        # 填充"课堂作业"列信息
        if target_info.get('regularGrade', 0) > 0:
            regular_full_score = full_scores[target_name].get('平时成绩', 0)
            regular_avg_score = statistics[target_name].get('平时成绩_平均值', 0)
            regular_achievement = statistics[target_name].get('平时成绩_达成度', 0)

            row['课堂作业_额定值'] = regular_full_score
            row['课堂作业_分值'] = regular_avg_score
            row['课堂作业_达成度'] = regular_achievement
        else:
            row['课堂作业_额定值'] = row['课堂作业_分值'] = row['课堂作业_达成度'] = '-'

        # 填充"实验报告"列信息
        if target_info.get('lab', 0) > 0:
            lab_full_score = full_scores[target_name].get('上机成绩', 0)
            lab_avg_score = statistics[target_name].get('上机成绩_平均值', 0)
            lab_achievement = statistics[target_name].get('上机成绩_达成度', 0)

            row['实验报告_额定值'] = lab_full_score
            row['实验报告_分值'] = lab_avg_score
            row['实验报告_达成度'] = lab_achievement
        else:
            row['实验报告_额定值'] = row['实验报告_分值'] = row['实验报告_达成度'] = '-'

        # 计算达成度（标准= 0.6）
        total_ratio = target_info['total']  # 课程目标占总成绩的比例
        achievement_sum = 0

        for method, target_key in method_mapping.items():
            if target_info.get(target_key, 0) > 0:
                method_achievement = statistics[target_name].get(f'{method}_达成度', 0)
                method_ratio = target_info[target_key]  # 该考核方式占总成绩的比例
                achievement_sum += method_achievement * (method_ratio / total_ratio)

        row['达成度（标准= 0.6）'] = achievement_sum

        # 将行数据加入 DataFrame
        new_row_df = pd.DataFrame([row])
        if result_df.empty:
            result_df = new_row_df
        else:
            result_df = pd.concat([result_df, new_row_df], ignore_index=True)

    # 保存为 CSV 文件，使用 utf-8-sig 编码确保在 Excel 中正常显示
    result_df.to_csv('overall_achievement_table.csv', index=False, encoding='utf-8-sig')
    print("课程目标整体达成情况表已保存到 overall_achievement_table.csv 文件中。")

    return result_df

# 根据学生成绩绘制等级分布的柱状图
def plot_grade_distribution(statistics):
    try:
        print("正在生成成绩分布图...")
        
        grade_labels = ['优秀(≥90)', '良好(80-90)', '中等(70-80)', '及格(60-70)', '不及格(<60)']
        grades = ['优秀', '良好', '中等', '及格', '不及格']
        
        # 获取每个成绩等级的学生人数和占比
        counts = [statistics['成绩分布'][grade]['人数'] for grade in grades]
        percentages = [statistics['成绩分布'][grade]['占比'] for grade in grades]
        
        print(f"成绩分布数据: {dict(zip(grades, counts))}")

        # 创建图形，包含人数和占比两个柱状图
        fig, ax1 = plt.subplots(figsize=(12, 8))

        # 绘制人数柱状图
        colors = ['#2E8B57', '#4169E1', '#FF8C00', '#DC143C', '#8B0000']
        bars = ax1.bar(grade_labels, counts, color=colors, alpha=0.7, label='人数')
        ax1.set_xlabel('成绩等级', fontsize=12)
        ax1.set_ylabel('学生人数', fontsize=12)
        ax1.set_title('总成绩等级分布图', fontsize=16, fontweight='bold')
        
        # 在右侧创建一个共享 x 轴的 y 轴，用于显示占比
        ax2 = ax1.twinx()
        line = ax2.plot(grade_labels, percentages, color='red', marker='o', 
                       linestyle='-', linewidth=3, markersize=8, label='占比 (%)')
        ax2.set_ylabel('占比 (%)', fontsize=12)

        # 在柱状图上方显示学生人数
        for bar, count in zip(bars, counts):
            height = bar.get_height()
            if height > 0:  # 只在有数据时显示标签
                ax1.text(bar.get_x() + bar.get_width() / 2, height + 0.1, 
                        str(count), ha='center', va='bottom', fontsize=10, fontweight='bold')

        # 在线图上显示占比
        for i, (label, percentage) in enumerate(zip(grade_labels, percentages)):
            if percentage > 0:
                ax2.text(i, percentage + 1, f'{percentage:.1f}%', 
                        ha='center', va='bottom', fontsize=9, color='red')

        # 设置网格
        ax1.grid(True, alpha=0.3)
        
        # 添加图例
        ax1.legend(loc='upper left', fontsize=10)
        ax2.legend(loc='upper right', fontsize=10)

        # 调整布局
        plt.tight_layout()
        
        # 保存图表
        filename = 'grade_distribution_chart.png'
        plt.savefig(filename, dpi=300, bbox_inches='tight', facecolor='white')
        print(f"[OK] 成绩分布图已保存: {filename}")
        
        # 关闭图形释放内存
        plt.close(fig)
        
    except Exception as e:
        print(f"[ERROR] 生成成绩分布图失败: {e}")
        import traceback
        traceback.print_exc()

# 根据所有学生的课程目标达成度绘制散点图和柱状图
def plot_achievement_statistics(split_score_df, achievement_table_df, config):
    try:
        print("正在生成课程目标达成度图表...")
        
        course_targets = config['courseTargets']
        print(f"课程目标: {course_targets}")

        # 创建包含每个课程目标达成度的 DataFrame
        achievement_columns = [f"{target}_个人达成度" for target in course_targets]
        print(f"达成度列: {achievement_columns}")
        
        # 检查列是否存在
        existing_columns = [col for col in achievement_columns if col in split_score_df.columns]
        print(f"存在的达成度列: {existing_columns}")
        
        if not existing_columns:
            print("⚠ 没有找到达成度数据列")
            return
            
        achievement_data = split_score_df[existing_columns]

        # 从 achievement_table_df 中获取每个课程目标的平均达成度
        try:
            achievement_avg = achievement_table_df.set_index('课程目标')['达成度（标准= 0.6）']
            print(f"平均达成度数据: {achievement_avg.to_dict()}")
        except Exception as e:
            print(f"获取平均达成度失败: {e}")
            return

        # 绘制每个课程目标的达成度散点图
        for target in course_targets:
            try:
                target_column = f"{target}_个人达成度"
                
                if target_column not in split_score_df.columns:
                    print(f"⚠ 跳过 {target}：未找到对应的达成度数据")
                    continue
                
                print(f"正在生成 {target} 达成度散点图...")
                
                plt.figure(figsize=(12, 8))

                # 只保留非零的达成度数据点
                non_zero_data = split_score_df[split_score_df[target_column] > 0]
                
                if non_zero_data.empty:
                    print(f"⚠ {target} 没有有效的达成度数据")
                    plt.close()
                    continue

                # 绘制散点图
                plt.scatter(range(len(non_zero_data)), non_zero_data[target_column], 
                           color='blue', alpha=0.6, s=60, label='个人达成度', edgecolors='navy')

                # 添加平均线和达成标准线
                if target in achievement_avg.index:
                    avg_value = achievement_avg[target]
                    plt.axhline(y=avg_value, color='green', linestyle='-', linewidth=2, 
                               label=f'平均达成度={avg_value:.3f}')
                
                plt.axhline(y=0.6, color='red', linestyle='--', linewidth=2, 
                           label='达成标准=0.6')

                # 图表标题和标签
                plt.xlabel('学生序号', fontsize=12)
                plt.ylabel('达成度', fontsize=12)
                plt.title(f'{target} 达成度散点图', fontsize=16, fontweight='bold')
                plt.ylim(0, 1.1)  # 达成度范围设定为 0 到 1.1
                plt.grid(True, alpha=0.3)
                plt.legend(loc='best', fontsize=10)

                # 调整布局
                plt.tight_layout()
                
                # 保存测试图片
                filename = f'{target}_achievement_scatter_chart.png'
                plt.savefig(filename, dpi=300, bbox_inches='tight', facecolor='white')
                print(f"[OK] {target} 散点图已保存: {filename}")
                
                # 关闭图形释放内存
                plt.close()
                
            except Exception as e:
                print(f"[ERROR] 生成 {target} 散点图失败: {e}")
                import traceback
                traceback.print_exc()
                plt.close()  # 确保图形被关闭

        # 绘制柱状图 - 各课程目标的平均达成度
        try:
            print("正在生成课程目标达成度柱状图...")
            
            plt.figure(figsize=(12, 8))
            
            # 准备数据
            targets = list(achievement_avg.index)
            values = list(achievement_avg.values)
            
            print(f"柱状图数据: {dict(zip(targets, values))}")
            
            # 绘制柱状图
            colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728'][:len(targets)]
            bars = plt.bar(targets, values, color=colors, alpha=0.7, edgecolor='black', linewidth=1)
            
            # 添加数值标签
            for bar, value in zip(bars, values):
                height = bar.get_height()
                plt.text(bar.get_x() + bar.get_width()/2, height + 0.01, 
                        f'{value:.3f}', ha='center', va='bottom', fontsize=11, fontweight='bold')
            
            plt.xlabel('课程目标', fontsize=12)
            plt.ylabel('平均达成度', fontsize=12)
            plt.title('各课程目标平均达成度柱状图', fontsize=16, fontweight='bold')
            
            # 添加达成标准线
            plt.axhline(y=0.6, color='red', linestyle='--', linewidth=2, label='达成标准=0.6')
            
            plt.grid(True, alpha=0.3)
            plt.legend(loc='best', fontsize=10)
            
            # 调整布局
            plt.tight_layout()
            
            # 保存图表
            filename = 'achievement_bar_chart.png'
            plt.savefig(filename, dpi=300, bbox_inches='tight', facecolor='white')
            print(f"[OK] 达成度柱状图已保存: {filename}")
            
            # 关闭图形释放内存
            plt.close()
            
        except Exception as e:
            print(f"[ERROR] 生成达成度柱状图失败: {e}")
            import traceback
            traceback.print_exc()
            plt.close()  # 确保图形被关闭
            
    except Exception as e:
        print(f"[ERROR] 生成课程目标达成度图表失败: {e}")
        import traceback
        traceback.print_exc()

# 移除全空列
def drop_empty_columns(df):
    # 删除列名包含 'Unnamed' 的列
    unnamed_cols = [col for col in df.columns if 'Unnamed' in str(col)]
    if unnamed_cols:
        df = df.drop(columns=unnamed_cols)
        print(f"[INFO] 已删除 {len(unnamed_cols)} 个空列", flush=True)
    # 删除全为空的列
    df = df.dropna(axis=1, how='all')
    return df

if __name__ == "__main__":
    print("=== 课程目标达成评价系统 ===")
    print("正在初始化...")
    
    required_files = ['exam_config.json']
    missing_files = [f for f in required_files if not os.path.exists(f)]
    if missing_files:
        print(f"[ERROR] 缺少必要文件: {missing_files}")
        sys.exit(1)
    
    # 检查 CSV 文件（仅提示，不强制退出，因为可能部分考核方式未选择）
    csv_files = ['final_exam_scores_template.csv', 'regular_scores_template.csv', 'lab_scores_template.csv']
    available_csv = [f for f in csv_files if os.path.exists(f)]
    if not available_csv:
        print("[ERROR] 没有找到任何CSV数据文件")
        sys.exit(1)
    
    print("开始处理数据...")
    
    # 第一步：读取 JSON 和所有需要的 CSV 文件
    config = read_config()
    df = read_all_csv(config)

    if config is not None and df is not None:
        print("[OK] 数据读取完成，开始分析...")
        
        # 第二步：计算每个课程目标和考核方式的采分项满分值
        full_scores = calculate_full_scores(config)
        
        # 第三步：生成"定量评价数据成绩拆分表"
        split_score_df = generate_split_score_table(config, df, full_scores)
        
        # 第四步：生成统计数据
        statistics = generate_statistics(config, split_score_df, full_scores)

        # 第五步：生成整体达成情况表
        achievement_table_df = generate_achievement_table(config, full_scores, statistics)

        # 第六步：保存统计数据
        with open('statistics_summary.json', 'w', encoding='utf-8') as stats_file:
            json.dump(statistics, stats_file, ensure_ascii=False, indent=4, cls=NpEncoder)
        print("[OK] 统计数据已保存到 statistics_summary.json 文件中")

        # 第七步：绘制期末考核总成绩等级分布图（包括未参加考试）
        try:
            plot_grade_distribution(statistics)
            print("[OK] 成绩分布图生成完成")
        except Exception as e:
            print(f"[WARNING] 成绩分布图生成失败: {e}")

        # 第八步：绘制课程目标达成度图表（散点图和柱状图），直接使用生成的达成情况表
        try:
            plot_achievement_statistics(split_score_df, achievement_table_df, config)
            print("[OK] 课程目标达成度图表生成完成")
        except Exception as e:
            print(f"[WARNING] 课程目标达成度图表生成失败: {e}")
        
        print("=== 处理完成 ===")
        print("生成的文件:")
        print("- quantitative_evaluation_split_scores.csv (定量评价数据成绩拆分表)")
        print("- overall_achievement_table.csv (整体达成情况表)")
        print("- statistics_summary.json (统计数据)")
        print("- grade_distribution_chart.png (成绩分布图)")
        print("- achievement_bar_chart.png (达成度柱状图)")
        print("- *_achievement_scatter_chart.png (各课程目标散点图)")
    else:
        print("[ERROR] 数据读取失败，程序终止")
        sys.exit(1)