<template>
  <div class="dashboard-wrapper">

    <!-- 顶部欢迎区 -->
    <div class="welcome-header">
      <div class="header-content">
        <div class="badge">
          <i class="el-icon-success"></i>
          <span>系统运行中</span>
        </div>
        <h1 class="main-title">
          {{ greeting }}，{{ userName }}
          <span class="subtitle">欢迎使用课程达成度分析系统</span>
        </h1>
        <p class="description">
          基于算法的课程目标达成度智能分析平台，让每一份数据都转化为教学洞察
        </p>
      </div>

      <div class="header-stats">
        <div class="stat-badge">
          <div class="stat-value">2,847</div>
          <div class="stat-label">累计分析数据</div>
        </div>
        <div class="date-badge">
          <i class="el-icon-time"></i>
          <span>{{ currentDate }}</span>
        </div>
      </div>
    </div>

    <!-- Bento Grid 布局 -->
    <div class="bento-grid">

      <!-- 课程总数 - 大卡片 -->
      <div class="bento-item large primary-card">
        <div class="card-content">
          <div class="icon-wrapper">
            <i class="el-icon-document"></i>
          </div>
          <div class="text-content">
            <h3>
              课程管理
              <span class="highlight">中心</span>
            </h3>
            <p class="desc">
              管理所有课程的考核配置和目标设定，建立完整的评估体系
            </p>
            <div class="stats-row">
              <div class="stat-item">
                <div class="value">15</div>
                <div class="label">活跃课程</div>
              </div>
              <div class="stat-item">
                <div class="value">89%</div>
                <div class="label">平均达成度</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 学生总数 -->
      <div class="bento-item medium accent-card">
        <div class="icon-badge">
          <i class="el-icon-user"></i>
        </div>
        <h3>学生档案</h3>
        <div class="big-number">328</div>
        <p class="label">在读学生</p>
        <div class="footer-note">
          <i class="el-icon-check"></i>
          <span>数据已同步</span>
        </div>
      </div>

      <!-- 考核任务 -->
      <div class="bento-item small task-card">
        <div class="icon-badge cyan">
          <i class="el-icon-edit-outline"></i>
        </div>
        <div class="big-number">42</div>
        <p class="label">待处理任务</p>
      </div>

      <!-- 快速入口 1 -->
      <div class="bento-item medium action-card">
        <div class="action-header">
          <div class="icon-badge green">
            <i class="el-icon-setting"></i>
          </div>
          <h3>考核配置</h3>
        </div>
        <p class="action-desc">
          配置平时、上机、期末三类考核方式及权重分配
        </p>
        <el-button type="text" class="action-link">
          立即配置
          <i class="el-icon-right"></i>
        </el-button>
      </div>

      <!-- 快速入口 2 -->
      <div class="bento-item medium action-card">
        <div class="action-header">
          <div class="icon-badge purple">
            <i class="el-icon-edit-outline"></i>
          </div>
          <h3>目标管理</h3>
        </div>
        <p class="action-desc">
          设置课程目标与考核方式的支撑关系矩阵
        </p>
        <el-button type="text" class="action-link">
          开始设置
          <i class="el-icon-right"></i>
        </el-button>
      </div>

      <!-- 达成度分析 - 宽卡片 -->
      <div class="bento-item wide analysis-card">
        <div class="analysis-content">
          <div class="analysis-text">
            <div class="icon-badge blue">
              <i class="el-icon-data-analysis"></i>
            </div>
            <h3>
              达成度报告
              <span class="highlight">生成</span>
            </h3>
            <p class="desc">
              自动生成课程目标达成度分析报告，支持数据导出和可视化展示
            </p>
          </div>
          <div class="analysis-stats">
            <div class="progress-item">
              <span class="progress-label">数据完整度</span>
              <div class="progress-bar">
                <div class="progress-fill" style="width: 92%"></div>
              </div>
              <span class="progress-value">92%</span>
            </div>
            <div class="progress-item">
              <span class="progress-label">报告生成</span>
              <div class="progress-bar">
                <div class="progress-fill green" style="width: 78%"></div>
              </div>
              <span class="progress-value">78%</span>
            </div>
          </div>
        </div>
      </div>

    </div>

  </div>
</template>

