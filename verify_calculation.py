#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
验证课程目标比例计算逻辑是否符合规范要求
"""
import json

def verify_calculation():
    """验证计算逻辑"""
    
    # 加载默认配置
    with open('d:/RuoYi-Vue-master-reall/ruoyi-admin/src/main/resources/scripts/default_config.json', 'r', encoding='utf-8') as f:
        config = json.load(f)
    
    print("=" * 80)
    print("配置文件验证")
    print("=" * 80)
    
    # 1. 验证总占比是否为100%
    total_ratio = config['regularGrade'] + config['labGrade'] + config['finalExam']
    print(f"\n1. 考核方式总占比检查:")
    print(f"   平时成绩: {config['regularGrade']}%")
    print(f"   上机成绩: {config['labGrade']}%")
    print(f"   期末考核: {config['finalExam']}%")
    print(f"   总计: {total_ratio}%")
    print(f"   结果: {'✓ 通过' if abs(total_ratio - 100) < 0.01 else '✗ 失败'}")
    
    # 2. 验证课程目标比例的总和
    print(f"\n2. 课程目标总贡献值检查:")
    total_contribution = sum(target['total'] for target in config['courseTargetProportions'])
    print(f"   所有课程目标的total之和: {total_contribution}")
    print(f"   结果: {'✓ 通过' if abs(total_contribution - 100) < 0.01 else '✗ 失败'}")
    
    # 3. 验证每个课程目标的行总和
    print(f"\n3. 每个课程目标的行总和检查:")
    for target in config['courseTargetProportions']:
        row_total = target['regularGrade'] + target['finalExam'] + target['lab']
        expected_total = target['total']
        match = abs(row_total - expected_total) < 0.01
        print(f"   {target['courseTarget']}: {row_total} (期望: {expected_total}) {'✓' if match else '✗'}")
    
    # 4. 验证列总和（各考核方式的总贡献）
    print(f"\n4. 各考核方式的列总和检查:")
    regular_sum = sum(target['regularGrade'] for target in config['courseTargetProportions'])
    lab_sum = sum(target['lab'] for target in config['courseTargetProportions'])
    final_sum = sum(target['finalExam'] for target in config['courseTargetProportions'])
    
    print(f"   平时成绩列总和: {regular_sum} (应该等于 regularGrade={config['regularGrade']})")
    print(f"   上机成绩列总和: {lab_sum} (应该等于 labGrade={config['labGrade']})")
    print(f"   期末考核列总和: {final_sum} (应该等于 finalExam={config['finalExam']})")
    
    regular_ok = abs(regular_sum - config['regularGrade']) < 0.01
    lab_ok = abs(lab_sum - config['labGrade']) < 0.01
    final_ok = abs(final_sum - config['finalExam']) < 0.01
    
    print(f"   结果: {'✓ 全部通过' if (regular_ok and lab_ok and final_ok) else '✗ 有错误'}")
    
    # 5. 计算每个课程目标在各考核方式中的满分值
    print(f"\n5. 课程目标在各考核方式中的满分值计算:")
    regular_max_score = config.get('regularTotalScore', 0)
    lab_max_score = config.get('labTotalScore', 0)
    final_max_score = config.get('finalTotalScore', 0)
    
    regular_grade_ratio = config.get('regularGrade', 0)
    lab_grade_ratio = config.get('labGrade', 0)
    final_exam_ratio = config.get('finalExam', 0)
    
    for target in config['courseTargetProportions']:
        target_name = target['courseTarget']
        print(f"\n   {target_name}:")
        
        if target.get('regularGrade', 0) > 0 and regular_grade_ratio > 0:
            contribution = target['regularGrade']
            full_score = regular_max_score * (contribution / regular_grade_ratio)
            percent_in_assessment = (contribution / regular_grade_ratio) * 100
            print(f"     平时成绩: 贡献值={contribution}, 在平时的占比={percent_in_assessment:.1f}%, 满分={full_score:.1f}")
        
        if target.get('lab', 0) > 0 and lab_grade_ratio > 0:
            contribution = target['lab']
            full_score = lab_max_score * (contribution / lab_grade_ratio)
            percent_in_assessment = (contribution / lab_grade_ratio) * 100
            print(f"     上机成绩: 贡献值={contribution}, 在上机的占比={percent_in_assessment:.1f}%, 满分={full_score:.1f}")
        
        if target.get('finalExam', 0) > 0 and final_exam_ratio > 0:
            contribution = target['finalExam']
            full_score = final_max_score * (contribution / final_exam_ratio)
            percent_in_assessment = (contribution / final_exam_ratio) * 100
            print(f"     期末考核: 贡献值={contribution}, 在期末的占比={percent_in_assessment:.1f}%, 满分={full_score:.1f}")
    
    # 6. 验证实例数据
    print(f"\n{'=' * 80}")
    print("实例数据验证（基于规范中的示例）")
    print("=" * 80)
    print("\n假设场景:")
    print("  - 平时成绩占比30%，总分100")
    print("  - 上机成绩占比20%，总分100")
    print("  - 期末考核占比50%，总分100")
    print("  - 课程目标1: 平时占60%(即总成绩18%)，上机占100%(即总成绩20%)，期末占70%(即总成绩35%)")
    print("  - 课程目标2: 平时占40%(即总成绩12%)，期末占30%(即总成绩15%)")
    
    print("\n预期结果:")
    print("  课程目标1:")
    print("    平时成绩: 贡献值=18, 在平时的占比=60%, 满分=100*0.6=60分")
    print("    上机成绩: 贡献值=20, 在上机的占比=100%, 满分=100*1.0=100分")
    print("    期末考核: 贡献值=35, 在期末的占比=70%, 满分=100*0.7=70分")
    print("    总贡献: 18+20+35=73")
    print("  课程目标2:")
    print("    平时成绩: 贡献值=12, 在平时的占比=40%, 满分=100*0.4=40分")
    print("    期末考核: 贡献值=15, 在期末的占比=30%, 满分=100*0.3=30分")
    print("    总贡献: 12+0+15=27")
    
    print("\n实际配置数据:")
    for target in config['courseTargetProportions']:
        print(f"  {target['courseTarget']}:")
        print(f"    regularGrade={target['regularGrade']}, lab={target['lab']}, finalExam={target['finalExam']}, total={target['total']}")
    
    print("\n" + "=" * 80)
    print("验证完成")
    print("=" * 80)

if __name__ == '__main__':
    verify_calculation()
