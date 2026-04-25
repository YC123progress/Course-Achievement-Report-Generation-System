<template>
  <div class="app-container">
    <div class="luckysheet-container">
      <iframe ref="luckysheetFrame" :src="url" frameborder="0" width="100%" height="100%"></iframe>
      <div class="button-group">
        <el-button type="primary" @click="saveData">保存数据</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

export default {
  name: "Luckysheet",
  data() {
    return {
      url: process.env.VUE_APP_BASE_API + "/luckysheet.html",
    };
  },
  methods: {
    saveData() {
      // 检查是否已登录
      const token = getToken();
      if (!token) {
        this.$modal.msgError("请先登录系统");
        this.$router.push('/login');
        return;
      }

      try {
        // 获取iframe中的luckysheet对象
        const luckysheet = this.$refs.luckysheetFrame.contentWindow.luckysheet;
        if (!luckysheet) {
          this.$modal.msgError("未能获取Luckysheet对象，请刷新页面重试");
          return;
        }
        
        // 获取当前工作表数据
        const currentSheet = luckysheet.getSheetData();
        if (!currentSheet) {
          this.$modal.msgError("未能获取工作表数据，请刷新页面重试");
          return;
        }
        
        console.log("工作表数据:", currentSheet);
        
        // 发送数据到后端
        request({
          url: '/system/luckysheet/save',
          method: 'post',
          headers: {
            'Authorization': 'Bearer ' + token
          },
          data: {
            sheetName: currentSheet.name || 'Sheet1',
            cellData: JSON.stringify(currentSheet.celldata || []),
            remark: '保存时间：' + new Date().toLocaleString()
          }
        }).then(response => {
          this.$modal.msgSuccess("保存成功");
        }).catch(error => {
          console.error("保存失败", error);
          if (error.response && error.response.status === 401) {
            this.$modal.msgError("登录已过期，请重新登录");
            this.$router.push('/login');
          } else {
            this.$modal.msgError("保存失败：" + (error.message || '未知错误'));
          }
        });
      } catch (e) {
        console.error("获取表格数据失败", e);
        this.$modal.msgError("获取表格数据失败：" + e.message);
      }
    }
  }
};
</script>

<style scoped>
.app-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 84px);
}

.luckysheet-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.button-group {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  text-align: center;
}
</style> 