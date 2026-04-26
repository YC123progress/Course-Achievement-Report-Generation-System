import request from '@/utils/request'

// 使用默认配置
export function loadDefaultConfig() {
  return request({
    url: '/luckysheet/use-default-config',
    method: 'get'
  })
}

// 上传配置文件
export function uploadConfig(data) {
  return request({
    url: '/luckysheet/upload-config',
    method: 'post',
    data: data
  })
}

// 生成Excel模板
export function generateTemplate(configId) {
  return request({
    url: '/luckysheet/generate-template',
    method: 'get',
    params: { configId }
  })
}

// 处理数据 - 设置更长的超时时间
export function processData(excelData, configId) {
  return request({
    url: '/luckysheet/process-data',
    method: 'post',
    params: { configId },
    data: excelData,
    timeout: 300000  // 5分钟超时
  })
}

// 直接处理手动配置数据
export function processDataDirect(data) {
  return request({
    url: '/luckysheet/process-data-direct',
    method: 'post',
    data: data,
    timeout: 300000
  })
}

// 下载报告
export function downloadReport(reportId, configId) {
  return request({
    url: `/luckysheet/download-report/${reportId}`,
    method: 'get',
    params: { configId },
    responseType: 'blob'
  })
}

// 获取结果文件
export function getResultFile(configId, fileName) {
  return request({
    url: `/luckysheet/result/${configId}/${fileName}`,
    method: 'get',
    responseType: 'blob'
  })
} 