<script>
export default {
  name: "Index",
  data() {
    return {
      currentDate: '',
      userName: '管理员'
    };
  },
  computed: {
    greeting() {
      const hour = new Date().getHours();
      if (hour < 6) return '凌晨好';
      if (hour < 9) return '早上好';
      if (hour < 12) return '上午好';
      if (hour < 14) return '中午好';
      if (hour < 17) return '下午好';
      if (hour < 19) return '傍晚好';
      if (hour < 22) return '晚上好';
      return '夜深了';
    }
  },
  mounted() {
    this.updateDate();
    try {
      const userInfo = this.$store.state.user;
      if (userInfo && userInfo.name) {
        this.userName = userInfo.name;
      }
    } catch (e) {
      console.log('获取用户信息失败');
    }
  },
  methods: {
    updateDate() {
      const now = new Date();
      const year = now.getFullYear();
      const month = String(now.getMonth() + 1).padStart(2, '0');
      const day = String(now.getDate()).padStart(2, '0');
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
      const weekday = weekdays[now.getDay()];
      this.currentDate = `${year}年${month}月${day}日 ${weekday}`;
    }
  }
};
</script>

<style scoped lang="scss">
// Viridis 色系变量（与卡片渐变一致，便于页眉/链接呼应）
$vir-ink: #440154;
$vir-blue: #31688e;
$vir-mid: #21918c;
$vir-mint: #35b779;
$vir-yellow: #fde725;

// 主容器
.dashboard-wrapper {
  min-height: calc(100vh - 84px);
  background: linear-gradient(
    to bottom right,
    #fafaf8 0%,
    #ffffff 45%,
    rgba(49, 104, 142, 0.06) 100%
  );
  padding: 32px;
}

// 顶部欢迎区
.welcome-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 40px;
  gap: 32px;
}

.header-content {
  max-width: 600px;
}

.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(33, 145, 140, 0.12);
  color: $vir-mid;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 20px;

  i {
    font-size: 14px;
  }
}

.main-title {
  font-size: 40px;
  font-weight: 500;
  color: #1c1917;
  margin: 0 0 12px 0;
  line-height: 1.2;
  letter-spacing: -0.5px;

  .subtitle {
    display: block;
    font-size: 16px;
    font-weight: 400;
    color: #78716c;
    margin-top: 8px;
    letter-spacing: 0;
  }
}

.description {
  font-size: 16px;
  color: #57534e;
  line-height: 1.7;
  margin: 0;
}

.header-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-end;
}

.stat-badge {
  background: white;
  padding: 20px 28px;
  border-radius: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(49, 104, 142, 0.12);

  .stat-value {
    font-size: 28px;
    font-weight: 600;
    color: #1c1917;
    margin-bottom: 4px;
  }

  .stat-label {
    font-size: 13px;
    color: #78716c;
  }
}

.date-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #57534e;

  i {
    color: $vir-mid;
  }
}

// Bento Grid 布局（行高略减，主卡仅占一行，整体更紧凑）
.bento-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  grid-auto-rows: minmax(124px, auto);
  grid-auto-flow: dense;
}

// 卡片基础样式
.bento-item {
  background: white;
  border-radius: 24px;
  padding: 28px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(49, 104, 142, 0.1);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  }
}

// 尺寸变体
.large {
  grid-column: span 3;
  grid-row: span 1;
}

.medium {
  grid-column: span 2;
  grid-row: span 1;
}

.small {
  grid-column: span 1;
  grid-row: span 1;
}

// 底部分析区略窄，不占满整行
.wide {
  grid-column: 1 / span 3;
  grid-row: span 1;
  max-width: 100%;
  justify-self: start;
}

// 主卡片（课程管理）Viridis：深紫 → 蓝
.primary-card {
  background: linear-gradient(135deg, $vir-ink 0%, $vir-blue 100%);
  color: #fff;
  padding: 20px 22px;

  .card-content {
    height: 100%;
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    gap: 18px;
    justify-content: flex-start;
  }

  .text-content {
    flex: 1;
    min-width: 0;
  }

  .icon-wrapper {
    width: 48px;
    height: 48px;
    flex-shrink: 0;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
    margin-bottom: 0;

    i {
      font-size: 24px;
    }
  }

  h3 {
    font-size: 20px;
    font-weight: 500;
    margin: 0 0 6px 0;
    line-height: 1.25;

    .highlight {
      font-weight: 300;
      font-style: italic;
      opacity: 0.9;
    }
  }

  .desc {
    font-size: 13px;
    opacity: 0.9;
    line-height: 1.55;
    margin-bottom: 12px;
  }

  .stats-row {
    display: flex;
    gap: 24px;
  }

  .stat-item {
    .value {
      font-size: 26px;
      font-weight: 600;
      margin-bottom: 2px;
    }

    .label {
      font-size: 11px;
      opacity: 0.85;
    }
  }
}

