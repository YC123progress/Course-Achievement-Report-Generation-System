# Course Achievement Report Generation System

## 项目简介

本系统是一个基于SpringBoot架构的课程达成度报告生成系统，旨在帮助教师高效管理课程考核数据、自动生成课程目标达成度分析报告。系统提供可视化的流程式操作界面，支持多种考核方式的灵活配置、课程目标权重分配、期末试卷命题表管理以及数据模板导出等功能。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.x | 后端框架 |
| Vue.js | 2.x | 前端框架 |
| Element UI | - | UI组件库 |
| Maven | - | 项目构建工具 |

## 系统功能

### 核心功能模块

#### 1. 考核方式配置
- 支持平时成绩、上机成绩、期末考核三种考核方式
- 可灵活选择启用/禁用任意考核方式
- 配置各考核方式的占比（总和需为100%）和总分

#### 2. 课程目标管理
- 设置课程目标数量（1-10个）
- 建立课程目标与考核方式的支撑关系
- 分配各课程目标在不同考核方式中的占比

#### 3. 期末试卷命题表
- 动态创建试卷大题和小题结构
- 为每道题分配分值和对应的课程目标
- 自动计算大题总分和试卷总分
- 实时校验试卷总分与配置总分的一致性

#### 4. 数据导出
- 导出考试配置文件（exam_config.json）
- 生成成绩录入模板（CSV格式，含UTF-8 BOM）
  - 期末考核成绩模板
  - 平时成绩模板
  - 上机成绩模板

## 项目结构

```
Course Achievement Report Generation System/
├── backend/                    # SpringBoot后端代码
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java源代码
│   │   │   └── resources/     # 配置文件
│   │   └── test/              # 测试代码
│   └── pom.xml                # Maven配置
├── frontend/                   # Vue前端代码
│   ├── src/
│   │   ├── components/        # Vue组件
│   │   ├── views/             # 页面视图
│   │   └── assets/            # 静态资源
│   └── package.json           # 前端依赖配置
└── docs/                       # 项目文档
```

## 快速开始

### 环境要求

- JDK 1.8 或更高版本
- Maven 3.6+
- Node.js 14+
- npm 或 yarn

### 后端启动

```bash
# 进入后端目录
cd backend

# 编译打包
mvn clean package

# 运行应用
java -jar target/course-achievement-0.0.1-SNAPSHOT.jar
```

后端服务默认运行在 `http://localhost:8080`

### 前端启动

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run serve
```

前端应用默认运行在 `http://localhost:8081`

## API接口说明

### 配置导出接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/export/config` | 导出考试配置文件 |
| POST | `/api/export/template` | 导出成绩录入模板 |

### 请求示例

```json
// POST /api/export/config
{
  "regularGrade": 30,
  "labGrade": 20,
  "finalExam": 50,
  "regularTotalScore": 100,
  "labTotalScore": 100,
  "finalTotalScore": 100,
  "courseTargets": ["目标1", "目标2"],
  "courseTargetProportions": [
    {"regular": 40, "lab": 50, "final": 60},
    {"regular": 60, "lab": 50, "final": 40}
  ],
  "examPaper": [
    {
      "questions": [
        {"score": 10, "target": "目标1"},
        {"score": 15, "target": "目标2"}
      ],
      "totalScore": 25
    }
  ]
}
```

## 操作流程

1. **选择考核方式** - 勾选需要使用的考核类型（默认全选）
2. **设置占比与总分** - 填写各考核方式的占比（总和100%）和总分
3. **设置课程目标** - 输入课程目标数量（1-10个）
4. **建立支撑关系** - 勾选各考核方式支撑的课程目标
5. **分配占比** - 为每个课程目标在各考核方式中分配占比
6. **编辑命题表** - 构建期末试卷结构，分配分值和课程目标
7. **导出文件** - 下载配置文件和成绩录入模板

## 数据验证规则

| 验证项 | 规则 |
|--------|------|
| 占比总和 | 所有启用考核方式的占比之和必须等于100% |
| 考核方式内占比 | 各课程目标在同一考核方式中的占比之和必须为100% |
| 总加权占比 | 所有课程目标的加权占比之和必须为100% |
| 试卷总分 | 命题表计算的总分必须与配置的期末考核总分一致 |
| 课程目标覆盖 | 所有课程目标必须在命题表中至少被分配一道题 |

## 常见问题

### Q: 为什么某些考核方式的输入框不显示？
A: 请检查步骤1中是否勾选了对应的考核方式，未勾选的考核方式对应的配置项会被隐藏。

### Q: 占比总和提示错误怎么办？
A: 请确保所有已勾选考核方式的占比之和等于100%，如果某项考核方式不需要，请在步骤1中取消勾选。

### Q: 生成的CSV文件打开后中文乱码？
A: 系统已自动添加UTF-8 BOM头，如果仍出现乱码，请使用支持UTF-8的编辑器（如Excel需通过数据导入功能选择UTF-8编码打开）。

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0.0 | 2024-01 | 初始版本，实现核心功能 |

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

[MIT License](LICENSE)

## 联系方式

如有问题或建议，请通过以下方式联系：

- 项目仓库：[[GitHub Repository URL](https://github.com/chenyinsu/Course-Achievement-Report-Generation-System)]
---

*本系统为课程达成度报告生成提供完整的解决方案，如有功能需求或改进建议，欢迎反馈。*
