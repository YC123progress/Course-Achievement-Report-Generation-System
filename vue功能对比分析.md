# Vue文件功能对比分析报告

## 概述
本报告对比当前 `assessment.vue` 文件与需求描述的功能一致性。

---

## 一、整体结构对比

### ✅ 已实现
1. **步骤式流程**：使用 `currentStep` 变量和 `v-if` 控制显示/隐藏
2. **8个步骤**：实际有8个步骤（比描述的6个多）
   - 步骤1: 考核方式选择 (currentStep === 0)
   - 步骤2: 成绩占比设置 (currentStep === 1)
   - 步骤3: 课程目标设置 (currentStep === 2)
   - 步骤4: 支撑关系设置 (currentStep === 3)
   - 步骤5: 占比分配 (currentStep === 4)
   - 步骤6: 期末试卷命题 (currentStep === 5)
   - 步骤7: 数据录入与处理 (currentStep === 6)
   - 步骤8: 报告下载 (currentStep === 7)

### ❌ 缺失功能
1. **resetInputs() 函数**：页面加载时没有清空所有数值输入并重置复选框为选中状态
   - 当前只在 `mounted()` 中调用 `validateStep1()`
   - 没有初始化复选框默认全部选中

---

## 二、步骤1：选择考核方式

### ✅ 已实现
1. **三个复选框**：regular（平时成绩）、lab（上机成绩）、final（期末考核）
2. **下一步按钮**：触发 `nextStep()` → `proceedToGradeForm()` 的等效功能
3. **根据勾选控制步骤2显示**：通过 `v-if="selectedAssessmentTypes.includes('xxx')"` 实现

### ⚠️ 差异
1. **默认状态**：需求要求"默认全部选中"，但当前代码中 `selectedAssessmentTypes` 初始化为空数组 `[]`
   ```javascript
   selectedAssessmentTypes: [], // 第693行
   ```

---

## 三、步骤2：输入占比与总分

### ✅ 已实现
1. **输入字段**：
   - 平时成绩：占比（proportions.regular）、总分（scores.regular）
   - 上机成绩：占比（proportions.lab）、总分（scores.lab）
   - 期末考核：占比（proportions.final）、总分（scores.final）
2. **校验逻辑**：`validateProportions()` 函数验证总和是否为100%
3. **错误提示**：通过 `totalMessage` 显示验证结果

### ✅ 符合需求
- 未勾选的考核方式对应的div被隐藏
- 仅校验占比和为100%，不校验总分字段

---

## 四、步骤3：输入课程目标数量

### ✅ 已实现
1. **输入字段**：`targetCount`（整数，范围1-10）
2. **生成表格**：`generateTargetTable()` 函数
3. **验证**：检查目标数量是否有效

### ⚠️ 差异
1. **支撑表格行的显示/隐藏**：需求要求根据步骤1的勾选状态显示/隐藏对应行
   - 当前实现：支撑关系表中所有考核方式列都通过 `v-if` 动态显示/隐藏
   - 符合需求

2. **复选框命名**：需求要求name格式为 `{type}Target{i}`
   - 当前实现：使用 `v-model` 绑定到 `supportRelationTable` 数组对象
   - 实现方式不同但功能等效

---

## 五、步骤4：选择课程目标支撑

### ✅ 已实现
1. **用户勾选**：通过复选框选择每个考核方式支撑的课程目标
2. **计算占比**：点击"下一步"触发 `goToStep5()` → 进入步骤5

### ❌ 严重差异 - 核心数据处理逻辑完全不同

#### 需求描述的流程：
```
1. 遍历每个课程目标i（1~n）
2. 对三种考核类型分别检查复选框是否被选中
3. 若选中，弹出prompt让用户输入"该考核分数中课程目标i所占的百分比"
4. 计算：proportion = (用户输入百分比 × 该考核总占比) / 100
5. 将计算结果填入动态表格
```

