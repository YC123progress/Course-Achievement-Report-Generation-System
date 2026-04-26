<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span style="font-size: 18px; font-weight: bold;">课程目标达成评价报告生成工具</span>
      </div>

      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" align-center style="margin-bottom: 30px;">
        <el-step title="考核方式选择" description="选择需要的考核方式" />
        <el-step title="成绩占比设置" description="设置各考核方式的占比和总分" />
        <el-step title="课程目标设置" description="设置课程目标数量和内容" />
        <el-step title="支撑关系设置" description="设置考核方式与课程目标的支撑关系" />
        <el-step title="占比分配" description="分配各课程目标的具体占比" />
        <el-step title="试卷命题" description="设置期末试卷命题表" />
        <el-step title="数据录入与处理" description="填写学生成绩并生成分析结果" />
        <el-step title="报告下载" description="下载评价报告" />
      </el-steps>

      <!-- 步骤1: 考核方式选择 -->
      <div v-if="currentStep === 0" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第一步：考核方式选择</h3>
            <p>请选择本课程使用的考核方式</p>
            
            <el-checkbox-group v-model="selectedAssessmentTypes" @change="validateStep1">
              <el-checkbox label="regular" border>平时成绩</el-checkbox>
              <el-checkbox label="lab" border>上机成绩</el-checkbox>
              <el-checkbox label="final" border>期末考核</el-checkbox>
            </el-checkbox-group>
            
            <div class="step-actions">
              <el-button
                type="primary"
                size="large"
                :disabled="!isStep1Valid"
                @click="nextStep"
              >
                下一步
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤2: 成绩占比设置 -->
      <div v-if="currentStep === 1" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第二步：成绩占比设置</h3>
            <p>请设置各考核方式的占比和总分</p>
            
            <el-form :model="proportions" label-width="120px">
              <div class="input-sections">
                <!-- 平时成绩 -->
                <div v-if="selectedAssessmentTypes.includes('regular')" class="input-section">
                  <h4>平时成绩</h4>
                  <el-form-item label="占比：">
                    <el-input-number 
                      v-model="proportions.regular" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入占比"
                      @change="updateTargetProportions"
                    />%
                  </el-form-item>
                  <el-form-item label="总分：">
                    <el-input-number 
                      v-model="scores.regular" 
                      :min="1" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入总分"
                    />
                  </el-form-item>
                </div>
                
                <!-- 上机成绩 -->
                <div v-if="selectedAssessmentTypes.includes('lab')" class="input-section">
                  <h4>上机成绩</h4>
                  <el-form-item label="占比：">
                    <el-input-number 
                      v-model="proportions.lab" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入占比"
                      @change="updateTargetProportions"
                    />%
                  </el-form-item>
                  <el-form-item label="总分：">
                    <el-input-number 
                      v-model="scores.lab" 
                      :min="1" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入总分"
                    />
                  </el-form-item>
                </div>
                
                <!-- 期末考核 -->
                <div v-if="selectedAssessmentTypes.includes('final')" class="input-section">
                  <h4>期末考核</h4>
                  <el-form-item label="占比：">
                    <el-input-number 
                      v-model="proportions.final" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入占比"
                      @change="updateTargetProportions"
                    />%
                  </el-form-item>
                  <el-form-item label="总分：">
                    <el-input-number 
                      v-model="scores.final" 
                      :min="1" 
                      :step="1" 
                      controls-position="right"
                      placeholder="请输入总分"
                    />
                  </el-form-item>
                </div>
              </div>
            </el-form>
            
            <div class="validation-result" style="margin-top: 20px;">
              <el-alert 
                :title="totalMessage"
                :type="totalMessage.includes('错误') ? 'error' : (totalMessage.includes('成功') ? 'success' : 'info')"
                show-icon
                :closable="false"
              />
            </div>
            
            <div class="step-actions" style="margin-top: 20px;">
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="validateProportions">验证占比</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤3: 课程目标设置 -->
      <div v-if="currentStep === 2" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第三步：课程目标设置</h3>
            <p>请设置课程目标数量</p>
            
            <el-form :model="{ targetCount }" label-width="120px">
              <el-form-item label="课程目标数量：">
                <el-input-number 
                  v-model="targetCount" 
                  :min="1" 
                  :max="10" 
                  :step="1" 
                  controls-position="right"
                />
              </el-form-item>
            </el-form>
            
            <div class="step-actions">
              <el-button @click="prevStep">上一步</el-button>
              <el-button 
                type="primary" 
                @click="generateTargetTable"
                :disabled="targetCount < 1 || targetCount > 10"
              >
                下一步
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤4: 支撑关系设置 -->
      <div v-if="currentStep === 3" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第四步：支撑关系设置</h3>
            <p>请为每个考核方式选择支撑的课程目标（至少选择一个）</p>
            
            <!-- 添加验证提示 -->
            <el-alert
              v-if="!hasSupportRelation"
              title="请至少为一个考核方式设置支撑关系"
              type="warning"
              :closable="false"
              style="margin-bottom: 15px;"
            />
            
            <el-table :data="supportRelationTable" style="width: 100%; margin-bottom: 20px;">
              <el-table-column prop="target" label="课程目标" width="120">
                <template slot-scope="scope">
                  目标{{ scope.$index + 1 }}
                </template>
              </el-table-column>
              
              <el-table-column v-if="selectedAssessmentTypes.includes('regular')" label="平时成绩" width="120">
                <template slot-scope="scope">
                  <el-checkbox v-model="scope.row.regular" @change="checkSupportRelations"></el-checkbox>
                </template>
              </el-table-column>
              
              <el-table-column v-if="selectedAssessmentTypes.includes('lab')" label="上机成绩" width="120">
                <template slot-scope="scope">
                  <el-checkbox v-model="scope.row.lab" @change="checkSupportRelations"></el-checkbox>
                </template>
              </el-table-column>
              
              <el-table-column v-if="selectedAssessmentTypes.includes('final')" label="期末考核" width="120">
                <template slot-scope="scope">
                  <el-checkbox v-model="scope.row.final" @change="checkSupportRelations"></el-checkbox>
                </template>
              </el-table-column>
            </el-table>
            
            <div class="step-actions">
              <el-button @click="prevStep">上一步</el-button>
              <el-button 
                type="primary" 
                @click="goToStep5"
                :disabled="!hasSupportRelation"
              >
                下一步
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤5: 占比分配 -->
      <div v-if="currentStep === 4" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第五步：占比分配</h3>
            <p>请为每个课程目标分配在各考核方式中的占比（每个考核方式内各课程目标占比之和必须为100%）</p>
            
            <!-- 加权计算说明 -->
            <el-alert
              title="加权计算说明"
              description="表格中输入的占比会乘以考核方式占比的加权系数，确保最终总占比为100%"
              type="info"
              style="margin-bottom: 20px;"
              :closable="false"
            />
            
            <el-table :data="targetProportionsTable" style="width: 100%; margin-bottom: 20px;">
              <el-table-column prop="target" label="课程目标" width="120">
                <template slot-scope="scope">
                  目标{{ scope.$index + 1 }}
                </template>
              </el-table-column>
              <el-table-column v-if="selectedAssessmentTypes.includes('regular')" label="平时成绩占比" width="180">
                <template slot-scope="scope">
                  <div>
                    <el-input-number 
                      v-model="scope.row.regular" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      :disabled="!scope.row.regularSupported"
                      controls-position="right"
                      @change="updateTargetProportions"
                    />%
                  </div>
                  <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                    加权后: {{ scope.row.weightedRegular.toFixed(2) }}%
                  </div>
                </template>
              </el-table-column>
              <el-table-column v-if="selectedAssessmentTypes.includes('lab')" label="上机成绩占比" width="180">
                <template slot-scope="scope">
                  <div>
                    <el-input-number 
                      v-model="scope.row.lab" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      :disabled="!scope.row.labSupported"
                      controls-position="right"
                      @change="updateTargetProportions"
                    />%
                  </div>
                  <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                    加权后: {{ scope.row.weightedLab.toFixed(2) }}%
                  </div>
                </template>
              </el-table-column>
              <el-table-column v-if="selectedAssessmentTypes.includes('final')" label="期末考核占比" width="180">
                <template slot-scope="scope">
                  <div>
                    <el-input-number 
                      v-model="scope.row.final" 
                      :min="0" 
                      :max="100" 
                      :step="1" 
                      :disabled="!scope.row.finalSupported"
                      controls-position="right"
                      @change="updateTargetProportions"
                    />%
                  </div>
                  <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                    加权后: {{ scope.row.weightedFinal.toFixed(2) }}%
                  </div>
                </template>
              </el-table-column>
            </el-table>
            
            <!-- 各考核方式占比汇总 -->
            <div class="proportion-summary" style="margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px;">
              <p><strong>各考核方式课程目标占比汇总：</strong></p>
              <p v-if="selectedAssessmentTypes.includes('regular')" :style="{color: getRegularTotal() === 100 ? '#67C23A' : '#F56C6C'}">
                平时成绩：{{ getRegularTotal().toFixed(2) }}% {{ getRegularTotal() === 100 ? '✓' : '✗ (应为100%)' }}
              </p>
              <p v-if="selectedAssessmentTypes.includes('lab')" :style="{color: getLabTotal() === 100 ? '#67C23A' : '#F56C6C'}">
                上机成绩：{{ getLabTotal().toFixed(2) }}% {{ getLabTotal() === 100 ? '✓' : '✗ (应为100%)' }}
              </p>
              <p v-if="selectedAssessmentTypes.includes('final')" :style="{color: getFinalTotal() === 100 ? '#67C23A' : '#F56C6C'}">
                期末考核：{{ getFinalTotal().toFixed(2) }}% {{ getFinalTotal() === 100 ? '✓' : '✗ (应为100%)' }}
              </p>
            </div>
            
            <!-- 考核方式占比汇总信息 -->
            <div class="proportion-summary" style="margin-bottom: 20px; padding: 15px; background: #f5f7fa; border-radius: 4px;">
              <p><strong>考核方式占比设置（来自步骤2）：</strong></p>
              <p v-if="selectedAssessmentTypes.includes('regular')">
                平时成绩占比: {{ proportions.regular }}% (加权系数: {{ (proportions.regular / 100).toFixed(2) }})
              </p>
              <p v-if="selectedAssessmentTypes.includes('lab')">
                上机成绩占比: {{ proportions.lab }}% (加权系数: {{ (proportions.lab / 100).toFixed(2) }})
              </p>
              <p v-if="selectedAssessmentTypes.includes('final')">
                期末考核占比: {{ proportions.final }}% (加权系数: {{ (proportions.final / 100).toFixed(2) }})
              </p>
              <p style="margin-top: 10px; font-weight: bold; color: #67c23a;">
                当前总加权占比: {{ getTotalWeightedProportion().toFixed(2) }}%
              </p>
            </div>
            
            <div class="validation-result">
              <el-alert 
                :title="proportionError"
                :type="proportionError.includes('错误') ? 'error' : 'success'"
                show-icon
                :closable="false"
              />
            </div>
            
            <div class="step-actions">
              <el-button @click="prevStep">上一步</el-button>
              <el-button type="primary" @click="validateAndProceed">验证并继续</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤6: 期末试卷命题 -->
      <div v-if="currentStep === 5" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第六步：期末试卷命题</h3>
            <p v-if="selectedAssessmentTypes.includes('final')">请设置期末试卷的大题和小题</p>
            <p v-else>您未选择期末考核，跳过此步骤</p>
            
            <div v-if="selectedAssessmentTypes.includes('final')">
              <!-- 试卷设置界面 -->
              <div class="table-controls">
                <el-button type="primary" @click="addRow">添加大题</el-button>
                <el-button type="danger" @click="removeRow" :disabled="rows.length <= 1">删除大题</el-button>
                <el-button type="primary" @click="addColumn">添加小题</el-button>
                <el-button type="danger" @click="removeColumn" :disabled="columns <= 1">删除小题</el-button>
              </div>
              
              <!-- 修复表格渲染 -->
              <el-table :data="rows" style="width: 100%; margin-top: 20px;" border>
                <el-table-column label="大题" width="80" align="center">
                  <template slot-scope="scope">
                    大题{{ scope.$index + 1 }}
                  </template>
                </el-table-column>
                
                <!-- 动态生成小题列 -->
                <el-table-column 
                  v-for="(col, colIndex) in columns" 
                  :key="colIndex"
                  :label="`小题${colIndex + 1}`" 
                  width="180"
                  align="center"
                >
                  <template slot-scope="scope">
                    <div style="margin-bottom: 10px;">
                      <el-input-number
                        v-model="scope.row.cells[colIndex].score"
                        :min="0"
                        :max="100"
                        :step="1"
                        size="small"
                        placeholder="分值"
                        @change="updateTotals"
                      />
                    </div>
                    <div>
                      <el-select
                        v-model="scope.row.cells[colIndex].target"
                        placeholder="选择目标"
                        size="small"
                        style="width: 100%"
                        @change="updateTotals"
                      >
                        <el-option
                          v-for="target in targetOptions"
                          :key="target"
                          :label="target"
                          :value="target"
                        />
                      </el-select>
                    </div>
                  </template>
                </el-table-column>
                
                <el-table-column label="大题总分" width="100" align="center">
                  <template slot-scope="scope">
                    <span style="font-weight: bold; color: #409EFF;">{{ scope.row.total }}</span>
                  </template>
                </el-table-column>
              </el-table>
              
              <div class="total-score">
                <h4>试卷总分: {{ totalScore }} / {{ scores.final }} 分</h4>
              </div>
              
              <div class="validation-result">
                <el-alert 
                  :title="paperValidationMessage"
                  :type="paperValidationType"
                  show-icon
                  :closable="false"
                  style="white-space: pre-line; line-height: 1.8;"
                />
              </div>
            </div>
            
            <div class="step-actions">
              <el-button @click="prevStep">上一步</el-button>
              <el-button 
                type="primary" 
                @click="handleStep6Next"
                :loading="step6.loading"
                :disabled="step6.loading"
              >
                {{ step6.loading ? '正在生成数据...' : '下一步' }}
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤7: 数据录入与处理 -->
      <div v-if="currentStep === 6" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <!-- 数据处理状态 -->
            <div v-if="dataProcessing.status === 'processing' || 
                      dataProcessing.status === 'saving' ||
                      dataProcessing.status === 'extracting' ||
                      dataProcessing.status === 'validating' ||
                      dataProcessing.status === 'submitting'">
              <div style="text-align: center; padding: 40px; background: #f8f9fa; border-radius: 8px; margin-bottom: 20px;">
                <el-progress
                  :percentage="dataProcessing.progress"
                  :status="dataProcessing.status === 'error' ? 'exception' : undefined"
                  :stroke-width="12"
                  style="max-width: 500px; margin: 0 auto;"
                />
                <h3 style="margin: 20px 0 10px; color: #409EFF;">{{ dataProcessing.message }}</h3>
                <p v-if="dataProcessing.details" style="color: #606266; margin-top: 10px;">
                  {{ dataProcessing.details }}
                </p>
              </div>
              
              <!-- 显示正在处理的表格预览（可选） -->
              <div v-if="dataProcessing.currentSheet" class="processing-preview">
                <el-alert 
                  :title="`正在处理: ${dataProcessing.currentSheet}`"
                  type="info"
                  :closable="false"
                  style="margin-bottom: 15px;"
                />
              </div>
            </div>
            
            <!-- 数据录入界面（非处理状态时显示） -->
            <div v-else>
              <h3>第七步：数据录入与处理</h3>
              
              <!-- 处理状态提示 -->
              <div v-if="dataProcessing.status !== 'idle'" class="processing-result">
                <el-alert 
                  :title="dataProcessing.message"
                  :type="dataProcessing.status === 'error' ? 'error' : 'success'"
                  :closable="dataProcessing.status === 'error'"
                  style="margin-bottom: 15px;"
                  @close="dataProcessing.status = 'idle'"
                />
              </div>
              
              <!-- 加载状态 -->
              <div v-if="step7.isLoading" class="loading-overlay">
                <div style="text-align: center; padding: 40px;">
                  <el-progress
                    type="circle"
                    :percentage="70"
                    :status="undefined"
                    :width="80"
                  />
                  <p style="margin-top: 15px; color: #606266;">正在加载表格模板...</p>
                </div>
              </div>
              
              <!-- 表格容器 -->
              <div class="luckysheet-fullscreen">
                <iframe
                  ref="luckysheetFrame"
                  :src="luckysheetUrl"
                  frameborder="0"
                  width="100%"
                  height="100%"
                  @load="onLuckysheetLoad"
                ></iframe>
              </div>
              
              <el-alert
                title="使用说明"
                description="请直接在表格中填写学生数据，第一行为表头，从第二行开始填写实际学生信息。填写完成后点击'开始处理数据'按钮。"
                type="info"
                :closable="false"
                style="margin-bottom: 15px;"
              />
              
              <!-- 数据状态统计 -->
              <div v-if="table.isReady && table.lastValidCheck" class="data-statistics">
                <el-card shadow="never" style="margin-bottom: 15px;">
                  <div slot="header">
                    <span style="font-weight: bold;">数据统计</span>
                  </div>
                  <div style="display: flex; justify-content: space-around; text-align: center;">
                    <div>
                      <div style="font-size: 24px; color: #409EFF; font-weight: bold;">
                        {{ dataProcessing.extractedData ? Object.keys(dataProcessing.extractedData.sheets || {}).length : 0 }}
                      </div>
                      <div style="color: #909399;">表格数量</div>
                    </div>
                    <div>
                      <div style="font-size: 24px; color: #67C23A; font-weight: bold;">
                        {{ getStudentCount() }}
                      </div>
                      <div style="color: #909399;">学生数量</div>
                    </div>
                    <div>
                      <div style="font-size: 24px; color: #E6A23C; font-weight: bold;">
                        {{ getLastCheckTime() }}
                      </div>
                      <div style="color: #909399;">最后检查</div>
                    </div>
                  </div>
                </el-card>
              </div>
              
              <div class="step-actions">
                <el-button @click="prevStep" :disabled="dataProcessing.status !== 'idle'">上一步</el-button>
                <el-button 
                  type="primary" 
                  @click="processData"
                  :loading="dataProcessing.status !== 'idle'"
                  :disabled="!table.isReady"
                  icon="el-icon-caret-right"
                >
                  {{ getProcessButtonText() }}
                </el-button>
                <el-button 
                  type="danger" 
                  @click="clearSheetData"
                  :disabled="dataProcessing.status !== 'idle'"
                  icon="el-icon-delete"
                >
                  清除数据
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 步骤8：结果展示和报告下载 -->
      <div v-if="currentStep === 7" class="box-card">
        <el-row :gutter="20">
          <el-col :span="24">
            <h3>第八步：结果展示与报告下载</h3>

            <!-- 统计图表展示 -->
            <el-tabs v-model="activeTab" type="card">
              <el-tab-pane label="成绩分布图" name="gradeDistribution">
                <div style="text-align: center;">
                  <img
                    v-if="results.gradeDistributionChart"
                    :src="getImageUrl(results.gradeDistributionChart)"
                    style="max-width: 100%; height: auto;"
                    alt="成绩分布图"
                    @error="handleImageError($event, results.gradeDistributionChart)"
                  >
                  <p v-else>暂无成绩分布图</p>
                </div>
              </el-tab-pane>

              <el-tab-pane label="达成度柱状图" name="achievementBar">
                <div style="text-align: center;">
                  <img
                    v-if="results.achievementBarChart"
                    :src="getImageUrl(results.achievementBarChart)"
                    style="max-width: 100%; height: auto;"
                    alt="达成度柱状图"
                    @error="handleImageError($event, results.achievementBarChart)"
                  >
                  <p v-else>暂无达成度柱状图</p>
                </div>
              </el-tab-pane>

              <el-tab-pane label="课程目标散点图" name="scatterCharts">
                <div style="text-align: center;">
                  <div v-if="results.scatterCharts && results.scatterCharts.length > 0">
                    <div
                      v-for="(chart, index) in results.scatterCharts"
                      :key="index"
                      style="margin-bottom: 20px;"
                    >
                      <h4>{{ chart.replace('_achievement_scatter_chart.png', '').replace('_', ' ') }}</h4>
                      <img
                        :src="getImageUrl(chart)"
                        style="max-width: 100%; height: auto;"
                        :alt="chart"
                        @error="handleImageError($event, chart)"
                      >
                    </div>
                  </div>
                  <p v-else>暂无课程目标散点图</p>
                </div>
              </el-tab-pane>
            </el-tabs>

            <!-- 下载按钮 -->
            <div style="margin-top: 30px; text-align: center;">
              <el-button
                type="success"
                size="large"
                icon="el-icon-download"
                @click="downloadReport"
              >
                下载完整报告 (Word格式)
              </el-button>

              <el-button
                type="primary"
                size="large"
                icon="el-icon-refresh"
                @click="restart"
              >
                重新开始
              </el-button>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script>