// 强调卡片（学生）青绿
.accent-card {
  background: linear-gradient(135deg, $vir-mint 0%, #2a9d72 100%);
  color: #fff;
  padding: 18px 20px;

  .icon-badge {
    width: 40px;
    height: 40px;
    background: rgba(255, 255, 255, 0.22);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;

    i {
      font-size: 18px;
    }
  }

  h3 {
    font-size: 15px;
    font-weight: 500;
    margin: 0 0 8px 0;
  }

  .big-number {
    font-size: 40px;
    font-weight: 600;
    line-height: 1;
    margin-bottom: 4px;
  }

  .label {
    font-size: 12px;
    opacity: 0.95;
    margin-bottom: 10px;
  }

  .footer-note {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    opacity: 0.9;
  }
}

// 任务卡片（黄绿，深字易读）
.task-card {
  background: linear-gradient(135deg, $vir-yellow 0%, #e6d13a 100%);
  color: $vir-ink;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;

  .icon-badge {
    width: 36px;
    height: 36px;
    background: rgba(68, 1, 84, 0.1);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 8px;

    i {
      font-size: 16px;
      color: $vir-ink;
    }
  }

  .big-number {
    font-size: 34px;
    font-weight: 700;
    line-height: 1;
    margin-bottom: 4px;
  }

  .label {
    font-size: 11px;
    opacity: 0.9;
    margin: 0;
  }
}

// 操作卡片（浅底 + Viridis 中间色块）
.action-card {
  background: linear-gradient(to bottom right, #fbfaf8, #f0f1f2);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px 22px;
  border-color: rgba(49, 104, 142, 0.12);

  .action-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;
  }

  .icon-badge {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    i {
      font-size: 18px;
      color: #fff;
    }

    &.green {
      background: linear-gradient(135deg, $vir-mid 0%, #2a8b97 100%);
    }

    &.purple {
      background: linear-gradient(135deg, $vir-blue 0%, #2c728e 100%);
    }

    &.blue {
      background: linear-gradient(135deg, $vir-mid 0%, $vir-mint 100%);
    }

    &.cyan {
      background: rgba(33, 145, 140, 0.25);
    }
  }

  h3 {
    font-size: 16px;
    font-weight: 500;
    color: #1c1917;
    margin: 0;
  }

  .action-desc {
    font-size: 12px;
    color: #57534e;
    line-height: 1.55;
    margin: 0 0 12px 0;
  }

  .action-link {
    color: $vir-mid;
    font-weight: 500;
    padding: 0;

    &:hover {
      color: $vir-ink;
    }

    i {
      margin-left: 4px;
    }
  }
}

// 分析卡片（青色系 Viridis）
.analysis-card {
  background: linear-gradient(135deg, $vir-mid 0%, #1a6b66 100%);
  color: #fff;
  padding: 20px 22px;

  .analysis-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 100%;
    gap: 20px;
  }

  .analysis-text {
    flex: 1;
    min-width: 0;
  }

  .icon-badge {
    width: 42px;
    height: 42px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
    backdrop-filter: blur(10px);

    i {
      font-size: 20px;
    }
  }

  h3 {
    font-size: 18px;
    font-weight: 500;
    margin: 0 0 6px 0;

    .highlight {
      font-weight: 300;
      font-style: italic;
      opacity: 0.9;
    }
  }

  .desc {
    font-size: 13px;
    opacity: 0.92;
    line-height: 1.55;
    margin: 0;
  }

  .analysis-stats {
    flex: 0 0 min(168px, 40%);
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .progress-item {
    .progress-label {
      display: block;
      font-size: 11px;
      margin-bottom: 5px;
      opacity: 0.9;
    }

    .progress-bar {
      height: 5px;
      background: rgba(255, 255, 255, 0.22);
      border-radius: 999px;
      overflow: hidden;
      margin-bottom: 4px;
    }

    .progress-fill {
      height: 100%;
      background: $vir-yellow;
      border-radius: 999px;
      transition: width 0.6s ease;

      &.green {
        background: $vir-mint;
      }
    }

    .progress-value {
      font-size: 12px;
      font-weight: 600;
    }
  }
}

// 响应式
@media (max-width: 1200px) {
  .bento-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .large {
    grid-column: span 2;
  }

  .wide {
    grid-column: 1 / span 3;
    justify-self: start;
  }
}

@media (max-width: 768px) {
  .dashboard-wrapper {
    padding: 20px;
  }

  .welcome-header {
    flex-direction: column;
  }

  .header-stats {
    align-items: flex-start;
    width: 100%;
  }

  .bento-grid {
    grid-template-columns: 1fr;
    grid-auto-rows: auto;
  }

  .large,
  .medium,
  .small,
  .wide {
    grid-column: span 1;
    grid-row: span 1;
    min-height: 200px;
  }

  .analysis-card .analysis-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .analysis-card .analysis-stats {
    flex: 1;
    width: 100%;
  }
}
</style>