#### 当前实现的流程：
```
1. 步骤4只负责选择支撑关系（勾选复选框）
2. 步骤5才进行占比分配
3. 用户在表格中直接输入各课程目标在各考核方式中的占比
4. 系统自动计算加权占比：weightedRegular = (regular * proportions.regular) / 100
5. 验证每个考核方式内各课程目标占比之和为100%
```

**关键区别**：
- ❌ **没有使用prompt弹窗**让用户逐个输入
- ❌ **没有在步骤4立即计算占比**
- ✅ **改为在步骤5用表格批量输入**，用户体验更好但不符合需求描述

---

## 六、步骤5：占比分配

### ✅ 已实现
1. **动态表格**：`targetProportionsTable` 展示各课程目标的占比
2. **输入框**：用户可以输入各考核方式的占比
3. **加权计算**：
   ```javascript
   row.weightedRegular = (row.regular * this.proportions.regular) / 100
   row.weightedLab = (row.lab * this.proportions.lab) / 100
   row.weightedFinal = (row.final * this.proportions.final) / 100
   ```
4. **汇总显示**：
   - 各考核方式占比汇总（getRegularTotal, getLabTotal, getFinalTotal）
   - 总加权占比（getTotalWeightedProportion）
5. **验证逻辑**：`calculateTargetProportions()` 验证：
   - 每个考核方式内各课程目标占比之和为100%
   - 总加权占比为100%

### ❌ 与需求描述的差异
1. **需求描述的计算时机**：在步骤4点击"计算课程目标占比"时立即计算
2. **当前实现**：在步骤5手动输入后实时计算加权值
3. **需求描述的验证**：校验行合计之和与列合计之和是否为100%
4. **当前实现**：校验每个考核方式内部占比之和为100%，以及总加权占比为100%

**结论**：计算逻辑数学上等价，但交互流程和验证方式不同

---

## 七、步骤6：期末试卷命题表

### ✅ 已实现
1. **表格结构**：
   - 第一列为"大题"
   - 动态小题列
   - 每行末尾显示大题总分
   - 底部显示试卷总分

2. **单元格内容**：
   - 分值输入框（el-input-number）
   - 课程目标下拉选择框（el-select）

3. **动态行列操作**：
   - `addRow()`：添加大题
   - `removeRow()`：删除大题（至少保留1个）
   - `addColumn()`：添加小题
   - `removeColumn()`：删除小题（至少保留1个）

4. **实时计算**：
   - `updateTotals()`：计算每行总分和试卷总分
   - 分值变化时自动更新

### ❌ 缺失功能

#### 1. toggleZeroScore() 函数
- **需求**：分值为0时添加CSS类zero-score（灰色背景）
- **当前**：没有此样式切换功能

#### 2. 合规性检查函数
需求要求的两个检查函数都**不存在**：

##### checkCompliance() - 命题表合规性
```
规则1：每道大题的第1小题分值必须 > 0
规则2：同一大题内，分值非零的小题必须连续排列在前面
规则3：整个试卷至少有一个小题分值 > 0
```
**当前实现**：`validateExamPaper()` 函数有不同的验证规则：
- 总分是否匹配
- 有分值的题目是否分配了课程目标
- 是否有未覆盖的课程目标
- **没有检查小题连续性规则**

##### checkTotalScoreCompliance() - 总分一致性
```
读取命题表计算的总分calculatedTotalScore
读取步骤2中填写的期末考核总分expectedTotalScore
二者差的绝对值需 < 0.01
```
**当前实现**：在 `validateExamPaper()` 中有类似检查：
```javascript
if (this.totalScore !== this.scores.final) {
  errors.push(`试卷总分${this.totalScore}与期末考核总分${this.scores.final}不匹配`)
}
```
**差异**：使用严格相等 `!==` 而非容差比较 `< 0.01`

---

## 八、生成文件功能

### ❌ 完全缺失

需求描述的 `generateFiles()` 函数及其相关功能**完全不存在**：