import { loadDefaultConfig, generateTemplate, processData, uploadConfig, processDataDirect } from '@/api/luckysheet/assessment'

export default {
  name: 'Assessment',
  data() {
    return {
      // 步骤控制
      currentStep: 0,
      loading: false,

      // 考核方式选择
      selectedAssessmentTypes: ['regular', 'lab', 'final'],
      isStep1Valid: false,
      
      // 考核配置
      proportions: {
        regular: 0,
        lab: 0,
        final: 0
      },
      scores: {
        regular: 0,
        lab: 0,
        final: 0 
      },
      totalMessage: '',
      
      // 课程目标
      targetCount: 2,
      supportRelationTable: [],
      
      // 占比分配
      targetProportionsTable: [],
      proportionError: '',
      
      // 期末试卷
      rows: [],
      columns: 1,
      totalScore: 0,
      paperValidationMessage: '请设置试卷',
      paperValidationType: 'info',
      isStep6Valid: false,

      // LuckySheet 相关
      luckysheetUrl: '/luckysheet.html',

      // 配置文件
      examConfig: null,
    
      // 步骤6专用状态
      step6: {
        loading: false,
        lastValidation: null
      },
      
      // 步骤7专用状态
      step7: {
        isInitialized: true,
        isLoading: true,
        hasData: false,
        luckysheetReady: false,
        lastError: null
      },

      // LuckySheet 数据
      luckysheetData: null,

      cachedLuckysheet: null,   // 缓存实例

      // ========== 统一的数据处理状态 ==========
      dataProcessing: {
        // 处理状态
        status: 'idle', // 'idle' | 'saving' | 'extracting' | 'validating' | 'submitting' | 'processing' | 'success' | 'error'
        progress: 0,
        message: '',
        details: '',
        currentSheet: '',
        
        // 数据状态
        hasSavedData: false,
        lastSaveTime: null,
        configId: '',
        reportId: '',
        
        // 提取的数据
        extractedData: null,
        
        // 处理结果
        results: null,
      },
      
      // ========== 表格相关状态 ==========
      table: {
        isReady: false,
        isDataValid: false,
        lastValidCheck: null,
        backupKey: 'luckysheet_backup_data',
        studentCount: 0,
      },

      activeTab: 'gradeDistribution',
      
      // 其他配置
      finalConfig: null,
      csvHeaders: {
        final: [],
        regular: [],
        lab: []
      }
    }
  },
  
  computed: {
    targetOptions() {
      return Array.from({ length: this.targetCount }, (_, i) => `目标${i + 1}`)
    },

    // 验证是否有支撑关系
    hasSupportRelation() {
      return this.supportRelationTable.some(row => 
        row.regular || row.lab || row.final
      )
    },

    // 新增：处理按钮文本
    processButtonText() {
      const status = this.dataProcessing.status
      
      const texts = {
        'idle': '开始处理数据',
        'saving': '正在保存...',
        'extracting': '正在提取...',
        'validating': '正在验证...',
        'processing': '正在处理...',
        'await': '请稍候...',
        'error': '处理失败',
        'success': '处理完成'
      }
      
      return texts[status] || '开始处理数据'
    },

    results() {
      return this.dataProcessing.results || {}
    }
  },
  
  watch: {
    // 监听 proportions 变化，自动重新计算加权占比
    proportions: {
      handler(newVal, oldVal) {
        // 避免初始化时触发
        if (oldVal && (newVal.regular !== oldVal.regular || 
                       newVal.lab !== oldVal.lab || 
                       newVal.final !== oldVal.final)) {
          console.log('proportions 发生变化，重新计算加权占比', newVal)
          this.updateTargetProportions()
        }
      },
      deep: true
    }
  },
  
  mounted() {
    document.title = '课程目标达成评价报告 - 课程达成度分析系统'
    this.validateStep1()
    
    // 监听页面刷新或关闭
    window.addEventListener('beforeunload', this.saveOnUnload)
    
    // 检查是否有未保存的数据
    this.checkForSavedDataOnLoad()
  },

  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener('beforeunload', this.saveOnUnload)
  },
  
  methods: {
    // ========== 步骤导航方法 ==========
    nextStep() {
      if (!this.validateCurrentStep()) {
        return
      }

      // 特殊处理：步骤7（数据录入与处理）跳过，因为处理完成后会直接跳转到步骤8
      if (this.currentStep === 6) {
        console.log('步骤7不执行常规跳转，通过processData()处理')
        return
      }
      
      // 执行步骤特定的初始化
      this.initializeNextStep()
      
      this.currentStep++
      
      // 如果进入步骤7，确保初始化
      if (this.currentStep === 6) {
        this.$nextTick(() => {
          setTimeout(() => {
            this.initializeStep7()
          }, 500)
        })
      }
    },
    
    prevStep() {
      if (this.currentStep === 6) {
        this.saveTableData();  // 返回前保存
      }
      if (this.currentStep > 0) {
        this.currentStep--
      }
    },
    
    // 统一的步骤验证函数
    validateCurrentStep() {
      switch (this.currentStep) {
        case 0: // 步骤1
          return this.selectedAssessmentTypes.length > 0
          
        case 1: // 步骤2
          return this.totalMessage.includes('验证成功')
          
        case 2: // 步骤3
          return this.targetCount >= 1 && this.targetCount <= 10
          
        case 3: // 步骤4
          return this.hasSupportRelation
          
        case 4: // 步骤5
          return this.proportionError.includes('验证成功')
        
        case 5: // 步骤6
          if (this.selectedAssessmentTypes.includes('final')) {
            const validationResult = this.validateExamPaper()
            // 允许有警告但继续，只有错误才阻止
            return validationResult.errors.length === 0
          }
          return true
          
        case 6: // 步骤7（数据录入与处理）
          // 只有在非处理状态时才能离开（返回上一步）
          return this.dataProcessing.status === 'idle' || 
                this.dataProcessing.status === 'success' || 
                this.dataProcessing.status === 'error'

        case 7: // 步骤8（结果展示）- 现在是步骤8
          return this.dataProcessing.results && Object.keys(this.dataProcessing.results).length > 0
        
        default:
          return true
      }
    },
    
    // 初始化下一步骤
    initializeNextStep() {
      switch (this.currentStep) {
        case 0: // 步骤1 -> 步骤2
          // 不需要特殊初始化
          break
          
        case 1: // 步骤2 -> 步骤3
          // 不需要特殊初始化
          break
          
        case 2: // 步骤3 -> 步骤4
          this.generateTargetTable()
          break
          
        case 3: // 步骤4 -> 步骤5
          console.log('从步骤4进入步骤5，初始化目标占比表')
          this.initTargetProportionsTable()
          break
          
        case 4: // 步骤5 -> 步骤6
          if (this.selectedAssessmentTypes.includes('final')) {
            this.initializeExamTable()
          }
          break
          
        case 5: // 步骤6 -> 步骤7
          this.step7.isInitialized = false
          break
          
        case 6: // 步骤7 -> 步骤8
          // 不需要特殊初始化，processData会处理
          break
          
        case 7: // 步骤8 -> 步骤9
          // 不需要特殊初始化，processData会自动跳转
          break
      }
    },

    // 页面刷新或关闭前保存数据
    saveOnUnload(event) {
      if (this.currentStep === 6 || this.currentStep === 7) {
        this.saveTableData();
      }
    },
    
    // 页面加载时检查是否有保存的数据
    checkForSavedDataOnLoad() {
      const savedData = localStorage.getItem('luckysheet_backup_data')
      if (savedData) {
        const parsedData = JSON.parse(savedData)
        const now = new Date().getTime()
        const oneDay = 24 * 60 * 60 * 1000
        
        // 如果数据在一天内保存的，显示恢复提示
        if (now - parsedData.timestamp < oneDay) {
          this.$message.info('检测到上次未完成的表格数据，已自动恢复')
          // 设置标记，在进入步骤7时恢复
          this.hasSavedDataToRestore = true
        }
      }
    },
    
    // ========== 步骤1: 考核方式选择 ==========
    validateStep1() {
      this.isStep1Valid = this.selectedAssessmentTypes.length > 0
      console.log('选中的考核方式:', this.selectedAssessmentTypes)
    },
    
    // ========== 步骤2: 成绩占比设置 ==========
    validateProportions() {
      const total = this.proportions.regular + this.proportions.lab + this.proportions.final
      if (total === 100) {
        this.totalMessage = '验证成功：占比设置正确'
        this.nextStep()
      } else {
        this.totalMessage = `错误：总占比必须为100%，当前为${total}%`
      }
    },
    
    // ========== 步骤3: 课程目标设置 ==========
    generateTargetTable() {
      // 先验证目标数量
      if (this.targetCount < 1 || this.targetCount > 10) {
        this.$message.error('请设置有效的课程目标数量（1-10）')
        return
      }
      
      // 生成支撑关系表
      this.supportRelationTable = Array.from({ length: this.targetCount }, (_, index) => ({
        target: `目标${index + 1}`,
        regular: true,
        lab: true,
        final: true
      }))
      
      // 直接进入下一步，不调用 nextStep() 避免重复验证
      this.currentStep++
    },

    // ========== 步骤4: 支撑关系设置 ==========
    goToStep5() {
      console.log('点击进入步骤5，当前支撑关系表:', this.supportRelationTable)
      
      // 重新验证支撑关系
      const hasSupportRelation = this.supportRelationTable && 
        this.supportRelationTable.some(row => 
          (row.regular !== undefined && row.regular) || 
          (row.lab !== undefined && row.lab) || 
          (row.final !== undefined && row.final)
        )
      
      console.log('支撑关系验证结果:', hasSupportRelation)
      
      if (!hasSupportRelation) {
        this.$message.error('请至少为一个考核方式设置课程目标支撑关系')
        return
      }
      
      // 使用统一的下一步方法
      this.nextStep()
    },

    checkSupportRelations() {
      console.log('支撑关系发生变化:', this.supportRelationTable)
      // 这里可以添加额外的验证逻辑
    },

    // ========== 步骤5: 占比分配 ==========
    initTargetProportionsTable() {
      console.log('初始化目标占比表，目标数量:', this.targetCount)
      console.log('支撑关系表:', this.supportRelationTable)
      
      this.targetProportionsTable = this.supportRelationTable.map((row, index) => {
        // 计算该目标支持的考核方式数量
        const supportedTypes = []
        if (row.regular) supportedTypes.push('regular')
        if (row.lab) supportedTypes.push('lab')
        if (row.final) supportedTypes.push('final')
        
        // 默认值设置为0，由用户手动输入
        const targetProportion = {
          target: `目标${index + 1}`,
          regular: 0,
          lab: 0,
          final: 0,
          regularSupported: row.regular,
          labSupported: row.lab,
          finalSupported: row.final,
          weightedRegular: 0,
          weightedLab: 0,
          weightedFinal: 0,
          weightedTotal: 0
        }
        
        console.log(`目标${index + 1}配置:`, targetProportion)
        return targetProportion
      })
      
      // 立即更新加权占比
      this.updateTargetProportions()
      
      console.log('最终目标占比表:', this.targetProportionsTable)
    },
    
    updateTargetProportions() {
      console.log('=== updateTargetProportions 被调用 ===')
      console.log('当前 proportions:', this.proportions)
      console.log('targetProportionsTable:', this.targetProportionsTable)
      
      this.targetProportionsTable.forEach(row => {
        // 计算加权后的占比
        // 加权占比 = 输入占比 × 考核方式占比 / 100
        row.weightedRegular = (row.regular * this.proportions.regular) / 100
        row.weightedLab = (row.lab * this.proportions.lab) / 100
        row.weightedFinal = (row.final * this.proportions.final) / 100
        
        // 计算总加权占比
        row.weightedTotal = row.weightedRegular + row.weightedLab + row.weightedFinal
        
        console.log(`${row.target} 计算结果:`, {
          regular: row.regular,
          lab: row.lab,
          final: row.final,
          weightedRegular: row.weightedRegular,
          weightedLab: row.weightedLab,
          weightedFinal: row.weightedFinal,
          weightedTotal: row.weightedTotal
        })
      })
      
      console.log('更新后的 targetProportionsTable:', this.targetProportionsTable)
    },

    // 获取总加权占比
    getTotalWeightedProportion() {
      return this.targetProportionsTable.reduce((sum, row) => sum + row.weightedTotal, 0)
    },
    
    // 获取平时成绩输入占比总和
    getRegularTotal() {
      return this.targetProportionsTable
        .filter(row => row.regularSupported)
        .reduce((sum, row) => sum + (row.regular || 0), 0)
    },
    
    // 获取上机成绩输入占比总和
    getLabTotal() {
      return this.targetProportionsTable
        .filter(row => row.labSupported)
        .reduce((sum, row) => sum + (row.lab || 0), 0)
    },
    
    // 获取期末考核输入占比总和
    getFinalTotal() {
      return this.targetProportionsTable
        .filter(row => row.finalSupported)
        .reduce((sum, row) => sum + (row.final || 0), 0)
    },
    
    calculateTargetProportions() {
      this.updateTargetProportions()
      
      let isValid = true
      const errorMessages = []
      
      // 验证每个考核方式内部各课程目标的输入占比之和是否为100%
      
      // 验证平时成绩
      if (this.selectedAssessmentTypes.includes('regular')) {
        const regularTotal = this.targetProportionsTable
          .filter(row => row.regularSupported)
          .reduce((sum, row) => sum + (row.regular || 0), 0)
        
        if (Math.abs(regularTotal - 100) > 0.01) {
          isValid = false
          errorMessages.push(`平时成绩中各课程目标输入占比之和为${regularTotal.toFixed(2)}%（应为100%）`)
        }
      }
      
      // 验证上机成绩
      if (this.selectedAssessmentTypes.includes('lab')) {
        const labTotal = this.targetProportionsTable
          .filter(row => row.labSupported)
          .reduce((sum, row) => sum + (row.lab || 0), 0)
        
        if (Math.abs(labTotal - 100) > 0.01) {
          isValid = false
          errorMessages.push(`上机成绩中各课程目标输入占比之和为${labTotal.toFixed(2)}%（应为100%）`)
        }
      }
      
      // 验证期末考核
      if (this.selectedAssessmentTypes.includes('final')) {
        const finalTotal = this.targetProportionsTable
          .filter(row => row.finalSupported)
          .reduce((sum, row) => sum + (row.final || 0), 0)
        
        if (Math.abs(finalTotal - 100) > 0.01) {
          isValid = false
          errorMessages.push(`期末考核中各课程目标输入占比之和为${finalTotal.toFixed(2)}%（应为100%）`)
        }
      }
      
      // 验证总加权占比是否为100%
      const totalWeighted = this.getTotalWeightedProportion()
      if (Math.abs(totalWeighted - 100) > 0.01) {
        isValid = false
        errorMessages.push(`总加权占比为${totalWeighted.toFixed(2)}%（应为100%）`)
      }
      
      // 更新验证消息
      if (isValid) {
        this.proportionError = '验证成功：各考核方式内课程目标占比分配正确，总加权占比为100%'
      } else {
        this.proportionError = `错误：${errorMessages.join('；')}`
      }

      return isValid // 返回验证结果
    },

    // 步骤5的验证并继续
    validateAndProceed() {
      try {
        console.log('=== 开始步骤5验证 ===')
        
        // 执行验证
        const isValid = this.calculateTargetProportions()
        
        console.log('验证结果:', isValid)
        console.log('错误信息:', this.proportionError)
        console.log('当前步骤:', this.currentStep)
        
        if (isValid && this.proportionError.includes('验证成功')) {
          console.log('验证通过，准备跳转')
          
          // 如果是步骤5，需要初始化试卷表格（如果选择了期末考核）
          if (this.currentStep === 4 && this.selectedAssessmentTypes.includes('final')) {
            this.initializeExamTable()
          }
          
          // 使用统一的下一步方法
          this.nextStep()
          
          console.log('跳转后步骤:', this.currentStep)
        } else {
          console.log('验证未通过')
          this.$message.warning('请先完成占比分配验证')
        }
        
      } catch (error) {
        console.error('验证过程出错:', error)
        this.$message.error('验证过程发生错误: ' + error.message)
      }
    },
    
    // ========== 步骤6: 期末试卷命题相关方法 ==========
    addRow() {
      const newRow = {
        cells: Array(this.columns).fill().map(() => ({ 
          score: 0, 
          target: '' 
        })),
        total: 0
      }
      this.rows.push(newRow)
      this.updateTotals()
    },

    removeRow() {
      if (this.rows.length > 1) {
        this.rows.pop()
        this.updateTotals()
      } else {
        this.$message.warning('至少需要保留一个大题')
      }
    },

    addColumn() {
      this.columns++
      this.rows.forEach(row => {
        row.cells.push({ score: 0, target: '' })
      })
      this.updateTotals()
    },

    removeColumn() {
      if (this.columns > 1) {
        this.columns--
        this.rows.forEach(row => {
          row.cells.pop()
        })
        this.updateTotals()
      } else {
        this.$message.warning('至少需要保留一个小题')
      }
    },

    updateTotals() {
      // 计算每行总分
      this.rows.forEach(row => {
        row.cells.forEach(cell => {
          // 如果分值为0，自动清除目标选择
          if (cell.score <= 0 && cell.target) {
            cell.target = ''
          }
        })
        row.total = row.cells.reduce((sum, cell) => {
          const score = Number(cell.score) || 0
          return sum + score
        }, 0)
      })
      
      // 计算试卷总分
      this.totalScore = this.rows.reduce((sum, row) => sum + row.total, 0)
      
      // 验证试卷
      this.validateExamPaper()
    },

    // 试卷表格初始化
    initializeExamTable() {
      console.log('初始化试卷表格')
      
      // 如果还没有行数据，初始化默认的试卷结构
      if (this.rows.length === 0) {
        this.rows = [
          {
            cells: Array(this.columns).fill().map(() => ({ 
              score: 0, 
              target: '' 
            })),
            total: 0
          }
        ]
      }
      
      // 更新总分
      this.updateTotals()
      
      console.log('试卷表格初始化完成，行数:', this.rows.length)
    },

    // 统一的试卷验证函数
    validateExamPaper() {
      const errors = []
      const warnings = []
      
      // ========== 新增：严格合规性检查（仅当选择了期末考核时生效）==========
      if (this.selectedAssessmentTypes.includes('final')) {
        // 规则1：每道大题的第1小题分值必须 > 0
        this.rows.forEach((row, rowIndex) => {
          if (row.cells.length > 0 && row.cells[0].score <= 0) {
            errors.push(`大题${rowIndex + 1}的第1小题分值必须大于0（当前为${row.cells[0].score}分）`)
          }
        })
        
        // 规则2：同一大题内，分值非零的小题必须连续排列在前面
        this.rows.forEach((row, rowIndex) => {
          let foundZero = false
          row.cells.forEach((cell, colIndex) => {
            if (cell.score <= 0) {
              foundZero = true
            } else if (foundZero && cell.score > 0) {
              errors.push(`大题${rowIndex + 1}的小题${colIndex + 1}存在分值间断：非零小题必须连续排列在前面（不允许出现"0分小题之后又出现非零分小题"的情况）`)
            }
          })
        })
        
        // 规则3：整个试卷至少有一个小题分值 > 0
        const hasValidQuestion = this.rows.some(row => 
          row.cells.some(cell => cell.score > 0)
        )
        if (!hasValidQuestion && this.rows.length > 0) {
          errors.push('试卷中至少需要有一个小题的分值大于0')
        }
      }
      
      // ========== 原有验证逻辑 ==========
      
      // 1. 总分验证（使用容差比较，避免浮点数精度问题）
      if (this.selectedAssessmentTypes.includes('final')) {
        const scoreDiff = Math.abs(this.totalScore - this.scores.final)
        if (scoreDiff > 0.01) {
          errors.push(`试卷总分${this.totalScore.toFixed(2)}与期末考核总分${this.scores.final}不匹配（差值：${scoreDiff.toFixed(2)}）`)
        }
      }
      
      // 2. 目标分配验证
      let hasUnassignedTarget = false
      let totalQuestions = 0
      const targetScoreMap = new Map()
      
      this.rows.forEach((row, rowIndex) => {
        row.cells.forEach((cell, colIndex) => {
          if (cell.score > 0) {
            totalQuestions++
            
            if (!cell.target) {
              hasUnassignedTarget = true
              errors.push(`大题${rowIndex + 1}小题${colIndex + 1}有分值但未分配课程目标`)
            } else {
              // 统计各目标分值
              const currentScore = targetScoreMap.get(cell.target) || 0
              targetScoreMap.set(cell.target, currentScore + cell.score)
            }
          }
        })
      })
      
      // 3. 题目数量验证
      if (totalQuestions === 0 && this.rows.length > 0) {
        errors.push('试卷中没有任何有分值的题目')
      }
      
      // 4. 目标覆盖验证
      const supportedTargets = this.supportRelationTable
        .filter(row => row.final)
        .map(row => row.target)
      
      const usedTargets = Array.from(targetScoreMap.keys())
      const uncoveredTargets = supportedTargets.filter(target => 
        !usedTargets.includes(target)
      )
      
      if (uncoveredTargets.length > 0) {
        warnings.push(`以下课程目标在试卷中未覆盖: ${uncoveredTargets.join(', ')}`)
      }
      
      // 更新验证状态
      if (errors.length === 0) {
        if (warnings.length > 0) {
          this.paperValidationMessage = `试卷设置基本正确！\n${warnings.join('\n')}`
        } else {
          this.paperValidationMessage = '试卷设置正确！总分匹配且所有题目分配合理'
        }
        this.paperValidationType = warnings.length > 0 ? 'warning' : 'success'
        this.isStep6Valid = true
      } else {
        // 将错误和警告分行显示
        const errorLines = errors.map((err, index) => `${index + 1}. ${err}`)
        const warningLines = warnings.length > 0 
          ? ['\n提示：', ...warnings.map((warn, index) => `${index + 1}. ${warn}`)]
          : []
        
        this.paperValidationMessage = [
          `发现${errors.length}个问题：`,
          ...errorLines,
          ...warningLines
        ].join('\n')
        
        this.paperValidationType = 'error'
        this.isStep6Valid = false
      }
      
      return {
        isValid: errors.length === 0,
        errors,
        warnings,
        totalQuestions,
        targetCoverage: {
          supported: supportedTargets,
          used: usedTargets,
          uncovered: uncoveredTargets
        }
      }
    },

    // 步骤6的下一步处理
    async handleStep6Next() {
      if (this.step6.loading) return
      
      try {
        this.step6.loading = true
        
        // 执行验证
        const validationResult = this.validateExamPaper()
        
        console.log('步骤6验证结果:', validationResult)
        
        // 如果有错误，不允许进入下一步
        if (!validationResult.isValid) {
          this.$message.error('试卷设置存在问题，请修正后再继续')
          this.step6.loading = false
          return
        }
        
        // 清除备份数据，避免恢复旧数据
        localStorage.removeItem(this.table.backupKey)
        // 直接跳转到步骤7
        this.currentStep = 6
        this.$nextTick(() => {
          setTimeout(() => {
            this.initializeStep7()
          }, 100)
        })
      } catch (error) {
        console.error('步骤6处理失败:', error)
        this.$message.error('跳转失败: ' + error.message)
      } finally {
        this.step6.loading = false
      }
    },

    // 验证期末试卷结构
    validateFinalExamStructure() {
      if (!this.selectedAssessmentTypes.includes('final')) {
        return { isValid: true, message: '未选择期末考核' }
      }
      
      const issues = []
      
      // 检查是否有大题
      if (this.rows.length === 0) {
        issues.push('请至少添加一个大题')
      }
      
      // 检查是否有小题
      let totalQuestions = 0
      this.rows.forEach((row, index) => {
        const questionsInRow = row.cells.filter(cell => cell.score > 0).length
        totalQuestions += questionsInRow
        
        if (questionsInRow === 0) {
          issues.push(`大题${index + 1}没有设置小题`)
        }
      })
      
      if (totalQuestions === 0) {
        issues.push('试卷中没有设置任何小题')
      }
      
      // 检查总分匹配
      if (this.totalScore !== this.scores.final) {
        issues.push(`试卷总分${this.totalScore}与设置的期末总分${this.scores.final}不匹配`)
      }
      
      return {
        isValid: issues.length === 0,
        message: issues.length > 0 ? issues.join('；') : '试卷结构正确',
        issues,
        totalQuestions
      }
    },

    // 确保课程配置生成
    ensureCourseConfig() {
      if (!this.examConfig) {
        this.examConfig = this.generateCourseConfig()
      }
      return this.examConfig
    },

    // 统一的配置生成函数
    generateCourseConfig() {
      // 计算各考核方式占比
      const regularGrade = this.proportions.regular || 0
      const labGrade = this.proportions.lab || 0
      const finalExam = this.proportions.final || 0

      // 计算课程目标占比
      const courseTargetProportions = this.targetProportionsTable.map(row => ({
        courseTarget: row.target,
        regularGrade: row.weightedRegular,
        lab: row.weightedLab,
        finalExam: row.weightedFinal,
        total: row.weightedTotal
      }))

      const config = {
        proportions: {
          regular: regularGrade,
          lab: labGrade,
          final: finalExam
        },
        scores: {
          regular: this.scores.regular || 0,
          lab: this.scores.lab || 0,
          final: this.scores.final || 0
        },
        targets: courseTargetProportions,
        assessmentTypes: [...this.selectedAssessmentTypes],
        // 不再需要 examPaper 字段，因为直接从 rows 生成
        hasFinalExam: this.selectedAssessmentTypes.includes('final')
      }
      
      console.log('生成的简化课程配置:', config)
      console.log('courseTargetProportions详情:', courseTargetProportions)
      return config
    },

    // 显示试卷问题警告
    async showExamPaperWarning(validationResult) {
      const errorCount = validationResult.errors.length
      const warningCount = validationResult.warnings.length
      
      let message = ''
      if (errorCount > 0) {
        message = `发现 ${errorCount} 个错误问题，${warningCount} 个警告：\n\n`
        message += validationResult.errors.slice(0, 3).map(err => `• ${err}`).join('\n')
        if (errorCount > 3) {
          message += `\n... 还有 ${errorCount - 3} 个错误`
        }
        if (warningCount > 0) {
          message += `\n\n警告：\n`
          message += validationResult.warnings.slice(0, 2).map(warn => `• ${warn}`).join('\n')
        }
      } else if (warningCount > 0) {
        message = `发现 ${warningCount} 个警告问题：\n\n`
        message += validationResult.warnings.slice(0, 5).map(warn => `• ${warn}`).join('\n')
        if (warningCount > 5) {
          message += `\n... 还有 ${warningCount - 5} 个警告`
        }
      }
      
      message += '\n\n是否继续生成数据模板？'
      
      try {
        await this.$confirm(
          message,
          '试卷设置警告',
          {
            confirmButtonText: '继续生成',
            cancelButtonText: '返回修改',
            type: errorCount > 0 ? 'error' : 'warning',
            dangerouslyUseHTMLString: false
          }
        )
        return true
      } catch {
        return false
      }
    },

    // 创建普通考核sheet
    createAssessmentSheet(type, name, config) {
      const baseHeaders = ['班级', '学号', '姓名', '成绩']
      
      return {
        name: name,
        color: type === 'regular' ? '#409EFF' : type === 'lab' ? '#67C23A' : '#E6A23C',
        index: this.luckysheetData.length,
        status: 1,
        order: this.luckysheetData.length,
        celldata: [
          // 表头行
          { r: 0, c: 0, v: '班级' },
          { r: 0, c: 1, v: '学号' },
          { r: 0, c: 2, v: '姓名' },
          { r: 0, c: 3, v: '成绩' }
        ],
        config: {
          columnlen: { 0: 100, 1: 120, 2: 80, 3: 90 }
        }
      }
    },

    // 创建期末考核sheet（特殊处理）
    createFinalExamSheet(type, name, config) {
      const headers = ['班级', '学号', '姓名']
      
      // 添加试卷题目列
      if (config.examPaper && config.examPaper.sections) {
        config.examPaper.sections.forEach(section => {
          section.questions.forEach(question => {
            headers.push(`大题${section.section}.${question.number}`)
          })
        })
      }
      
      return {
        name: name,
        celldata: this.generateCellData(headers),
        config: {
          columnlen: this.generateColumnConfig(headers),
          rowlen: 40
        }
      }
    },

    // 生成单元格数据
    generateCellData(headers) {
      const celldata = []
      
      // 生成表头行
      headers.forEach((header, colIndex) => {
        celldata.push({
          r: 0,
          c: colIndex,
          v: header
        })
      })
      
      return celldata
    },

    // 生成列配置
    generateColumnConfig(headers) {
      const columnConfig = {}
      headers.forEach((header, index) => {
        if (index === 0) columnConfig[index] = 100   // 班级列
        else if (index === 1) columnConfig[index] = 120 // 学号列
        else if (index === 2) columnConfig[index] = 80  // 姓名列
        else columnConfig[index] = 90 // 成绩列
      })
      return columnConfig
    },

    // 添加调试方法
    checkLuckySheetStatus() {
      console.log('LuckySheet 状态检查:')
      console.log('- window.luckysheet:', !!window.luckysheet)
      console.log('- 容器元素:', document.getElementById('luckysheet'))
      console.log('- 步骤7状态:', this.step7)
      console.log('- 数据:', this.luckysheetData)
    },

    // ========== 步骤7: 数据处理相关方法 ==========
    
    // 统一的步骤7初始化
    initializeStep7() {
      console.log('开始初始化步骤7')
      
      // 检查是否需要恢复数据而不是重新初始化
      if (this.needsDataRestore) {
        console.log('检测到需要恢复数据，跳过重新初始化')
        this.step7.isLoading = true
        return
      }
      
      this.step7.isLoading = true
      
      // 生成模板数据（现在只是用于配置验证）
      this.generateLuckysheetTemplateData()
      
      // 重置iframe以触发重新加载
      this.luckysheetUrl = process.env.VUE_APP_BASE_API + "/luckysheet.html?t=" + Date.now()
    },

    // 数据存在性检查方法
    async checkIfDataExists() {
      try {
        const iframe = this.$refs.luckysheetFrame
        if (!iframe || !iframe.contentWindow) {
          console.log('iframe未就绪')
          return false
        }
        
        const luckysheet = iframe.contentWindow.luckysheet
        if (!luckysheet) {
          console.log('luckysheet实例未就绪')
          return false
        }
        
        const allSheets = luckysheet.getAllSheets()
        if (!allSheets || allSheets.length === 0) {
          console.log('没有找到任何sheet')
          return false
        }
        
        // 简化数据检查逻辑 - 只要有非表头数据就认为有数据
        let hasValidData = false
        
        for (const sheet of allSheets) {
          const sheetData = luckysheet.getSheetData(sheet.id)
          if (sheetData && sheetData.length > 1) {
            // 从第二行开始检查（跳过表头）
            for (let i = 1; i < sheetData.length; i++) {
              const row = sheetData[i]
              if (row && Array.isArray(row)) {
                // 检查是否有任何非空单元格
                for (let j = 0; j < row.length; j++) {
                  const cell = row[j]
                  if (cell && cell.v !== undefined && cell.v !== '' && cell.v != null) {
                    hasValidData = true
                    console.log(`发现有效数据: sheet=${sheet.name}, row=${i}, col=${j}, value=`, cell.v)
                    break
                  }
                }
                if (hasValidData) break
              }
            }
          }
          if (hasValidData) break
        }
        
        console.log('数据存在性检查结果:', hasValidData)
        return hasValidData
        
      } catch (error) {
        console.error('检查数据存在性失败:', error)
        return false
      }
    },

    // 备用数据检查方法
    fallbackDataCheck(luckysheet, allSheets) {
      try {
        // 通过获取所有单元格数据来检查
        let hasData = false
        
        allSheets.forEach(sheet => {
          const range = luckysheet.getRange()
          if (range && range.length > 1) {
            hasData = true
          }
        })
        
        if (hasData) {
          console.log('备用检查发现数据')
          this.step7.hasData = true
        }
      } catch (error) {
        console.warn('备用数据检查失败:', error)
      }
    },

    // 向Luckysheet加载数据
    async loadDataToLuckysheet() {
      return new Promise((resolve, reject) => {
        try {
          const iframe = this.$refs.luckysheetFrame
          const luckysheet = iframe.contentWindow.luckysheet
          
          // 生成Luckysheet格式的数据
          const luckysheetData = this.generateLuckysheetData()
          console.log('生成的Luckysheet数据:', luckysheetData)
          
          // 销毁现有实例（如果有）
          if (typeof luckysheet.destroy === 'function') {
            luckysheet.destroy()
          }
          
          // 延迟创建以确保DOM就绪
          setTimeout(() => {
            try {
              // 创建新的Luckysheet实例
              luckysheet.create({
                container: 'luckysheet',
                lang: 'zh',
                data: luckysheetData,
                title: '学生成绩数据表',
                userInfo: false,
                showtoolbar: true,
                showinfobar: false,
                showsheetbar: true,
                showstatisticBar: true,
                cellRightClickConfig: {
                  copy: true,
                  copyAs: true,
                  paste: true,
                  insertRow: true,
                  insertColumn: true,
                  deleteRow: true,
                  deleteColumn: true,
                  deleteCell: true,
                  hideRow: true,
                  hideColumn: true,
                  rowHeight: true,
                  columnWidth: true,
                  clear: true
                }
              })
              
              console.log('Luckysheet创建成功，加载了', luckysheetData.length, '个sheet')
              
              // 加载完成后检查是否有现有数据
              setTimeout(() => {
                this.checkIfDataExists()
                resolve()
              }, 500)
              
            } catch (error) {
              reject(new Error('创建Luckysheet失败: ' + error.message))
            }
          }, 500)
          
        } catch (error) {
          reject(new Error('加载数据到Luckysheet失败: ' + error.message))
        }
      })
    },

    // 生成Luckysheet格式的数据
    generateLuckysheetData() {
      const config = this.ensureCourseConfig()
      const sheets = []
      
      // 为每个选中的考核方式创建sheet
      if (this.selectedAssessmentTypes.includes('regular')) {
        sheets.push(this.createRegularSheet())
      }
      
      if (this.selectedAssessmentTypes.includes('lab')) {
        sheets.push(this.createLabSheet())
      }
      
      if (this.selectedAssessmentTypes.includes('final')) {
        sheets.push(this.createFinalSheet(config))
      }
      
      return sheets
    },

    // 创建平时成绩sheet - 严格按照CSV格式
    createRegularSheet() {
      const headers = ['班级', '学号', '姓名', '平时成绩总分']
      const celldata = []
      
      // 只生成表头和一空行数据
      const data = [
        headers,
        ['', '', '', ''] // 空行供填写
      ]
      
      // 转换为Luckysheet格式
      data.forEach((row, rowIndex) => {
        row.forEach((cell, colIndex) => {
          celldata.push({
            r: rowIndex,
            c: colIndex,
            v: cell
          })
        })
      })
      
      return {
        name: '平时成绩',
        color: '#409EFF',
        index: 0,
        status: 1,
        order: 0,
        celldata: celldata,
        config: {
          columnlen: {
            0: 100,
            1: 120,
            2: 80,
            3: 90
          }
        }
      }
    },

    // 创建上机成绩sheet - 严格按照CSV格式
    createLabSheet() {
      const headers = ['班级', '学号', '姓名', '上机成绩总分']
      const celldata = []
      
      const data = [
        headers,
        ['', '', '', ''] // 空行供填写
      ]
      
      data.forEach((row, rowIndex) => {
        row.forEach((cell, colIndex) => {
          celldata.push({
            r: rowIndex,
            c: colIndex,
            v: cell
          })
        })
      })
      
      return {
        name: '上机成绩',
        color: '#67C23A',
        index: 1,
        status: 1,
        order: 1,
        celldata: celldata,
        config: {
          columnlen: {
            0: 100,
            1: 120,
            2: 80,
            3: 90
          }
        }
      }
    },

    // 创建期末成绩sheet
    createFinalSheet(config) {
      const headers = ['班级', '学号', '姓名']
      const celldata = []
      
      console.log('生成期末成绩表，直接使用rows数据')
      console.log('rows数据:', this.rows)
      
      // 直接从rows数据生成列头 - 去掉对config.examPaper的依赖
      this.generateHeadersFromRows(headers)
      
      console.log('最终表头:', headers)
      
      // 生成表头行
      headers.forEach((header, colIndex) => {
        celldata.push({
          r: 0, // 第1行
          c: colIndex,
          v: header
        })
      })
      
      // 生成一行空数据行（供填写成绩）
      headers.forEach((_, colIndex) => {
        celldata.push({
          r: 1, // 第2行
          c: colIndex,
          v: '' // 空值，等待用户填写
        })
      })
      
      // 生成列宽配置
      const columnlen = {}
      headers.forEach((header, index) => {
        if (index === 0) columnlen[index] = 100   // 班级列
        else if (index === 1) columnlen[index] = 120 // 学号列
        else if (index === 2) columnlen[index] = 80  // 姓名列
        else columnlen[index] = 120 // 小题列（更宽以显示完整格式）
      })
      
      return {
        name: '期末成绩',
        color: '#E6A23C',
        index: 2,
        status: 1,
        order: 2,
        celldata: celldata,
        config: {
          columnlen: columnlen,
          rowlen: 25 // 行高
        }
      }
    },
  
    // 新增备用列头生成方法
    generateHeadersFromRows(headers) {
      console.log('从rows数据生成列头，rows:', this.rows)
      
      if (this.rows && this.rows.length > 0) {
        this.rows.forEach((row, rowIndex) => {
          if (row.cells && Array.isArray(row.cells)) {
            row.cells.forEach((cell, cellIndex) => {
              // 只生成有分值的题目列头
              if (cell && cell.score > 0) {
                const header = `${rowIndex + 1}.${cellIndex + 1}（${cell.score}分）`
                headers.push(header)
                console.log(`从rows生成列头: ${header}`)
              }
            })
          }
        })
      }
      
      // 如果还是没有列头，添加默认列确保表格结构完整
      if (headers.length <= 3) {
        console.log('rows数据为空或没有有效题目，添加默认列头')
        for (let i = 1; i <= 5; i++) {
          headers.push(`题目${i}（0分）`)
        }
      }
      
      console.log('生成的列头数量:', headers.length - 3)
    },

    // 同时需要修改试卷验证方法，确保小题号正确
    updateTotals() {
      // 计算每行总分
      this.rows.forEach(row => {
        row.cells.forEach((cell, index) => {
          // 如果分值为0，自动清除目标选择
          if (cell.score <= 0 && cell.target) {
            cell.target = ''
          }
          // 确保小题号正确（从1开始）
          cell.questionNumber = index + 1
        })
        row.total = row.cells.reduce((sum, cell) => {
          const score = Number(cell.score) || 0
          return sum + score
        }, 0)
      })
      
      // 计算试卷总分
      this.totalScore = this.rows.reduce((sum, row) => sum + row.total, 0)
      
      // 验证试卷
      this.validateExamPaper()
    },

    // 生成LuckySheet模板数据（清理后）
    generateLuckysheetTemplateData() {
      try {
        const config = this.ensureCourseConfig()
        
        // 构建完整的表格配置
        const templateConfig = {
          proportions: config.proportions,
          scores: config.scores,
          targets: config.targets,
          assessmentTypes: config.assessmentTypes
          // 不再需要 sheets 字段，因为数据会通过 generateLuckysheetData() 生成
        }
        
        this.examConfig = templateConfig
        console.log('生成的模板配置:', this.examConfig)
        
        return templateConfig
        
      } catch (error) {
        console.error('生成模板数据失败:', error)
        throw new Error(`模板数据生成失败: ${error.message}`)
      }
    },

    // 生成表头单元格数据
    generateHeaderCells(headers) {
      const celldata = [];
      
      headers.forEach((header, colIndex) => {
        celldata.push({
          r: 0, // 行索引，0表示第一行
          c: colIndex, // 列索引
          v: {
            v: header, // 单元格值
            m: header, // 显示值
            ct: { fa: "General", t: "g" }, // 单元格格式
            bg: '#f0f9ff', // 背景色
            fs: 12, // 字体大小
            ff: 'Microsoft YaHei', // 字体
            bl: 1, // 粗体
            it: 0, // 斜体
            vt: 0 // 垂直对齐
          }
        });
      });
      
      return celldata;
    },

    // 生成列宽配置
    generateColumnWidths(headers) {
      const columnlen = {};
      headers.forEach((_, index) => {
        if (index === 0) columnlen[index] = 100;   // 班级列
        else if (index === 1) columnlen[index] = 120; // 学号列
        else if (index === 2) columnlen[index] = 80;  // 姓名列
        else columnlen[index] = 90; // 成绩列
      });
      return columnlen;
    },

    // 获取sheet名称列表
    getSheetNames() {
      return this.luckysheetData.map(sheet => sheet.name).join('、');
    },

    // 初始化LuckySheet并加载数据
    async initializeLuckySheetWithData() {
      return new Promise((resolve, reject) => {
        // 等待LuckySheet加载完成
        const checkLuckySheet = () => {
          const iframe = this.$refs.luckysheetFrame;
          if (!iframe || !iframe.contentWindow) {
            setTimeout(checkLuckySheet, 100);
            return;
          }
          
          try {
            const luckysheet = iframe.contentWindow.luckysheet;
            if (!luckysheet) {
              setTimeout(checkLuckySheet, 100);
              return;
            }
            
            // 销毁现有实例
            if (typeof luckysheet.destroy === 'function') {
              luckysheet.destroy();
            }
            
            // 创建新实例并加载数据
            setTimeout(() => {
              try {
                luckysheet.create({
                  container: 'luckysheet',
                  lang: 'zh',
                  plugins: ['chart'],
                  data: this.luckysheetData,
                  title: '学生成绩数据表',
                  userInfo: false,
                  userMenuItem: [],
                  showtoolbar: true,
                  showinfobar: false,
                  showsheetbar: true,
                  showstatisticBar: true
                });
                
                console.log('LuckySheet 初始化成功，加载了', this.luckysheetData.length, '个sheet');
                resolve();
              } catch (error) {
                reject(error);
              }
            }, 500);
            
          } catch (error) {
            reject(error);
          }
        };
        
        checkLuckySheet();
        
        // 设置超时
        setTimeout(() => {
          reject(new Error('LuckySheet 初始化超时'));
        }, 10000);
      });
    },

    // 带重试机制的LuckySheet初始化
    async initializeLuckysheetWithRetry(maxRetries = 3) {
      for (let attempt = 1; attempt <= maxRetries; attempt++) {
        try {
          console.log(`LuckySheet初始化尝试 ${attempt}/${maxRetries}`)
          
          const luckysheet = this.getLuckysheetInstance()
          if (!luckysheet) {
            throw new Error('LuckySheet实例未找到')
          }
          
          // 检查是否已有数据，避免重复初始化
          const hasExistingData = await this.checkExistingData()
          if (hasExistingData) {
            console.log('检测到已有数据，保留现有数据')
            this.step7.hasData = true
            return true
          }
          
          // 加载模板数据
          await this.loadTemplateData(luckysheet)
          this.step7.hasData = false // 新加载的模板没有学生数据
          
          console.log('LuckySheet初始化成功')
          return true
          
        } catch (error) {
          console.warn(`LuckySheet初始化尝试 ${attempt} 失败:`, error)
          
          if (attempt === maxRetries) {
            throw new Error(`LuckySheet初始化失败，已重试${maxRetries}次: ${error.message}`)
          }
          
          // 等待一段时间后重试
          await new Promise(resolve => setTimeout(resolve, 1000 * attempt))
        }
      }
    },

    // 获取LuckySheet实例
    getLuckysheetInstance() {
      try {
        const iframe = this.$refs.luckysheetFrame
        if (!iframe || !iframe.contentWindow) {
          throw new Error('LuckySheet iframe 未找到或未加载完成')
        }
        
        const luckysheet = iframe.contentWindow.luckysheet
        if (!luckysheet) {
          throw new Error('LuckySheet 全局对象不存在')
        }
        
        return luckysheet
      } catch (error) {
        console.error('获取 LuckySheet 实例时出错:', error)
        return null
      }
    },

    // 检查是否已有数据
    async checkExistingData() {
      const luckysheet = this.getLuckysheetInstance()
      if (!luckysheet) return false
      
      const allSheets = luckysheet.getAllSheets()
      if (!allSheets || allSheets.length === 0) return false
      
      // 检查是否有实际的学生数据（不仅仅是表头）
      for (const sheet of allSheets) {
        const data = luckysheet.getSheetData(sheet.id)
        if (data && data.length > 1) {
          // 检查第2行（索引1）是否有数据
          const firstDataRow = data[1]
          if (firstDataRow && firstDataRow.length >= 3) {
            const classInfo = firstDataRow[0] && firstDataRow[0].v
            const studentId = firstDataRow[1] && firstDataRow[1].v
            const studentName = firstDataRow[2] && firstDataRow[2].v
            
            if (classInfo || studentId || studentName) {
              return true
            }
          }
        }
      }
      
      return false
    },

    // 加载模板数据
    async loadTemplateData(luckysheet) {
      return new Promise((resolve, reject) => {
        try {
          const container = 'luckysheet'
          
          // 销毁现有实例
          if (typeof luckysheet.destroy === 'function') {
            luckysheet.destroy()
          }
          
          // 重新创建表格
          setTimeout(() => {
            try {
              luckysheet.create({
                container: container,
                data: this.luckysheetData,
                title: '学生成绩数据表',
                lang: 'zh'
              })
              resolve()
            } catch (error) {
              reject(error)
            }
          }, 500)
          
        } catch (error) {
          reject(error)
        }
      })
    },

    // 提取行数据
    extractRowData(row) {
      if (!row) return []
      
      const result = []
      const maxColumns = Math.max(...this.luckysheetData.map(sheet => 
        sheet.celldata.filter(cell => cell.r === 0).length
      )) || 10
      
      for (let i = 0; i < maxColumns; i++) {
        const cell = row[i]
        if (cell && typeof cell === 'object' && cell.v !== undefined) {
          result.push(cell.v)
        } else {
          result.push(cell || '')
        }
      }
      return result
    },

    // 检查是否有有效的学生数据
    hasValidStudentData(rowData) {
      if (!rowData || rowData.length < 3) return false
      
      const [studentClass, studentId, studentName, ...scores] = rowData
      
      // 必须有学号或姓名
      if (!studentId && !studentName) return false
      
      // 必须有至少一个成绩数据（且不是空字符串）
      return scores.some(score => {
        // 检查是否为有效数字且大于0
        const scoreNum = Number(score)
        return !isNaN(scoreNum) && scoreNum > 0
      })
    },

    // 验证学生数据
    async validateStudentData(extractedData) {
      const issues = []
      let totalStudents = 0
      const studentMap = new Map()
      
      // 检查是否有任何数据
      if (Object.keys(extractedData).length === 0) {
        return {
          isValid: false,
          message: '没有找到任何数据',
          issues: ['请在各表格中填写学生成绩数据'],
          studentCount: 0,
          sheetCount: 0
        }
      }
      
      for (const [sheetName, sheetData] of Object.entries(extractedData)) {
        const { headers, rows } = sheetData
        
        if (!headers || headers.length < 3) {
          issues.push(`${sheetName}: 表头不完整，至少需要班级、学号、姓名`)
          continue
        }
        
        if (rows.length === 0) {
          issues.push(`${sheetName}: 没有学生数据`)
          continue
        }
        
        // 验证数据行
        let sheetStudentCount = 0
        rows.forEach((row, index) => {
          const rowNumber = index + 2
          
          const studentClass = row[0] || ''
          const studentId = row[1] || ''
          const studentName = row[2] || ''
          
          // 基本验证：学号和姓名至少有一个
          if (!studentId && !studentName) {
            issues.push(`${sheetName}第${rowNumber}行: 缺少学号和姓名`)
            return
          }
          
          // 检查学号重复
          if (studentId) {
            if (studentMap.has(studentId)) {
              issues.push(`${sheetName}第${rowNumber}行: 学号 ${studentId} 重复`)
            } else {
              studentMap.set(studentId, {
                sheet: sheetName,
                row: rowNumber,
                name: studentName
              })
            }
          }
          
          // 检查成绩数据有效性（从第4列开始）
          let validScores = 0
          let invalidScores = []
          
          for (let i = 3; i < row.length; i++) {
            const score = row[i]
            if (score !== '' && score != null) {
              const scoreNum = Number(score)
              if (isNaN(scoreNum)) {
                invalidScores.push(`第${i + 1}列`)
              } else if (scoreNum >= 0) {
                validScores++
              } else {
                invalidScores.push(`第${i + 1}列(负数)`)
              }
            }
          }
          
          if (invalidScores.length > 0) {
            issues.push(`${sheetName}第${rowNumber}行: 无效成绩数据 [${invalidScores.join(', ')}]`)
          }
          
          if (validScores > 0) {
            sheetStudentCount++
            totalStudents++
          } else {
            issues.push(`${sheetName}第${rowNumber}行: 没有有效的成绩数据`)
          }
        })
        
        console.log(`${sheetName} 有效学生数:`, sheetStudentCount)
      }
      
      const isValid = totalStudents > 0
      const message = isValid 
        ? `数据验证通过，共发现 ${totalStudents} 名学生的有效数据`
        : `没有找到有效的学生成绩数据，请检查数据填写`
      
      return {
        isValid,
        message,
        issues,
        studentCount: totalStudents,
        sheetCount: Object.keys(extractedData).length,
        duplicateStudents: Array.from(studentMap.entries()).filter(([id, info]) => 
          issues.some(issue => issue.includes(id))
        )
      }
    },

    // 显示验证结果并确认
    async showValidationResult(validation) {
      if (validation.studentCount === 0) {
        await this.$alert(
          '没有找到有效的学生数据，请先填写成绩信息。\n\n确保：\n• 填写了学号或姓名\n• 至少有一个有效的成绩数据\n• 成绩数据为非负数',
          '数据验证失败',
          {
            confirmButtonText: '返回修改',
            type: 'error'
          }
        )
        return false
      }
      
      if (validation.issues.length > 0) {
        const issueList = validation.issues.slice(0, 8).map(issue => `• ${issue}`).join('\n')
        const moreIssues = validation.issues.length > 8 ? `\n... 还有 ${validation.issues.length - 8} 个问题` : ''
        
        try {
          await this.$confirm(
            `共发现 ${validation.studentCount} 名有效学生，但存在 ${validation.issues.length} 个问题：\n\n${issueList}${moreIssues}\n\n是否继续处理？`,
            '数据验证警告',
            {
              confirmButtonText: '继续处理',
              cancelButtonText: '返回修改',
              type: 'warning',
              customClass: 'validation-dialog',
              closeOnClickModal: false
            }
          )
          return true
        } catch {
          return false
        }
      }
      
      return true
    },

    // 清除表格数据
    async clearSheetData() {
      try {
        await this.$confirm(
          '确定要清除所有学生数据吗？这将重新加载初始模板。',
          '确认清除数据',
          {
            confirmButtonText: '确定清除',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        // 清除保存的数据
        localStorage.removeItem(this.table.backupKey)
        
        // 重新初始化表格
        this.initializeStep7()
        
        this.$message.success('数据清除完成，已重新加载模板')
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('清除数据失败:', error)
          this.$message.error('清除数据失败: ' + error.message)
        }
      }
    },

    // 统一的错误处理
    handleStepError(stepName, error) {
      console.error(`${stepName} 出错:`, error)
      
      const errorConfig = {
        'Network Error': '网络连接失败，请检查网络状态',
        'Timeout': '请求超时，请稍后重试',
        'Luckysheet not ready': '表格组件加载失败，请刷新页面',
        '未找到任何成绩表格': '没有找到数据表格，请确保已正确初始化'
      }
      
      const userMessage = errorConfig[error.message] || 
        (error.message.includes('失败') ? error.message : `${stepName}失败: ${error.message}`)
      
      this.$message.error(userMessage)
      
      // 记录错误信息用于调试
      this.step7.lastError = {
        step: stepName,
        message: error.message,
        timestamp: new Date().toISOString(),
        userMessage: userMessage
      }
    },

    // 修改步骤7的下一步按钮处理
    async handleStep7Next() {
      try {
        this.step7.processing = true
        this.step7.fileGenerationStatus = '正在验证和处理数据...'
        
        // 1. 提取和验证数据
        const extractedData = await this.extractSheetData()
        const validation = await this.validateStudentData(extractedData)
        this.step7.validationResult = validation
        
        // 2. 根据验证结果处理
        if (!validation.isValid || validation.studentCount === 0) {
          const shouldContinue = await this.showValidationResult(validation)
          if (!shouldContinue) {
            return
          }
        }
        
        // 3. 设置数据状态，但不立即处理
        this.step7.hasData = true
        this.step7.fileGenerationStatus = `验证通过，共 ${validation.studentCount} 名学生数据`
        
        this.$message.success(`数据验证通过，共 ${validation.studentCount} 名学生数据`)
        
      } catch (error) {
        this.handleStepError('步骤7数据处理', error)
        this.step7.fileGenerationStatus = '数据处理失败，请检查数据格式'
      } finally {
        this.step7.processing = false
      }
    },

    /**
     * 从Luckysheet提取数据
     */
    async extractLuckysheetData() {
      return new Promise((resolve, reject) => {
        try {
          const iframe = this.$refs.luckysheetFrame
            
          if (!iframe || !iframe.contentWindow) {
            reject(new Error('表格组件未加载'))
            return
          }
          
          const luckysheet = iframe.contentWindow.luckysheet
          if (!luckysheet || !luckysheet.getAllSheets) {
            reject(new Error('Luckysheet实例不可用'))
            return
          }
          
          const allSheets = luckysheet.getAllSheets()
          if (!allSheets || allSheets.length === 0) {
            reject(new Error('未找到任何表格数据'))
            return
          }
          
          const sheets = []
          console.log('开始提取Luckysheet数据，共', allSheets.length, '个sheet')

          allSheets.forEach(sheet => {
            try {
              const celldata = sheet.celldata || []
              const data = []

              console.log('处理Sheet:', sheet.name, '单元格数量:', celldata.length)

              // 确定数据范围
              let maxRow = 0
              let maxCol = 0
              celldata.forEach(cell => {
                maxRow = Math.max(maxRow, cell.r)
                maxCol = Math.max(maxCol, cell.c)
              })

              console.log(`Sheet ${sheet.name} 数据范围: ${maxRow + 1}行 × ${maxCol + 1}列`)

              // 构建数据矩阵
              for (let r = 0; r <= maxRow; r++) {
                const row = []
                for (let c = 0; c <= maxCol; c++) {
                  const cell = celldata.find(item => item.r === r && item.c === c)
                  
                  // 安全地提取单元格值
                  let cellValue = ''
                  if (cell && cell.v) {
                    // 如果是对象格式的值
                    if (typeof cell.v === 'object' && cell.v.v !== undefined) {
                      cellValue = cell.v.v
                    } else {
                      // 如果是直接的值
                      cellValue = cell.v
                    }
                  }
                  
                  // 确保值不是null或undefined
                  if (cellValue === null || cellValue === undefined) {
                    cellValue = ''
                  }
                  
                  row.push(cellValue)
                }
                data.push(row)
              }

              // 验证数据有效性（至少要有表头行）
              if (data.length > 0 && data[0].length > 0) {
                sheets.push({
                  name: sheet.name || `Sheet${sheets.length + 1}`,
                  data: data
                })
                console.log(`Sheet ${sheet.name} 数据提取成功: ${data.length}行 × ${data[0]?.length || 0}列`)
              } else {
                console.warn(`Sheet ${sheet.name} 数据为空或无效`)
              }
              
            } catch (error) {
              console.error(`提取sheet ${sheet.name} 失败:`, error)
            }
          })

          if (sheets.length === 0) {
            reject(new Error('所有表格数据提取失败或为空'))
            return
          }

          console.log('最终提取的数据: ', { sheets })
          
          // 保存到状态中
          this.dataProcessing.extractedData = { sheets }
          
          // 同时返回数据用于验证
          resolve({ sheets })
          
        } catch (error) {
          console.error('提取Luckysheet数据失败:', error)
          reject(new Error('提取表格数据失败: ' + error.message))
        }
      })
    },

    // 添加手动检查数据的方法
    manualCheckData() {
      this.checkIfDataExists().then(hasData => {
        if (hasData) {
          this.$message.success('检测到有效数据，可以开始处理')
        } else {
          this.$message.warning('未检测到有效数据，请填写成绩信息')
        }
      })
    },

    // 重试数据处理
    async retryProcessData() {
      // 重置状态
      this.resetProcessingState()
      
      // 重新处理
      await this.processData()
    },

    /**
     * 获取处理按钮文本
     */
    getProcessButtonText() {
      const status = this.dataProcessing.status
      
      const texts = {
        'idle': '开始处理数据',
        'saving': '正在保存...',
        'validating': '正在检查数据...', // 新增的检查状态
        'extracting': '正在提取...',
        'submitting': '正在提交...',
        'processing': '正在处理...',
        'success': '处理完成',
        'error': '处理失败'
      }
      
      return texts[status] || '开始处理数据'
    },

    /**
     * 统一的数据处理入口（直接发送配置+数据，无本地文件依赖）
     */
    async processData() {
      try {
        console.log('开始处理数据...')
        
        this.dataProcessing.status = 'validating'
        this.dataProcessing.message = '正在保存当前数据...'
        this.dataProcessing.progress = 10
        this.dataProcessing.details = '保存表格数据并缓存实例'
        
        // 1. 保存当前数据（内部会将 luckysheet 实例缓存到 this.cachedLuckysheet）
        const saved = await this.saveTableData()
        if (!saved) {
          console.warn('未检测到表格数据，将继续处理')
        }
        this.dataProcessing.progress = 20
        
        // 2. 提取表格数据（直接使用缓存实例，无需等待 iframe 就绪）
        this.dataProcessing.status = 'extracting'
        this.dataProcessing.message = '正在提取表格数据...'
        this.dataProcessing.progress = 30
        this.dataProcessing.details = '读取所有sheet中的学生成绩数据'
        
        await this.extractTableData()
        
        // 3. 验证提取的数据
        this.dataProcessing.status = 'validating'
        this.dataProcessing.message = '正在验证数据格式...'
        this.dataProcessing.progress = 50
        this.dataProcessing.details = '检查学号、姓名、成绩格式是否正确'
        
        const validationResult = await this.validateExtractedData()
        
        // 4. 获取学生数量
        const studentCount = this.getStudentCount()
        this.table.studentCount = studentCount
        this.table.isReady = true   // 标记表格就绪状态
        
        // 5. 数据验证结果处理
        if (studentCount === 0) {
          this.dataProcessing.status = 'error'
          this.dataProcessing.message = '未找到有效的学生数据'
          this.dataProcessing.progress = 0
          this.dataProcessing.details = '请填写至少一名学生的完整信息'
          
          await this.$alert(
            '请在表格中填写至少一名学生的完整信息：\n\n' +
            '1. 班级、学号、姓名（至少填写学号或姓名）\n' +
            '2. 至少一项有效的成绩数据（正数）\n' +
            '3. 确保数据从第二行开始填写\n\n' +
            '当前问题：未检测到有效的学生成绩数据',
            '数据验证失败',
            {
              confirmButtonText: '返回填写',
              type: 'error'
            }
          )
          
          this.resetProcessingState()
          return
        }
        
        // 6. 显示验证结果，询问是否继续
        if (validationResult && validationResult.issues && validationResult.issues.length > 0) {
          const issueCount = validationResult.issues.length
          const issueList = validationResult.issues.slice(0, 5).map(issue => `• ${issue}`).join('\n')
          const moreIssues = issueCount > 5 ? `\n... 还有 ${issueCount - 5} 个问题` : ''
          
          this.dataProcessing.message = `发现 ${issueCount} 个数据问题`
          this.dataProcessing.progress = 60
          this.dataProcessing.details = `请确认是否继续处理`
          
          try {
            await this.$confirm(
              `共发现 ${studentCount} 名有效学生，但存在 ${issueCount} 个问题：\n\n${issueList}${moreIssues}\n\n是否继续处理？`,
              '数据验证警告',
              {
                confirmButtonText: '继续处理',
                cancelButtonText: '返回修改',
                type: 'warning'
              }
            )
          } catch {
            this.resetProcessingState()
            this.$message.info('已取消数据处理，请修改数据后重试')
            return
          }
        }
        
        // 7. 再次保存数据（确保最新数据被保存）
        this.dataProcessing.status = 'saving'
        this.dataProcessing.message = '正在保存数据备份...'
        this.dataProcessing.progress = 70
        this.dataProcessing.details = '备份数据到本地存储'
        
        await this.saveTableData()
        
        // 8. 准备请求数据：生成后端格式配置，并获取表格数据
        this.dataProcessing.status = 'preparing'
        this.dataProcessing.message = '正在准备请求数据...'
        this.dataProcessing.progress = 75
        
        // 生成前端配置并转换为后端期望的格式
        const frontendConfig = this.generateCourseConfig()
        const backendConfig = this.convertToBackendConfig(frontendConfig) // 需实现转换函数
        const excelData = this.dataProcessing.extractedData
        
        const requestData = {
          config: backendConfig,
          sheets: excelData.sheets  // 直接发送提取的表格数据
        }
        
        console.log('发送的请求数据:', requestData)
        
        // 9. 调用新的直接处理接口
        this.dataProcessing.status = 'submitting'
        this.dataProcessing.message = '正在提交数据处理...'
        this.dataProcessing.progress = 80
        this.dataProcessing.details = `正在分析 ${studentCount} 名学生的成绩数据`
        
        try {
          // 导入并使用新的 API 方法（需在文件顶部导入 processDataDirect）
          const response = await processDataDirect(requestData)
          
          console.log('数据处理成功:', response)
          
          if (response.code !== 200) {
            throw new Error(response.msg || '数据处理失败')
          }
          
          // 保存后端返回的结果数据
          this.dataProcessing.results = response.results || {}
          this.dataProcessing.reportId = response.reportId || ''
          // 注意：后端返回的 dataDir 可能等于 reportId，用于图片访问
          if (response.dataDir) {
            this.dataProcessing.reportId = response.dataDir
            this.dataProcessing.configId = response.dataDir   // 添加这一行
          }
          
          this.dataProcessing.status = 'processing'
          this.dataProcessing.message = '正在生成分析报告...'
          this.dataProcessing.progress = 90
          this.dataProcessing.details = '计算课程目标达成度、生成统计图表'
          
          // 10. 跳转到结果页面
          await this.jumpToResultsPage()
          
        } catch (error) {
          console.error('数据处理失败:', error)
          
          this.dataProcessing.status = 'error'
          this.dataProcessing.message = `处理失败: ${error.message}`
          this.dataProcessing.progress = 0
          this.dataProcessing.error = error.message
          
          this.logProcessingError(error)
          
          await this.$confirm(
            `数据处理失败：${error.message}\n\n是否重试？`,
            '处理失败',
            {
              confirmButtonText: '重试',
              cancelButtonText: '返回修改',
              type: 'error'
            }
          ).then(() => {
            this.resetProcessingState()
            this.processData()
          }).catch(() => {
            this.returnToDataEntryWithRecovery()
          })
        }
        
      } catch (error) {
        console.error('数据处理过程中发生未预期的错误:', error)
        
        this.dataProcessing.status = 'error'
        this.dataProcessing.message = `系统错误: ${error.message}`
        this.dataProcessing.progress = 0
        
        this.$message.error(`数据处理失败: ${error.message}`)
        this.logProcessingError(error)
        
        await this.returnToDataEntryWithRecovery()
      }
    },

    /**
     * 重置处理状态
     */
    resetProcessingState() {
      this.dataProcessing.status = 'idle'
      this.dataProcessing.progress = 0
      this.dataProcessing.message = ''
      this.dataProcessing.details = ''
      this.dataProcessing.error = null
      // 不清除extractedData，保留已提取的数据用于显示
    },

    /**
     * 返回数据录入页面并恢复数据
     */
    async returnToDataEntryWithRecovery() {
      this.currentStep = 6;
      await this.$nextTick();
      
      // 尝试将备份数据填充回当前表格（不重新创建实例）
      try {
        const restored = await this.restoreTableData();
        if (restored) {
          this.$message.warning('数据已恢复，请检查后重试');
        } else {
          this.$message.warning('数据处理失败，请重新填写数据');
        }
      } catch (e) {
        console.warn('恢复数据失败', e);
      }
      
      this.resetProcessingState();
    },

    /**
     * 自动数据检查
     */
    async autoCheckData() {
      try {
        console.log('开始自动数据检查...')
        
        // 1. 检查表格状态 - 添加更多验证
        if (!this.table.isReady) {
          console.log('表格状态: 未就绪，检查iframe是否存在...')
          
          // 检查iframe是否存在
          if (!this.$refs.luckysheetFrame) {
            this.$message.error('表格组件未加载，请刷新页面')
            return false
          }
          
          // 检查iframe是否已加载
          const iframe = this.$refs.luckysheetFrame
          if (!iframe.contentWindow) {
            this.$message.warning('表格正在加载中，请稍候...')
            
            // 等待iframe加载
            await new Promise(resolve => setTimeout(resolve, 1000))
            
            // 再次检查
            if (!iframe.contentWindow) {
              this.$message.error('表格加载超时，请刷新页面')
              return false
            }
          }
          
          // 更新表格状态
          this.table.isReady = true
        }
        
        this.dataProcessing.status = 'validating'
        this.dataProcessing.message = '正在检查表格状态...'
        this.dataProcessing.progress = 10
        this.dataProcessing.details = '检查表格组件是否就绪'
        
        // 2. 获取表格实例（添加更详细的错误处理）
        let luckysheet
        try {
          console.log('尝试获取表格实例...')
          luckysheet = await this.waitForTableReady()
          console.log('表格实例获取成功:', !!luckysheet)
        } catch (error) {
          console.error('获取表格实例失败:', error)
          
          // 尝试备选方案：直接获取
          try {
            console.log('尝试直接获取表格实例...')
            const iframe = this.$refs.luckysheetFrame
            if (iframe && iframe.contentWindow) {
              luckysheet = iframe.contentWindow.luckysheet
              console.log('直接获取结果:', !!luckysheet)
            }
          } catch (directError) {
            console.error('直接获取也失败:', directError)
          }
          
          if (!luckysheet) {
            this.$message.error('表格组件未加载完成，请刷新页面重试')
            return false
          }
        }
        
        if (!luckysheet) {
          this.$message.error('无法获取表格实例')
          return false
        }
        
        this.dataProcessing.progress = 20
        this.dataProcessing.message = '正在检查表格数据...'
        
        // 3. 检查表格是否有数据
        try {
          const allSheets = luckysheet.getAllSheets ? luckysheet.getAllSheets() : []
          console.log('表格中的sheet数量:', allSheets.length)
          
          if (allSheets.length === 0) {
            this.$message.warning('表格中没有数据，请填写学生成绩信息')
            
            this.dataProcessing.message = '表格中没有数据'
            this.dataProcessing.progress = 30
            this.dataProcessing.details = '请填写至少一行学生数据'
            
            // 延迟显示以让用户看到进度
            await new Promise(resolve => setTimeout(resolve, 1500))
            return false
          }
        } catch (sheetError) {
          console.error('检查表格数据失败:', sheetError)
          this.$message.warning('表格数据检查失败，请确保已正确填写数据')
          return false
        }
        
        this.dataProcessing.progress = 30
        this.dataProcessing.message = '正在提取表格数据...'
        
        // 4. 提取数据
        try {
          await this.extractTableData()
        } catch (extractError) {
          console.error('提取数据失败:', extractError)
          this.$message.error(`数据提取失败: ${extractError.message}`)
          return false
        }
        
        this.dataProcessing.progress = 60
        this.dataProcessing.message = '正在验证数据格式...'
        
        // 5. 验证数据
        try {
          await this.validateExtractedData()
        } catch (validateError) {
          console.error('验证数据失败:', validateError)
          this.$message.error(`数据验证失败: ${validateError.message}`)
          return false
        }
        
        // 6. 获取学生数量
        const studentCount = this.getStudentCount()
        
        if (studentCount === 0) {
          this.$message.warning('未找到有效的学生数据，请填写成绩信息')
          
          // 显示详细的指导信息
          await this.$alert(
            '请在表格中填写至少一名学生的完整信息：\n\n' +
            '1. 班级、学号、姓名（至少填写一项）\n' +
            '2. 至少一项有效的成绩数据（正数）\n' +
            '3. 确保数据从第二行开始填写\n\n' +
            '当前问题：表格已加载但未检测到有效的学生数据',
            '数据填写指南',
            {
              confirmButtonText: '确定',
              type: 'warning'
            }
          )
          
          this.resetProcessingState()
          return false
        }
        
        // 7. 检查通过，显示提示信息
        this.dataProcessing.message = `数据检查通过，共发现 ${studentCount} 名学生数据`
        this.dataProcessing.progress = 80
        this.dataProcessing.details = '数据格式正确，准备开始处理'
        
        console.log('自动数据检查通过，学生数量:', studentCount)
        
        // 短暂显示成功消息
        await new Promise(resolve => setTimeout(resolve, 1000))
        
        return true
        
      } catch (error) {
        console.error('自动数据检查过程中发生未预期的错误:', error)
        
        // 更友好的错误提示
        let userMessage = '数据处理检查失败'
        
        if (error.message.includes('表格') || error.message.includes('Luckysheet')) {
          userMessage = '表格组件加载异常，请刷新页面后重试'
        } else if (error.message.includes('网络') || error.message.includes('连接')) {
          userMessage = '网络连接异常，请检查网络后重试'
        } else if (error.message.includes('超时')) {
          userMessage = '操作超时，请稍后重试'
        }
        
        this.$message.error(`${userMessage}: ${error.message}`)
        
        // 重置处理状态
        this.resetProcessingState()
        
        return false
      }
    },

    /**
     * 启动处理流程
     */
    async startDataProcessing() {
      // 设置处理状态
      this.dataProcessing.status = 'saving'
      this.dataProcessing.progress = 5
      this.dataProcessing.message = '正在保存当前数据...'
      this.dataProcessing.details = '保存表格数据到本地存储'
      
      // 保存当前表格数据
      const saved = await this.saveTableData()
      
      if (!saved) {
        this.dataProcessing.details = '表格数据为空，将继续处理'
      }
      
      // 生成配置ID
      this.dataProcessing.configId = this.generateConfigId()
      
      console.log('数据处理启动，配置ID:', this.dataProcessing.configId)
      console.log('数据检查已通过，学生数量:', this.getStudentCount())
    },

    /**
     * 执行处理步骤序列
     */
    async executeProcessingSteps() {
      const steps = [
        {
          name: 'extracting',
          action: () => this.submitForProcessing(), // 直接提交处理
          progress: 30,
          message: '正在提交数据处理...',
          details: '将数据发送到后端进行处理'
        },
        {
          name: 'processing',
          action: () => this.awaitProcessingResult(),
          progress: 70,
          message: '正在处理数据，请稍候...',
          details: '分析学生成绩并生成统计结果'
        }
      ]
      
      // 顺序执行所有步骤
      for (const step of steps) {
        try {
          // 更新状态
          this.dataProcessing.status = step.name
          this.dataProcessing.progress = step.progress
          this.dataProcessing.message = step.message
          this.dataProcessing.details = step.details
          
          console.log(`执行步骤: ${step.name}`)
          await step.action()
          
          // 短暂延迟，让用户看到进度变化
          await new Promise(resolve => setTimeout(resolve, 500))
          
        } catch (error) {
          console.error(`步骤 ${step.name} 失败:`, error)
          throw new Error(`数据处理失败: ${step.name} - ${error.message}`)
        }
      }
      
      return true
    },

    /**
     * 跳转到结果页面
     */
    async jumpToResultsPage() {
      // 处理成功
      this.dataProcessing.status = 'success'
      this.dataProcessing.progress = 100
      this.dataProcessing.message = '数据处理完成!'
      this.dataProcessing.details = '数据分析完成，准备展示结果'
      
      // 清除保存的数据
      localStorage.removeItem(this.table.backupKey)
      
      // 延迟显示成功消息
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      // 跳转到结果页面
      this.currentStep = 7
      
      // 初始化结果页面
      this.$nextTick(() => {
        this.initializeResultsPage()
      })
      
      this.$message.success('数据处理完成，已跳转到结果页面')
    },

    /**
     * 初始化结果页面
     */
    initializeResultsPage() {
      console.log('初始化结果页面')
      
      // 这里可以添加加载图表数据等操作
      // 比如：this.loadCharts()
      
      // 设置默认激活的标签页
      this.activeTab = 'gradeDistribution'
    },

    /**
     * 加载默认模板
     */
    async loadDefaultTemplate(luckysheet) {
      return new Promise((resolve) => {
        try {
          const defaultSheets = this.generateLuckysheetData()
          
          if (typeof luckysheet.destroy === 'function') {
            try {
              luckysheet.destroy()
            } catch (e) {
              console.log('清理现有实例:', e.message)
            }
          }
          
          setTimeout(() => {
            try {
              luckysheet.create({
                container: 'luckysheet',
                lang: 'zh',
                data: defaultSheets,
                title: '学生成绩数据表',
                userInfo: false,
                showtoolbar: true,
                showinfobar: false,
                showsheetbar: true,
                showstatisticBar: true
              })
              
              console.log('默认模板加载成功')
              resolve()
              
            } catch (error) {
              console.error('加载模板失败:', error)
              resolve()
            }
          }, 300)
          
        } catch (error) {
          console.error('准备模板失败:', error)
          resolve()
        }
      })
    },

    /**
     * 统一的错误处理
     */
    async handleProcessingError(error) {
      console.error('数据处理错误:', error)
      
      // 更新状态
      this.dataProcessing.status = 'error'
      this.dataProcessing.error = error.message
      this.dataProcessing.message = `处理失败: ${error.message}`
      this.dataProcessing.details = '请检查数据格式后重试'
      
      // 保存错误日志
      this.logProcessingError(error)
      
      // 显示错误信息
      this.$message.error(`数据处理失败: ${error.message}`)
      
      // 重置进度条
      this.dataProcessing.progress = 0
      
      // 延迟后允许用户重试
      setTimeout(() => {
        this.dataProcessing.details = '点击"开始处理数据"按钮重试'
      }, 3000)
    },

    /**
     * 获取学生数量
     */
    getStudentCount() {
      if (!this.dataProcessing.extractedData || 
          !this.dataProcessing.extractedData.sheets) {
        return 0
      }
      
      let count = 0
      const sheets = this.dataProcessing.extractedData.sheets
      
      sheets.forEach(sheet => {
        if (sheet.data && sheet.data.length > 1) {
          // 从第二行开始（第一行是表头）
          for (let i = 1; i < sheet.data.length; i++) {
            const row = sheet.data[i]
            // 检查是否有学号或姓名
            if (row && row.length >= 3 && (row[1] || row[2])) {
              count++
            }
          }
        }
      })
      
      // 更新表格状态
      this.table.studentCount = count
      this.table.lastValidCheck = new Date()
      
      return count
    },

    /**
     * 获取最后检查时间
     */
    getLastCheckTime() {
      if (!this.table.lastValidCheck) {
        return '未检查'
      }
      
      const now = new Date()
      const checkTime = new Date(this.table.lastValidCheck)
      const diffMinutes = Math.floor((now - checkTime) / (1000 * 60))
      
      if (diffMinutes < 1) {
        return '刚刚'
      } else if (diffMinutes < 60) {
        return `${diffMinutes}分钟前`
      } else {
        return `${Math.floor(diffMinutes / 60)}小时前`
      }
    },

    /**
     * 提取表格数据
     */
    async extractTableData() {
      return new Promise(async (resolve, reject) => {
        // 优先使用缓存的实例
        let luckysheet = this.cachedLuckysheet
        
        // 如果缓存不存在，尝试从 iframe 获取
        if (!luckysheet) {
          const iframe = this.$refs.luckysheetFrame
          if (iframe && iframe.contentWindow) {
            luckysheet = iframe.contentWindow.luckysheet
            if (luckysheet) {
              this.cachedLuckysheet = luckysheet  // 补缓存
            }
          }
        }
        
        if (!luckysheet) {
          reject(new Error('无法获取 Luckysheet 实例'))
          return
        }
        
        try {
          // 方案二：使用 getLuckysheetfile() 直接读取原始数据（最可靠）
          console.log('\n========== 开始提取表格数据 ==========')
          
          if (typeof luckysheet.getLuckysheetfile !== 'function') {
            reject(new Error('Luckysheet API未就绪：getLuckysheetfile方法不存在'))
            return
          }
          
          const luckysheetfile = luckysheet.getLuckysheetfile()
          
          if (!luckysheetfile || !Array.isArray(luckysheetfile) || luckysheetfile.length === 0) {
            reject(new Error('无法获取表格原始数据'))
            return
          }
          
          console.log('获取到', luckysheetfile.length, '个Sheet')
          
          // 遍历所有sheet，直接使用data字段（已经是二维数组格式）
          const sheets = []
          luckysheetfile.forEach((sheet, idx) => {
            console.log(`\nSheet ${idx + 1}:`, sheet.name)
            console.log('  Sheet ID:', sheet.id)
            console.log('  是否有data字段:', !!sheet.data)
            
            if (sheet.data && Array.isArray(sheet.data)) {
              // 规范化单元格数据：将对象转换为纯值
              const normalizeCell = (cell) => {
                if (cell == null) return ''
                if (typeof cell === 'object' && cell.v !== undefined) return cell.v
                if (typeof cell === 'object') return ''
                return cell
              }
              
              const normalizedData = sheet.data.map(row => {
                if (!Array.isArray(row)) return []
                return row.map(normalizeCell)
              })
              
              console.log('  表头（第1行）:', normalizedData[0] ? normalizedData[0].slice(0, 10) : '无')
              if (normalizedData.length > 1) {
                console.log('  第2行:', normalizedData[1].slice(0, 10))
              }
              if (normalizedData.length > 2) {
                console.log('  第3行:', normalizedData[2].slice(0, 10))
              }
              if (normalizedData.length > 3) {
                console.log('  第4行:', normalizedData[3].slice(0, 10))
              }
              console.log('  总行数:', normalizedData.length)
              
              sheets.push({
                name: sheet.name || `Sheet${idx + 1}`,
                data: normalizedData  // 使用规范化后的数据
              })
            } else {
              console.warn('  ⚠️ Sheet没有data字段或data不是数组')
            }
          })
          
          console.log('\n========================================\n')
          
          if (sheets.length === 0) {
            reject(new Error('没有找到有效的表格数据'))
            return
          }
          
          this.dataProcessing.extractedData = { sheets }
          console.log('数据提取成功，共', sheets.length, '个sheet')
          resolve()
          
        } catch (error) {
          reject(new Error('提取数据失败: ' + error.message))
        }
      })
    },

    /**
     * 验证提取的数据
     */
    async validateExtractedData() {
      if (!this.dataProcessing.extractedData) {
        throw new Error('未找到提取的数据')
      }
      
      const { sheets } = this.dataProcessing.extractedData
      
      // 基础验证
      if (!sheets || sheets.length === 0) {
        throw new Error('没有有效的表格数据')
      }
      
      let hasStudentData = false
      // 使用Set来去重统计学生数量（基于学号）
      const studentSet = new Set()
      
      // 检查每个sheet
      for (const sheet of sheets) {
        if (sheet.data && sheet.data.length > 1) {
          // 检查表头
          if (sheet.data[0].length < 3) {
            throw new Error(`${sheet.name}: 表头不完整`)
          }
          
          // 检查数据行
          for (let i = 1; i < sheet.data.length; i++) {
            const row = sheet.data[i]
            
            // 跳过空行
            if (!row || row.length < 3) continue
            
            // 检查基本信息
            const studentId = row[1]  // 学号
            const studentName = row[2]  // 姓名
            
            // 如果学号和姓名都为空，跳过
            if (!studentId && !studentName) continue
            
            // 检查成绩数据
            const hasScores = row.length > 3 && 
                            row.slice(3).some(score => 
                              score && !isNaN(Number(score)) && Number(score) > 0
                            )
            
            if (hasScores) {
              hasStudentData = true
              // 使用学号作为唯一标识，如果学号为空则使用姓名
              const uniqueKey = studentId ? String(studentId).trim() : String(studentName).trim()
              if (uniqueKey) {
                studentSet.add(uniqueKey)
              }
            }
          }
        }
      }
      
      if (!hasStudentData) {
        throw new Error('没有找到有效的学生成绩数据')
      }
      
      const totalStudents = studentSet.size
      console.log('数据验证通过，共发现', totalStudents, '名不重复的学生')
      this.table.isDataValid = true
      this.table.lastValidCheck = new Date()
    },

    /**
     * 提交数据处理
     */
    async submitForProcessing() {
      return new Promise((resolve, reject) => {
        try {
          // 1. 验证必要数据
          if (!this.dataProcessing.extractedData) {
            reject(new Error('没有可处理的数据'))
            return
          }

          if (!this.dataProcessing.configId) {
            reject(new Error('缺少配置ID'))
            return
          }

          console.log('开始提交数据处理...')
          console.log('配置ID:', this.dataProcessing.configId)
          console.log('提取的数据结构:', this.dataProcessing.extractedData)

          // 2. 调用后端API处理数据
          // 直接使用原代码中的processData API调用
          processData(this.dataProcessing.extractedData, this.dataProcessing.configId)
            .then(response => {
              console.log('processData API完整响应:', response)
              console.log('response.code:', response.code)
              console.log('response.results:', response.results)
              console.log('response.reportId:', response.reportId)

              // 检查响应
              if (!response) {
                reject(new Error('API响应为空'))
                return
              }

              if (typeof response !== 'object') {
                reject(new Error('API响应格式错误，期望对象'))
                return
              }

              if (response.code !== 200) {
                reject(new Error('API返回错误: ' + (response.msg || '未知错误')))
                return
              }

              // 保存处理结果
              this.dataProcessing.results = response.results || {}
              this.dataProcessing.reportId = response.reportId || ''

              console.log('数据处理提交成功，结果:', this.dataProcessing.results)
              console.log('报告ID:', this.dataProcessing.reportId)

              // 确保configId也正确设置（从原代码中可见需要保存configId用于后续图片加载）
              if (response.configId) {
                this.dataProcessing.configId = response.configId
              }

              resolve()
            })
            .catch(error => {
              console.error('processData API调用失败:', error)
              
              // 详细的错误处理
              let errorMessage = '数据处理API调用失败：'
              if (error.response) {
                // 服务器返回了错误响应
                console.error('HTTP错误响应:', error.response)
                console.error('HTTP状态码:', error.response.status)
                console.error('HTTP响应数据:', error.response.data)
                
                errorMessage += `服务器错误 ${error.response.status}: ${JSON.stringify(error.response.data) || error.response.statusText}`
              } else if (error.request) {
                // 网络错误
                console.error('网络请求错误:', error.request)
                errorMessage += '网络连接失败，请检查前端服务器是否启动(端口80)和后端服务是否启动(端口8080)'
              } else if (error.message) {
                // 其他错误
                errorMessage += error.message
              } else {
                errorMessage += '未知错误，请查看控制台日志'
              }
              
              reject(new Error(errorMessage))
            })
            
        } catch (error) {
          console.error('提交数据处理时发生异常:', error)
          reject(new Error('提交数据处理失败: ' + error.message))
        }
      })
    },

    // 添加新的表格状态检测方法
    async waitForTableFullyReady() {
      return new Promise((resolve, reject) => {
        const maxAttempts = 50
        const interval = 200
        let attempts = 0
        
        const check = async () => {
          attempts++
          
          try {
            // 1. 检查iframe引用是否存在（这是关键修改）
            const iframe = this.$refs.luckysheetFrame
            if (!iframe) {
              console.log(`[${attempts}] iframe引用不存在`)
              if (attempts >= maxAttempts) {
                reject(new Error('iframe引用未找到'))
                return
              }
              setTimeout(check, interval)
              return
            }
            
            // 2. 检查iframe窗口
            if (!iframe.contentWindow) {
              console.log(`[${attempts}] iframe窗口未加载`)
              if (attempts >= maxAttempts) {
                reject(new Error('iframe内容加载超时'))
                return
              }
              setTimeout(check, interval)
              return
            }
            
            // 3. 检查Luckysheet对象
            const luckysheet = iframe.contentWindow.luckysheet
            if (!luckysheet) {
              console.log(`[${attempts}] luckysheet对象不存在`)
              if (attempts >= maxAttempts) {
                reject(new Error('Luckysheet插件加载超时'))
                return
              }
              setTimeout(check, interval)
              return
            }
            
            // 4. 检查关键API
            if (typeof luckysheet.getAllSheets !== 'function') {
              console.log(`[${attempts}] getAllSheets方法不存在`)
              if (attempts >= maxAttempts) {
                reject(new Error('Luckysheet API未就绪'))
                return
              }
              setTimeout(check, interval)
              return
            }
            
            // 5. 测试API
            try {
              const sheets = luckysheet.getAllSheets()
              if (!sheets) {
                throw new Error('getAllSheets返回空')
              }
              console.log(`表格完全就绪! sheet数量: ${sheets.length}`)
              resolve(luckysheet)
            } catch (apiError) {
              console.log(`[${attempts}] API测试失败: ${apiError.message}`)
              if (attempts >= maxAttempts) {
                reject(new Error('Luckysheet API测试失败'))
                return
              }
              setTimeout(check, interval)
            }
            
          } catch (error) {
            console.error(`检查表格状态时出错: ${error.message}`)
            if (attempts >= maxAttempts) {
              reject(error)
            } else {
              setTimeout(check, interval)
            }
          }
        }
        
        console.log('开始等待表格完全就绪...')
        check()
      })
    },

    /**
     * 完成数据处理
     */
    async awaitProcessingResult() {
      return new Promise((resolve) => {
        
        const startTime = Date.now()
        const totalWaitTime = 5000 // 假设处理需要5秒
        
        // 模拟进度更新
        const interval = setInterval(() => {
          const elapsed = Date.now() - startTime
          const progress = Math.min(80 + Math.floor((elapsed / totalWaitTime) * 20), 95)
          
          this.dataProcessing.progress = progress
          this.dataProcessing.details = `处理进度: ${progress}%`
          
          // 更新处理消息
          if (progress < 85) {
            this.dataProcessing.message = '正在分析成绩数据...'
          } else if (progress < 90) {
            this.dataProcessing.message = '正在计算课程目标达成度...'
          } else {
            this.dataProcessing.message = '正在生成图表和报告...'
          }
          
        }, 500)
        
        // 模拟处理时间
        setTimeout(() => {
          clearInterval(interval)
          this.dataProcessing.progress = 100
          this.dataProcessing.message = '数据处理完成!'
          this.dataProcessing.details = '数据分析完成，准备展示结果'
          resolve()
        }, totalWaitTime)
      })
    },

    /**
     * 统一的错误处理
     */
    async handleProcessingError(error) {
      console.error('数据处理错误:', error)
      
      // 更新状态
      this.dataProcessing.status = 'error'
      this.dataProcessing.error = error.message
      this.dataProcessing.message = `处理失败: ${error.message}`
      
      // 保存错误日志
      this.logProcessingError(error)
      
      // 显示错误信息
      this.$message.error(`数据处理失败: ${error.message}`)
      
      // 延迟显示错误，让用户看清楚
      await new Promise(resolve => setTimeout(resolve, 2000))
      
      // 返回数据录入页面并恢复数据
      await this.returnToDataEntryWithRecovery()
    },


    /**
     * 重置处理状态
     */
    resetProcessingState() {
      this.dataProcessing.status = 'idle'
      this.dataProcessing.progress = 0
      this.dataProcessing.error = null
      this.dataProcessing.extractedData = null
    },

    /**
     * 保存表格数据
     */
    async saveTableData() {
      return new Promise((resolve) => {
        const iframe = this.$refs.luckysheetFrame
        
        if (!iframe || !iframe.contentWindow) {
          console.warn('保存失败: iframe未就绪')
          resolve(false)
          return
        }
        
        const luckysheet = iframe.contentWindow.luckysheet
        if (!luckysheet) {
          console.warn('保存失败: luckysheet实例未就绪')
          resolve(false)
          return
        }
        
        // 缓存实例供后续步骤使用
        this.cachedLuckysheet = luckysheet
        
        try {
          const allSheets = luckysheet.getAllSheets() || []
          
          if (allSheets.length === 0) {
            console.log('没有数据需要保存')
            resolve(false)
            return
          }
          
          const sheetsToSave = []
          
          allSheets.forEach((sheet, index) => {
            try {
              const sheetData = luckysheet.getSheetData(sheet.id)
              
              const celldata = []
              if (sheetData && Array.isArray(sheetData)) {
                for (let r = 0; r < sheetData.length; r++) {
                  const row = sheetData[r]
                  if (row && Array.isArray(row)) {
                    for (let c = 0; c < row.length; c++) {
                      const cell = row[c]
                      if (cell != null) {
                        if (typeof cell === 'object') {
                          celldata.push({
                            r: r,
                            c: c,
                            v: cell
                          })
                        } else {
                          celldata.push({
                            r: r,
                            c: c,
                            v: { v: cell }
                          })
                        }
                      }
                    }
                  }
                }
              }
              
              const hasActualData = celldata.some(cell => 
                cell.r > 0 &&
                (cell.v && (
                  (typeof cell.v === 'object' && cell.v.v) || 
                  (typeof cell.v !== 'object' && cell.v)
                ))
              )
              
              if (hasActualData) {
                sheetsToSave.push({
                  name: sheet.name || `Sheet${index + 1}`,
                  index: index,
                  order: index,
                  celldata: celldata,
                  config: {
                    columnlen: sheet.config?.columnlen || {},
                    rowlen: sheet.config?.rowlen || 40,
                    merge: sheet.config?.merge || {},
                    borderInfo: sheet.config?.borderInfo || [],
                    authority: null,
                    visibledatarow: sheet.config?.visibledatarow || [],
                    visibledatacolumn: sheet.config?.visibledatacolumn || [],
                    rowhidden: sheet.config?.rowhidden || {},
                    colhidden: sheet.config?.colhidden || {},
                    chart: sheet.config?.chart || [],
                    filter_select: null,
                    filter: null,
                    luckysheet_select_save: [],
                    calcChain: [],
                    isPivotTable: false,
                    pivotTable: null,
                    dataVerification: sheet.config?.dataVerification || {},
                    frozen: sheet.config?.frozen || null,
                    images: [],
                    hyperlinks: null,
                    conditionformat: null
                  }
                })
                console.log(`Sheet ${sheet.name} 有数据，已加入保存队列`)
              } else {
                console.log(`Sheet ${sheet.name} 无有效数据，跳过保存`)
              }
            } catch (error) {
              console.error(`保存sheet ${sheet.name} 失败:`, error)
            }
          })
          
          if (sheetsToSave.length > 0) {
            const savedData = {
              timestamp: Date.now(),
              sheets: sheetsToSave,
              version: '2.0'
            }
            
            localStorage.setItem(this.table.backupKey, JSON.stringify(savedData))
            this.dataProcessing.hasSavedData = true
            this.dataProcessing.lastSaveTime = savedData.timestamp
            
            console.log('数据保存成功，共', sheetsToSave.length, '个sheet')
            resolve(true)
          } else {
            console.log('没有需要保存的数据')
            localStorage.removeItem(this.table.backupKey)
            resolve(false)
          }
          
        } catch (error) {
          console.error('保存数据失败:', error)
          resolve(false)
        }
      })
    },

    /**
     * 恢复表格数据
     */
    async restoreTableData() {
      console.log('开始恢复表格数据...')
      
      return new Promise((resolve) => {
        // 检查是否有保存的数据
        const savedData = localStorage.getItem(this.table.backupKey)
        if (!savedData) {
          console.log('没有找到保存的数据')
          resolve(false)
          return
        }
        
        // 解析数据
        let parsedData
        try {
          parsedData = JSON.parse(savedData)
        } catch (error) {
          console.error('解析保存数据失败:', error)
          localStorage.removeItem(this.table.backupKey)
          resolve(false)
          return
        }
        
        // 检查数据格式
        if (!parsedData.sheets || !Array.isArray(parsedData.sheets) || parsedData.sheets.length === 0) {
          console.log('保存的数据格式无效')
          localStorage.removeItem(this.table.backupKey)
          resolve(false)
          return
        }
        
        // 检查数据时效性（24小时内）
        if (parsedData.timestamp) {
          const now = Date.now()
          const oneDay = 24 * 60 * 60 * 1000
          if (now - parsedData.timestamp > oneDay) {
            console.log('保存的数据已过期')
            localStorage.removeItem(this.table.backupKey)
            resolve(false)
            return
          }
        }
          
        // 等待表格就绪 - 使用修改后的方法
        this.waitForTableFullyReady().then((luckysheet) => {
          if (!luckysheet) {
            console.log('获取表格实例失败')
            resolve(false)
            return
          }
          
          // 传递 luckysheet 实例，而不是重新获取
          this.loadDataToTable(luckysheet, parsedData.sheets)
            .then((result) => {
              console.log('数据恢复成功')
              resolve(true)
            })
            .catch((error) => {
              console.error('加载数据到表格失败:', error)
              resolve(false)
            })
            
        }).catch((error) => {
          console.log('等待表格就绪失败:', error)
          resolve(false)
        })
      })
    },

    /**
     * 等待表格就绪
     */
    waitForTableReady(maxRetries = 30, delay = 200) {
      return new Promise((resolve, reject) => {
        let attempts = 0
        
        const check = () => {
          attempts++
          
          try {
            console.log(`尝试获取表格实例 (${attempts}/${maxRetries})...`)
            
            const iframe = this.$refs.luckysheetFrame
            if (!iframe) {
              console.log('iframe 引用不存在')
              if (attempts >= maxRetries) {
                reject(new Error('iframe 引用未找到'))
              } else {
                setTimeout(check, delay)
              }
              return
            }
            
            const iframeWindow = iframe.contentWindow
            if (!iframeWindow) {
              console.log('iframe 窗口未加载')
              if (attempts >= maxRetries) {
                reject(new Error('iframe 窗口未加载完成'))
              } else {
                setTimeout(check, delay)
              }
              return
            }
            
            const luckysheet = iframeWindow.luckysheet
            if (!luckysheet) {
              console.log('Luckysheet 对象不存在')
              if (attempts >= maxRetries) {
                reject(new Error('Luckysheet 插件未初始化'))
              } else {
                setTimeout(check, delay)
              }
              return
            }
            
            // 检查是否已经创建
            if (typeof luckysheet.create !== 'function') {
              console.log('Luckysheet.create 方法不存在')
              if (attempts >= maxRetries) {
                reject(new Error('Luckysheet 创建方法未就绪'))
              } else {
                setTimeout(check, delay)
              }
              return
            }
            
            console.log('表格实例获取成功')
            resolve(luckysheet)
            
          } catch (error) {
            console.error(`检查表格时出错 (${attempts}/${maxRetries}):`, error.message)
            if (attempts >= maxRetries) {
              reject(new Error(`表格检查失败: ${error.message}`))
            } else {
              setTimeout(check, delay)
            }
          }
        }
        
        check()
      })
    },

    /**
     * 加载数据到表格
     */
    async loadDataToTable(luckysheet, sheets) {
      return new Promise((resolve, reject) => {
        try {
          console.log('准备恢复数据，sheets数量:', sheets.length)
          
          // 准备数据，确保格式完全符合 LuckySheet 要求
          const formattedSheets = sheets.map((sheet, index) => {
            // 清理并格式化 celldata
            let cleanCelldata = []
            if (sheet.celldata && Array.isArray(sheet.celldata)) {
              // 过滤无效数据
              cleanCelldata = sheet.celldata
                .filter(cell => cell && typeof cell === 'object' && 
                      cell.r !== undefined && cell.c !== undefined && cell.v !== undefined)
                .map(cell => ({
                  r: Number(cell.r),
                  c: Number(cell.c),
                  v: cell.v
                }))
            }
            
            // 如果没有数据，添加一个空单元格来避免空数据错误
            if (cleanCelldata.length === 0) {
              cleanCelldata.push({
                r: 0,
                c: 0,
                v: ''
              })
            }
            
            // 确保每个sheet都有必要的属性
            return {
              name: sheet.name || `Sheet${index + 1}`,
              index: index,
              status: 1,
              order: index,
              celldata: cleanCelldata,
              config: {
                columnlen: sheet.config?.columnlen || {},
                rowlen: sheet.config?.rowlen || 40,
                merge: {}, // 必须有merge字段，即使为空
                borderInfo: [], // 必须有borderInfo字段
                authority: null, // 权限设置
                visibledatarow: [], // 可见行
                visibledatacolumn: [], // 可见列
                rowhidden: {}, // 隐藏行
                colhidden: {}, // 隐藏列
                chart: [], // 图表配置
                filter_select: null, // 筛选
                filter: null, // 筛选条件
                luckysheet_select_save: [], // 选区保存
                calcChain: [], // 公式链
                isPivotTable: false, // 是否是数据透视表
                pivotTable: null, // 数据透视表配置
                dataVerification: {}, // 数据验证
                frozen: null, // 冻结设置
                images: [], // 图片
                hyperlinks: null, // 超链接
                conditionformat: null // 条件格式
              }
            }
          })
          
          console.log('格式化后的数据:', JSON.stringify(formattedSheets[0]).substring(0, 200) + '...')
          
          // 检查是否有保存的数据需要恢复
          if (!luckysheet) {
            reject(new Error('Luckysheet实例不存在'))
            return
          }
          
           // 清理现有实例
          if (typeof luckysheet.destroy === 'function') {
            try {
              luckysheet.destroy()
              console.log('已销毁现有表格实例')
            } catch (e) {
              console.log('销毁表格实例失败:', e.message)
            }
          }
          
          // 等待DOM更新
          setTimeout(() => {
            try {
              // 关键修改：通过 iframe 获取容器，而不是 document.getElementById
              const iframe = this.$refs.luckysheetFrame
              if (!iframe || !iframe.contentWindow) {
                reject(new Error('iframe未就绪'))
                return
              }
              
              // 检查容器是否存在
              const container = iframe.contentWindow.document.getElementById('luckysheet')
              if (!container) {
                reject(new Error('表格容器未找到'))
                return
              }
              
              // 清空容器
              container.innerHTML = ''
              
              // 创建新实例（使用 iframe 中的 luckysheet 对象）
              const iframeLuckysheet = iframe.contentWindow.luckysheet
              if (!iframeLuckysheet) {
                reject(new Error('iframe中的luckysheet对象不存在'))
                return
              }
              
              iframeLuckysheet.create({
                container: 'luckysheet',
                lang: 'zh',
                data: formattedSheets,
                title: '学生成绩数据表',
                userInfo: false,
                showtoolbar: true,
                showinfobar: false,
                showsheetbar: true,
                showstatisticBar: true,
                allowEdit: true,
                sheetFormulaBar: false,
                enableAddRow: true,
                enableAddCol: true,
                row: 40,
                column: 20,
                forceCalculation: false
              })
              
              console.log('数据恢复成功，表格已重新创建')
              
              setTimeout(() => {
                resolve(true)
              }, 500)
              
            } catch (error) {
              console.error('创建表格失败:', error)
              reject(error)
            }
          }, 300)
          
        } catch (error) {
          console.error('准备数据失败:', error)
          reject(error)
        }
      })
    },

    /**
     * 转换单元格值
     */
    extractCellValue(cell) {
      if (cell == null) return ''
      
      if (typeof cell === 'object') {
        if (cell.v !== undefined) return cell.v
        if (cell.m !== undefined) return cell.m
        return ''
      }
      
      return cell
    },

    /**
     * 转换表格数据格式
     */
    convertSheetData(sheetData) {
      if (!sheetData || !Array.isArray(sheetData)) {
        return [['', '', '', '']]
      }
      
      return sheetData.map(row => {
        if (!row || !Array.isArray(row)) {
          return ['', '', '', '']
        }
        
        return row.map(cell => {
          if (cell && typeof cell === 'object') {
            return cell.v !== undefined ? cell.v : ''
          }
          return cell || ''
        })
      })
    },

    /**
     * 记录处理错误
     */
    logProcessingError(error) {
      const errorLog = {
        timestamp: new Date().toISOString(),
        configId: this.dataProcessing.configId,
        step: this.dataProcessing.status,
        message: error.message,
        stack: error.stack
      }
      
      // 可以保存到localStorage或发送到服务器
      console.error('处理错误日志:', errorLog)
    },

    /**
     * 步骤7的初始化（简化版）
     */
    initializeStep7() {
      console.log('初始化步骤7')
      
      // 重置表格状态
      this.table.isReady = false
      this.table.isDataValid = false
      
      // 重置iframe URL以触发重新加载
      this.luckysheetUrl = `/luckysheet.html?t=${Date.now()}`;
      
      // 标记为正在加载
      this.step7.isLoading = true
    },

    /**
     * iframe加载完成回调
     */
    async onLuckysheetLoad() {
      console.log('表格加载完成')
      
      try {
        // 等待表格就绪
        const luckysheet = await this.waitForTableReady()
        this.table.isReady = true
        
        // 检查是否需要恢复数据
        const savedData = localStorage.getItem(this.table.backupKey)
        if (savedData) {
          const restored = await this.restoreTableData()
          if (restored) {
            console.log('数据恢复成功')
            this.$message.success('已恢复上次填写的表格数据')
          } else {
            // 恢复失败，加载默认模板
            await this.loadDefaultTemplate(luckysheet)
          }
        } else {
          // 加载默认模板
          await this.loadDefaultTemplate(luckysheet)
        }
        
        this.step7.isLoading = false
        console.log('表格初始化完成')
        
        // 显示操作提示
        this.showDataEntryGuide()
        
      } catch (error) {
        console.error('表格初始化失败:', error)
        this.step7.isLoading = false
        this.$message.error('表格加载失败，请刷新页面重试')
      }
    },

    /**
     * 显示数据填写指南
     */
    showDataEntryGuide() {
      // 延迟显示，确保表格完全渲染
      setTimeout(() => {
        this.$notify({
          title: '数据填写说明',
          message: '请直接在表格中填写学生成绩数据：<br>' +
                  '1. 每个sheet代表一种考核方式<br>' +
                  '2. 第一行为表头，从第二行开始填写<br>' +
                  '3. 填写完成后点击"开始处理数据"按钮<br>' +
                  '4. 系统会自动检查数据完整性',
          type: 'info',
          duration: 8000,
          dangerouslyUseHTMLString: true
        })
      }, 1000)
    },

    // 添加生成配置ID的方法
    generateConfigId() {
      const timestamp = Date.now()
      const randomStr = Math.random().toString(36).substring(2, 10)
      return `config_${timestamp}_${randomStr}`
    },

    /**
     * 加载配置（基于原loadDefaultConfig方法重构）
     */
    async loadDefaultConfiguration() {
      try {
        this.loading = true
        console.log('开始调用loadDefaultConfig API...')
        
        const response = await loadDefaultConfig()
        console.log('API完整响应:', response)
        
        if (!response) {
          throw new Error('API响应为空')
        }
        
        if (typeof response !== 'object') {
          throw new Error('API响应格式错误')
        }
        
        if (response.code !== 200) {
          throw new Error('API调用失败: ' + (response.msg || '未知错误'))
        }
        
        if (response && response.code === 200 && response.configId && response.config) {
          console.log('API调用成功，configId:', response.configId)
          this.dataProcessing.configId = response.configId
          
          // 保存配置用于后续处理
          this.examConfig = response.config
          
          return response
        } else {
          throw new Error('API响应数据不完整')
        }
        
      } catch (error) {
        console.error('加载配置失败:', error)
        throw error
      } finally {
        this.loading = false
      }
    },

    convertToBackendConfig(frontendConfig) {
      // 1. 基础配置
      const backendConfig = {
        regularGrade: this.proportions.regular || 0,
        labGrade: this.proportions.lab || 0,
        finalExam: this.proportions.final || 0,
        regularTotalScore: this.scores.regular || 100,
        labTotalScore: this.scores.lab || 100,
        finalTotalScore: this.scores.final || 100,
        courseTargets: this.targetOptions,  // ["目标1", "目标2", ...]
        courseTargetProportions: []
      };

      // 2. 构建 courseTargetProportions
      // 直接使用已经计算好的 weighted 值
      this.targetProportionsTable.forEach(row => {
        backendConfig.courseTargetProportions.push({
          courseTarget: row.target,
          regularGrade: parseFloat(row.weightedRegular.toFixed(2)),
          lab: parseFloat(row.weightedLab.toFixed(2)),
          finalExam: parseFloat(row.weightedFinal.toFixed(2)),
          total: parseFloat(row.weightedTotal.toFixed(2))
        });
      });

      // 3. examPaper
      if (this.selectedAssessmentTypes.includes('final') && this.rows && this.rows.length > 0) {
        const examPaper = [];
        this.rows.forEach((row, rowIndex) => {
          const questions = [];
          row.cells.forEach((cell, colIndex) => {
            if (cell.score > 0) {
              questions.push({
                questionNumber: colIndex + 1,
                score: cell.score,
                target: cell.target || ''
              });
            }
          });
          if (questions.length > 0) {
            examPaper.push({
              title: String(rowIndex + 1),
              questions: questions
            });
          }
        });
        backendConfig.examPaper = examPaper;
      } else {
        backendConfig.examPaper = [];
      }

      return backendConfig;
    },

    // ========== 步骤8: 结果展示和报告下载 ==========
    
    handleImageError(event, filename) {
      console.error(`图片加载失败: ${filename}`, event)
      // 可以显示一个占位图或错误信息
      event.target.style.display = 'none'
      
      // 修复这一行：将可选链操作符改为传统判断
      if (event.target.nextElementSibling) {
        event.target.nextElementSibling.style.display = 'block'
      }
    },
    
    downloadReport() {
      try {
        // 模拟报告下载
        this.$message.info('开始生成并下载报告...')
        
        // 在实际应用中，这里应该调用后端API下载Word报告
        // 暂时使用模拟下载
        setTimeout(() => {
          this.$message.success('报告下载完成！')
          
          // 创建模拟下载
          const blob = new Blob(['模拟报告内容'], { type: 'application/msword' })
          const url = URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = `课程目标达成评价报告_${new Date().toLocaleDateString()}.doc`
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          URL.revokeObjectURL(url)
        }, 1500)
        
      } catch (error) {
        console.error('下载报告失败:', error)
        this.$message.error('下载报告失败：' + error.message)
      }
    },

    // 图片URL生成方法
    getImageUrl(fileName) {
      const reportId = this.dataProcessing.reportId;
      if (!fileName || !reportId) return null;
      // 添加时间戳避免浏览器缓存
      const timestamp = Date.now();
      const url = `/dev-api/luckysheet/result/${reportId}/${encodeURIComponent(fileName)}?t=${timestamp}`;
      console.log('生成图片URL:', url);
      return url;
    },

    // 图片加载错误处理
    handleImageError(event, fileName) {
      console.warn('图片加载失败:', fileName, '错误:', event)
      console.log('尝试加载的URL:', event.target.src)
      
      // PNG加载失败，显示错误占位符
      event.target.style.display = 'none'
      const placeholder = event.target.parentNode.querySelector('.image-placeholder')
      if (!placeholder) {
        const div = document.createElement('div')
        div.className = 'image-placeholder'
        div.innerHTML = `
          <div style="
            display: flex; 
            flex-direction: column;
            align-items: center; 
            justify-content: center; 
            min-height: 300px;
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            border: 2px dashed #ddd; 
            border-radius: 8px;
            color: #666; 
            text-align: center;
            font-size: 14px;
            padding: 20px;
            margin: 10px 0;
          ">
            <div style="font-size: 48px; margin-bottom: 15px; color: #ff6b6b;">📊</div>
            <div style="font-size: 18px; font-weight: bold; margin-bottom: 10px; color: #333;">
              图片加载失败
            </div>
            <div style="font-size: 14px; color: #666; margin-bottom: 15px;">
              无法加载图片文件：${fileName}
            </div>
            <div style="font-size: 12px; color: #999; margin-bottom: 15px;">
              URL: ${event.target.src}
            </div>
            <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 4px; padding: 15px; margin: 10px 0; text-align: left; max-width: 500px;">
              <div style="font-weight: bold; color: #856404; margin-bottom: 8px;">🔧 检查要点：</div>
              <div style="color: #856404; line-height: 1.6;">
                <div>• 确认后端服务正在运行 (localhost:8080)</div>
                <div>• 检查文件是否存在于服务器</div>
                <div>• 验证图片文件权限</div>
                <div>• 查看浏览器开发者工具的网络面板</div>
              </div>
            </div>
          </div>
        `
        event.target.parentNode.appendChild(div)
      }
    },

    // 下载报告方法
    async downloadReport() {
      try {
        if (!this.dataProcessing.reportId || !this.dataProcessing.configId) {
          this.$message.error('报告尚未生成，请先处理数据')
          return
        }
        
        // 使用正确的API前缀路径
        const downloadUrl = `/dev-api/luckysheet/download-report/${this.dataProcessing.reportId}?configId=${this.dataProcessing.configId}`
        console.log('下载URL:', downloadUrl)
        
        window.open(downloadUrl)
        this.$message.success('报告下载开始')
      } catch (error) {
        console.error('下载失败:', error)
        this.$message.error('下载失败：' + error.message)
      }
    },
 
    // ========== 工具方法 ==========
    restart() {
      // 步骤控制重置
      this.currentStep = 0
      this.loading = false

      // 考核方式选择重置
      this.selectedAssessmentTypes = ['regular', 'lab', 'final']
      this.isStep1Valid = true
      
      // 考核配置重置
      this.proportions = {
        regular: 0,
        lab: 0,
        final: 0
      }
      this.scores = {
        regular: 0,
        lab: 0,
        final: 0 
      }
      this.totalMessage = '请设置各考核方式的占比和总分'
      
      // 课程目标重置
      this.targetCount = 2
      this.supportRelationTable = []
      
      // 占比分配重置
      this.targetProportionsTable = []
      this.proportionError = ''
      
      // 期末试卷重置
      this.rows = []
      this.columns = 1
      this.totalScore = 0
      this.paperValidationMessage = '请设置试卷'
      this.paperValidationType = 'info'
      this.isStep6Valid = false

      // LuckySheet 相关重置
      this.luckysheetUrl = process.env.VUE_APP_BASE_API + "/luckysheet.html"
      this.luckysheetData = null
      
      // 配置文件重置
      this.examConfig = null

      // 步骤6专用状态重置
      this.step6 = {
        loading: false,
        lastValidation: null
      }
      
      // 步骤7专用状态重置
      this.step7 = {
        isInitialized: true,
        isLoading: true,
        hasData: false,
        luckysheetReady: false,
        lastError: null
      }

      // ========== 统一的数据处理状态重置 ==========
      this.dataProcessing = {
        // 处理状态
        status: 'idle', // 'idle' | 'saving' | 'extracting' | 'validating' | 'submitting' | 'processing' | 'success' | 'error'
        progress: 0,
        message: '',
        details: '',
        currentSheet: '',
        
        // 数据状态
        hasSavedData: false,
        lastSaveTime: null,
        configId: '',
        reportId: '',
        
        // 提取的数据
        extractedData: null,
        
        // 处理结果
        results: null,
        error: null
      }
      
      // ========== 表格相关状态重置 ==========
      this.table = {
        isReady: false,
        isDataValid: false,
        lastValidCheck: null,
        backupKey: 'luckysheet_backup_data',
        studentCount: 0,
      }

      // UI状态重置
      this.activeTab = 'gradeDistribution'
      
      // 其他配置重置
      this.finalConfig = null
      this.csvHeaders = {
        final: [],
        regular: [],
        lab: []
      }

      // 清理本地存储
      try {
        localStorage.removeItem(this.table.backupKey)
        console.log('已清理本地存储数据')
      } catch (error) {
        console.warn('清理本地存储失败:', error)
      }

      // 清理Luckysheet iframe内容
      if (this.$refs.luckysheetFrame) {
        const iframe = this.$refs.luckysheetFrame
        if (iframe.contentWindow && iframe.contentWindow.luckysheet) {
          try {
            const luckysheet = iframe.contentWindow.luckysheet
            if (typeof luckysheet.destroy === 'function') {
              luckysheet.destroy()
            }
          } catch (error) {
            console.warn('清理Luckysheet实例失败:', error)
          }
        }
      }

      this.cachedLuckysheet = null;

      // 重新初始化第一步
      this.$nextTick(() => {
        this.validateStep1()
        
        // 如果有需要，重新加载表格
        if (this.currentStep === 6) {
          setTimeout(() => {
            this.initializeStep7()
          }, 100)
        }
        
        // 触发视图更新
        this.$forceUpdate()
      })
      
      // 显示成功消息
      this.$message.success('已重置所有配置，可以重新开始')
      
      // 添加调试信息
      console.log('系统已重置，当前步骤:', this.currentStep)
      console.log('数据处理状态:', this.dataProcessing.status)
      console.log('表格状态:', this.table)
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.box-card {
  margin-bottom: 20px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both;
}

h3 {
  color: #303133;
  margin-bottom: 15px;
}

p {
  color: #606266;
  margin-bottom: 20px;
}

/* 新增样式 */
.step {
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  margin-bottom: 20px;
}

.input-group {
  margin-bottom: 15px;
}

.input-group label {
  margin-right: 20px;
  margin-bottom: 10px;
  display: inline-block;
}

.input-group input {
  margin-left: 5px;
  width: 60px;
  padding: 5px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

button {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 10px;
}

button:hover {
  background-color: #66b1ff;
}

.error {
  color: #f56c6c;
}

.success {
  color: #67c23a;
}

/* LuckySheet 容器样式 */
.luckysheet-container {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  padding: 10px;
  margin-bottom: 20px;
  background: #f8f9fa;
}

.luckysheet-container iframe {
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.button-group {
  margin-top: 15px;
}

.button-group .el-button {
  margin: 0 5px;
}

/* 步骤操作按钮 */
.step-actions {
  margin-top: 20px;
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

/* 进度显示 */
.process-status {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 4px;
  margin: 20px 0;
}

/* 全屏LuckySheet容器 */
.luckysheet-fullscreen {
  position: relative;
  width: 100%;
  height: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 20px;
  background: #f8f9fa;
}

.luckysheet-fullscreen iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 加载状态 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

</style>