#### 1. generateConfig() - 生成 exam_config.json
需求要求生成包含以下字段的JSON文件：
```json
{
  "regularGrade": 30,
  "labGrade": 20,
  "finalExam": 50,
  "regularTotalScore": 100,
  "labTotalScore": 100,
  "finalTotalScore": 100,
  "courseTargets": ["课程目标1", "课程目标2"],
  "courseTargetProportions": [...],
  "examPaper": [...]
}
```
**当前状态**：没有任何生成和下载JSON文件的代码

#### 2. CSV模板生成
需求要求生成三个CSV文件：
- `final_exam_scores_template.csv`：期末考核模板
- `regular_scores_template.csv`：平时成绩模板
- `lab_scores_template.csv`：上机成绩模板

**要求**：
- 添加UTF-8 BOM（\uFEFF）
- 自动下载
- 弹出提示框说明文件用途

**当前状态**：
- 虽然有 `csvHeaders` 数据结构（第790行）
- 但没有生成和下载CSV文件的函数
- 没有BOM处理逻辑
- 没有文件下载功能

#### 3. generateCourseTargetOptions()
需求要求根据courseTargetCount生成选项字符串
**当前实现**：使用computed属性 `targetOptions` 实现相同功能
```javascript
targetOptions() {
  return Array.from({ length: this.targetCount }, (_, i) => `目标${i + 1}`)
}
```

---

## 九、辅助函数对比

| 需求函数 | 当前状态 | 说明 |
|---------|---------|------|
| resetInputs() | ❌ 缺失 | 没有清空输入和重置复选框的函数 |
| generateCourseTargetOptions() | ✅ 已实现 | 以computed属性形式实现 |
| toggleZeroScore() | ❌ 缺失 | 没有0分样式切换功能 |
| updateTotalScores() | ✅ 已实现 | 命名为 `updateTotals()` |
| validateProportions() | ✅ 已实现 | 步骤2的占比验证 |
| calculateTargetProportions() | ✅ 已实现 | 步骤5的占比计算和验证 |
| checkCompliance() | ❌ 缺失 | 没有命题表合规性检查 |
| checkTotalScoreCompliance() | ⚠️ 部分实现 | 在validateExamPaper中有简化版本 |
| generateFiles() | ❌ 完全缺失 | 没有文件生成功能 |
| generateConfig() | ❌ 完全缺失 | 没有JSON配置生成 |

---

## 十、总结

### ✅ 已实现的核心功能
1. 6个主要步骤的流程控制
2. 考核方式选择和占比设置
3. 课程目标数量设置和支撑关系选择
4. 占比分配和加权计算
5. 期末试卷命题表的动态构建
6. 基本的验证逻辑

### ❌ 缺失的关键功能
1. **resetInputs()**：页面初始化时重置所有输入
2. **prompt弹窗输入**：步骤4中没有逐个弹窗输入占比
3. **toggleZeroScore()**：0分值的灰色样式标记
4. **checkCompliance()**：命题表的三项合规性检查规则
5. **generateFiles()**：生成配置文件和CSV模板的完整功能
6. **exam_config.json生成**：配置文件生成和下载
7. **CSV模板生成**：三个成绩模板文件的生成和下载
8. **UTF-8 BOM处理**：CSV文件的BOM添加

### ⚠️ 实现差异
1. **占比计算时机**：需求在步骤4立即计算，当前在步骤5手动输入后计算
2. **交互方式**：需求用prompt逐个输入，当前用表格批量输入
3. **验证规则**：需求检查小题连续性，当前检查目标覆盖
4. **总分比较**：需求用容差比较(<0.01)，当前用严格相等(!==)

---

## 建议

如果要求Vue文件**必须包括描述的所有功能**，需要补充以下内容：

1. 添加 `resetInputs()` 函数并在 `mounted()` 中调用
2. 修改步骤4的逻辑，添加prompt弹窗输入占比
3. 添加 `toggleZeroScore()` 函数和相关CSS样式
4. 实现 `checkCompliance()` 函数的三项规则检查
5. 实现完整的 `generateFiles()` 函数，包括：
   - `generateConfig()` 生成JSON配置
   - 生成三个CSV模板文件
   - 添加UTF-8 BOM
   - 实现文件下载功能
6. 修改总分比较逻辑，使用容差比较
