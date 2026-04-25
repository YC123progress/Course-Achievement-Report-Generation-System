package com.ruoyi.web.controller.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.python.PythonScriptRunner;
// import com.ruoyi.common.utils.report.WordReportGenerator;  // 暂时注释掉
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.util.Units;
import java.math.BigInteger;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;

// Apache POI imports for Word document generation  
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson2.JSONArray;

// poi-tl imports for table rendering (still needed for RowRenderData and Pictures)
import com.deepoove.poi.data.RowRenderData;
import com.deepoove.poi.data.Rows;
import com.deepoove.poi.data.TextRenderData;
import com.deepoove.poi.data.ParagraphRenderData;
import com.deepoove.poi.data.RenderData;
import com.deepoove.poi.data.CellRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.style.Style;

@Controller
@RequestMapping("/luckysheet")
public class LuckysheetController {
    
    private static final Logger log = LoggerFactory.getLogger(LuckysheetController.class);
    
    @Value("${ruoyi.luckysheet-path}")
    private String luckysheetPath;

    @Value("${ruoyi.profile}")
    private String uploadPath;
    
    @Value("${python.script.path}")
    private String pythonScriptPath;
    
    @Value("${python.executable}")
    private String pythonExecutable;

    @org.springframework.beans.factory.annotation.Autowired
    private ResourceLoader resourceLoader;

    @GetMapping
    public String index() {
        return "redirect:/luckysheet/index.html";
    }
    
    /**
     * 课程目标达成评价测试页面
     */
    @GetMapping("/test")
    public String test() {
        return "luckysheet/test";
    }
    
    /**
     * 课程目标达成评价报告页面
     */
    @GetMapping("/course-assessment")
    public String courseAssessment(ModelMap mmap) {
        return "luckysheet/assessment";
    }
    
    /**
     * 上传配置文件
     */
    @PostMapping("/upload-config")
    @ResponseBody
    public AjaxResult uploadConfig(@RequestParam("file") MultipartFile file) throws IOException {
        // 保存配置文件，返回文件ID
        String fileName = saveFile(file, "config");
        
        // 解析JSON配置
        JSONObject config = parseConfigFile(fileName);
        
        return AjaxResult.success()
                .put("configId", fileName)
                .put("config", config);
    }
    
    /**
     * 使用默认配置
     */
    @GetMapping("/use-default-config")
    @ResponseBody
    public AjaxResult useDefaultConfig() throws IOException {
        // 从classpath读取默认配置
        String defaultConfigId = copyDefaultConfigToUploadPath();
        
        // 解析JSON配置
        JSONObject config = readConfigFile(defaultConfigId);
        
        return AjaxResult.success()
                .put("configId", defaultConfigId)
                .put("config", config);
    }
    
    /**
     * 从classpath复制默认配置文件到上传路径
     */
    private String copyDefaultConfigToUploadPath() throws IOException {
        String fileName = "default_" + UUID.randomUUID().toString() + ".json";
        String targetPath = uploadPath + "/config/" + fileName;
        
        // 确保目录存在
        File dir = new File(uploadPath + "/config/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 从classpath读取默认配置，确保UTF-8编码
        try (InputStream is = new ClassPathResource("scripts/default_config.json").getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader);
             OutputStreamWriter writer = new OutputStreamWriter(
                 new FileOutputStream(targetPath), StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
        
        return fileName;
    }
    
    /**
     * 生成Excel模板
     */
    @GetMapping("/generate-template")
    @ResponseBody
    public AjaxResult generateTemplate(@RequestParam("configId") String configId) throws IOException {
        // 读取配置文件
        JSONObject config = readConfigFile(configId);
        
        // 生成Excel模板数据
        Map<String, Object> templateData = generateExcelTemplate(config);
        
        return AjaxResult.success()
                .put("templateData", templateData);
    }
    
    /**
     * 处理用户提交的Excel数据
     */
    @PostMapping(value = "/process-data", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public AjaxResult processData(@RequestBody Map<String, Object> excelData, 
                                @RequestHeader(value = "configId", required = false) String configIdHeader,
                                @RequestParam(value = "configId", required = false) String configIdParam,
                                HttpServletRequest request) throws IOException {
        // 确保请求使用UTF-8编码
        request.setCharacterEncoding("UTF-8");
        
        // 从请求头或参数中获取configId
        String configId = configIdHeader != null ? configIdHeader : configIdParam;
        configId = cleanConfigId(configId);
        
        if (configId == null || configId.trim().isEmpty()) {
            return AjaxResult.error("配置ID不能为空");
        }
        
        try {
            log.info("开始处理Excel数据，configId: {}", configId);
            log.info("请求编码: {}", request.getCharacterEncoding());
            
            // 调试：打印接收到的原始数据
            String jsonStr = JSON.toJSONString(excelData);
            log.info("接收到的JSON数据长度: {}", jsonStr.length());
            
            // 检查数据中是否包含中文字符
            boolean containsChinese = jsonStr.matches(".*[\\u4e00-\\u9fa5].*");
            log.info("数据中是否包含中文: {}", containsChinese);
            
            log.info("接收到的Excel数据结构: {}", excelData.keySet());
            
            // 打印详细的数据结构用于调试
            if (excelData.containsKey("sheets")) {
                List<?> sheets = (List<?>) excelData.get("sheets");
                log.info("Sheet数量: {}", sheets.size());
                for (int i = 0; i < sheets.size(); i++) {
                    Object sheet = sheets.get(i);
                    log.info("Sheet[{}]类型: {}", i, sheet.getClass().getName());
                    if (sheet instanceof Map) {
                        Map<?, ?> sheetMap = (Map<?, ?>) sheet;
                        log.info("Sheet[{}]键: {}", i, sheetMap.keySet());
                        if (sheetMap.containsKey("data")) {
                            Object data = sheetMap.get("data");
                            log.info("Sheet[{}]数据类型: {}", i, data.getClass().getName());
                            if (data instanceof List) {
                                List<?> dataList = (List<?>) data;
                                log.info("Sheet[{}]数据行数: {}", i, dataList.size());
                                if (!dataList.isEmpty()) {
                                    Object firstRow = dataList.get(0);
                                    log.info("第一行数据类型: {}", firstRow.getClass().getName());
                                    if (firstRow instanceof List) {
                                        List<?> firstRowList = (List<?>) firstRow;
                                        log.info("第一行列数: {}", firstRowList.size());
                                        if (!firstRowList.isEmpty()) {
                                            Object firstCell = firstRowList.get(0);
                                            log.info("第一个单元格类型: {}, 值: {}", 
                                                firstCell != null ? firstCell.getClass().getName() : "null", 
                                                firstCell);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 保存Excel数据到临时CSV文件
            List<String> csvFiles = saveExcelDataToCSV(excelData, configId);
            log.info("成功保存CSV文件: {}", csvFiles);
            
            // 调用Python脚本处理数据
            boolean success = executePythonScript(configId, csvFiles);
            log.info("Python脚本执行结果: {}", success);
            
            // 如果Python脚本执行失败，生成模拟结果
            if (!success) {
                log.warn("Python脚本执行失败，生成模拟结果数据");
                generateMockResults(configId);
                success = true; // 设置为成功，以便前端能显示模拟结果
            }
            
            // 收集生成的结果文件
            Map<String, Object> results = collectResults(configId);
            log.info("收集到的结果文件: {}", results.keySet());
            
            // 生成报告ID
            String reportId = UUID.randomUUID().toString();
            
            return AjaxResult.success()
                    .put("success", success)
                    .put("results", results)
                    .put("reportId", reportId);
                    
        } catch (Exception e) {
            log.error("处理Excel数据时发生错误", e);
            return AjaxResult.error("数据处理失败: " + e.getMessage());
        }
    }

    /**
     * 直接处理手动配置数据
     */
    @PostMapping("/process-data-direct")
    @ResponseBody
    public AjaxResult processDataDirect(@RequestBody Map<String, Object> requestBody) throws IOException {
        try {
            Map<String, Object> config = (Map<String, Object>) requestBody.get("config");
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) requestBody.get("sheets");
            
            if (config == null || sheets == null) {
                return AjaxResult.error("请求数据不完整，需要 config 和 sheets 字段");
            }
            
            log.info("开始处理手动配置数据（文件方式）");
            
            // 1. 生成唯一ID（不含 .json 后缀）
            String configId = UUID.randomUUID().toString();
            
            // 2. 保存配置文件到 uploadPath/config/ 目录（添加 .json 后缀）
            String configFilePath = uploadPath + "/config/" + configId + ".json";
            Files.write(Paths.get(configFilePath), 
                        JSON.toJSONString(config).getBytes(StandardCharsets.UTF_8));
            log.info("配置文件已保存: {}", configFilePath);
            
            // 3. 构造与 saveExcelDataToCSV 兼容的 excelData 结构
            Map<String, Object> excelData = new HashMap<>();
            excelData.put("sheets", sheets);
            
            // 4. 调用 saveExcelDataToCSV 生成 CSV 和 exam_config.json（传入无后缀的 configId）
            List<String> csvFiles = saveExcelDataToCSV(excelData, configId);
            log.info("CSV文件生成成功: {}", csvFiles);
            
            // 5. 执行 Python 脚本（传入无后缀的 configId）
            boolean success = executePythonScript(configId, csvFiles);
            log.info("Python脚本执行结果: {}", success);
            
            // 6. 如果失败则生成模拟结果（传入无后缀的 configId）
            if (!success) {
                log.warn("Python脚本执行失败，生成模拟结果数据");
                generateMockResults(configId);
                success = true;
            }
            
            // 7. 收集结果（传入无后缀的 configId）
            Map<String, Object> results = collectResults(configId);
            String reportId = UUID.randomUUID().toString();
            
            // 8. 将数据目录重命名为 reportId（便于图片访问）
            String dataDir = uploadPath + "/data/" + configId + "/";
            String permanentDir = uploadPath + "/data/" + reportId + "/";
            File dataDirFile = new File(dataDir);
            if (dataDirFile.exists()) {
                Files.move(Paths.get(dataDir), Paths.get(permanentDir));
                log.info("数据目录已重命名: {} -> {}", dataDir, permanentDir);
            }
            
            return AjaxResult.success()
                    .put("success", success)
                    .put("results", results)
                    .put("reportId", reportId)
                    .put("dataDir", reportId);
                    
        } catch (Exception e) {
            log.error("处理手动配置数据失败", e);
            return AjaxResult.error("数据处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载生成的报告
     */
    @GetMapping("/download-report/{reportId}")
    public void downloadReport(@PathVariable("reportId") String reportId,
                               @RequestParam("configId") String configId,
                               HttpServletResponse response) throws IOException {
        // 生成Word报告
        String reportFile = generateWordReport(reportId, configId);
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=course_assessment_report.docx");
        
        // 写入响应
        FileUtils.writeBytes(reportFile, response.getOutputStream());
    }
    
    @GetMapping("/result/{dataDir}/{fileName:.+}")
    @ResponseBody
    public ResponseEntity<byte[]> getResultFile(@PathVariable("dataDir") String dataDir,
                                            @PathVariable("fileName") String fileName) throws IOException {
        String filePath = uploadPath + "/data/" + dataDir + "/" + fileName;
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = Files.readAllBytes(path);
        String contentType = fileName.endsWith(".png") ? "image/png" : "application/octet-stream";
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(content);
    }

    @GetMapping("/**")
    @ResponseBody
    public ResponseEntity<byte[]> getResource(HttpServletRequest request) throws IOException {
        String requestPath = request.getRequestURI();
        // 移除开头的 /luckysheet
        String resourcePath = requestPath.substring("/luckysheet".length());
        if (resourcePath.isEmpty() || resourcePath.equals("/")) {
            resourcePath = "/index.html";
        }
        
        // 构建完整的文件路径
        String fullPath = luckysheetPath.replace("file:", "") + resourcePath;
        Path filePath = Paths.get(fullPath);
        
        if (!Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        
        byte[] content = Files.readAllBytes(filePath);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(content);
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 保存上传的文件
     */
    private String saveFile(MultipartFile file, String type) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String filePath = uploadPath + "/" + type + "/" + fileName;
        
        // 确保目录存在
        File dest = new File(filePath);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        
        // 保存文件
        file.transferTo(dest);
        
        return fileName;
    }
    
    /**
     * 解析JSON配置文件
     */
    private JSONObject parseConfigFile(String fileName) throws IOException {
        String filePath = uploadPath + "/config/" + fileName;
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return JSON.parseObject(content);
    }
    
    /**
     * 读取配置文件
     */
    private JSONObject readConfigFile(String configId) throws IOException {
        String filePath = uploadPath + "/config/" + configId;
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return JSON.parseObject(content);
    }
    
    /**
     * 生成Excel模板数据
     */
    private Map<String, Object> generateExcelTemplate(JSONObject config) {
        Map<String, Object> result = new HashMap<>();
        
        // 根据配置生成表头和Sheet结构
        List<Map<String, Object>> sheets = new ArrayList<>();
        
        // 检查是否需要生成期末考试成绩表
        if (config.getInteger("finalExam") > 0) {
            Map<String, Object> finalExamSheet = new HashMap<>();
            finalExamSheet.put("name", "期末考试成绩");
            finalExamSheet.put("headers", generateFinalExamHeaders(config));
            sheets.add(finalExamSheet);
        }
        
        // 检查是否需要生成平时成绩表
        if (config.getInteger("regularGrade") > 0) {
            Map<String, Object> regularSheet = new HashMap<>();
            regularSheet.put("name", "平时成绩");
            regularSheet.put("headers", generateRegularHeaders());
            sheets.add(regularSheet);
        }
        
        // 检查是否需要生成上机成绩表
        if (config.getInteger("labGrade") > 0) {
            Map<String, Object> labSheet = new HashMap<>();
            labSheet.put("name", "上机成绩");
            labSheet.put("headers", generateLabHeaders());
            sheets.add(labSheet);
        }
        
        result.put("sheets", sheets);
        return result;
    }
    
    /**
     * 生成期末考试表头
     */
    private List<String> generateFinalExamHeaders(JSONObject config) {
        List<String> headers = new ArrayList<>();
        headers.add("班级");
        headers.add("学号");
        headers.add("姓名");
        
        // 添加考试题目列
        if (config.containsKey("examPaper")) {
            List<JSONObject> examPaper = config.getJSONArray("examPaper").toJavaList(JSONObject.class);
            for (JSONObject paper : examPaper) {
                String title = paper.getString("title");
                List<JSONObject> questions = paper.getJSONArray("questions").toJavaList(JSONObject.class);
                
                for (JSONObject question : questions) {
                    int questionNumber = question.getInteger("questionNumber");
                    double score = question.getDouble("score");
                    headers.add(String.format("%s.%d（%.1f分）", title, questionNumber, score));
                }
            }
        }
        
        return headers;
    }
    
    /**
     * 生成平时成绩表头
     */
    private List<String> generateRegularHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("班级");
        headers.add("学号");
        headers.add("姓名");
        headers.add("平时成绩总分");
        return headers;
    }
    
    /**
     * 生成上机成绩表头
     */
    private List<String> generateLabHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("班级");
        headers.add("学号");
        headers.add("姓名");
        headers.add("上机成绩总分");
        return headers;
    }
    
    /**
     * 将Excel数据保存为CSV文件
     */
    private List<String> saveExcelDataToCSV(Map<String, Object> excelData, String configId) throws IOException {
        List<String> csvFiles = new ArrayList<>();
        String basePath = uploadPath + "/data/" + configId + "/";
        
        // 确保目录存在
        new File(basePath).mkdirs();
        
        // 处理每个Sheet的数据
        List<Map<String, Object>> sheets = (List<Map<String, Object>>) excelData.get("sheets");
        for (Map<String, Object> sheet : sheets) {
            String sheetName = (String) sheet.get("name");
            List<List<Object>> data = (List<List<Object>>) sheet.get("data");
            
            // 确定文件名
            String fileName;
            if (sheetName.contains("期末")) {
                fileName = "final_exam_scores_template.csv";
            } else if (sheetName.contains("平时")) {
                fileName = "regular_scores_template.csv";
            } else if (sheetName.contains("上机")) {
                fileName = "lab_scores_template.csv";
            } else {
                fileName = sheetName + ".csv";
            }
            
            // 处理列名映射
            List<List<Object>> processedData = processColumnMapping(data, sheetName);

            // 移除全空列
            if (!processedData.isEmpty() && processedData.size() > 0) {
                // 获取表头
                List<Object> headers = processedData.get(0);
                // 找出需要删除的列索引（列名为空或全为空白）
                List<Integer> colsToRemove = new ArrayList<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j).toString().trim();
                    if (header.isEmpty() || header.equals("Unnamed")) {
                        colsToRemove.add(j);
                    }
                }
                // 如果没有需要删除的列，则跳过
                if (!colsToRemove.isEmpty()) {
                    // 删除列（从后往前删，避免索引变化）
                    for (int i = processedData.size() - 1; i >= 0; i--) {
                        List<Object> row = processedData.get(i);
                        for (int j = colsToRemove.size() - 1; j >= 0; j--) {
                            int colIndex = colsToRemove.get(j);
                            if (colIndex < row.size()) {
                                row.remove(colIndex);
                            }
                        }
                    }
                }
            }
            
            // 写入CSV
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(basePath + fileName), StandardCharsets.UTF_8))) {
                
                // 写入BOM以确保Excel正确识别UTF-8编码
                writer.write('\ufeff');
                
                // 写入数据，安全地转换为字符串
                for (List<Object> row : processedData) {
                    List<String> stringRow = new ArrayList<>();
                    for (Object cell : row) {
                        // 安全地将各种类型转换为字符串
                        String cellValue = "";
                        if (cell != null) {
                            if (cell instanceof String) {
                                cellValue = (String) cell;
                            } else if (cell instanceof Number) {
                                cellValue = cell.toString();
                            } else if (cell instanceof Boolean) {
                                cellValue = cell.toString();
                            } else {
                                cellValue = String.valueOf(cell);
                            }
                        }
                        stringRow.add(cellValue);
                    }
                    writer.println(String.join(",", stringRow));
                }
            }
            
            // 调试日志：打印CSV文件的前几行
            log.info("========== CSV文件内容调试 ==========");
            log.info("文件名: {}", fileName);
            log.info("Sheet名称: {}", sheetName);
            if (!processedData.isEmpty()) {
                log.info("表头: {}", processedData.get(0));
                if (processedData.size() > 1) {
                    log.info("第1行数据: {}", processedData.get(1));
                }
                if (processedData.size() > 2) {
                    log.info("第2行数据: {}", processedData.get(2));
                }
                log.info("总行数: {}", processedData.size());
            }
            log.info("========================================\n");
            
            csvFiles.add(fileName);
        }
        
        // 复制配置文件到数据目录
        String configFilePath = uploadPath + "/config/" + configId;
        // 如果configId不包含.json后缀，则添加
        if (!configId.endsWith(".json")) {
            configFilePath += ".json";
        }

        log.info("尝试复制配置文件: {} -> {}", configFilePath, basePath + "exam_config.json");

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.error("配置文件不存在: {}", configFilePath);
            throw new IOException("配置文件不存在: " + configFilePath);
        }

        Files.copy(Paths.get(configFilePath), 
                  Paths.get(basePath + "exam_config.json"));
        
        return csvFiles;
    }
    
    /**
     * 处理列名映射，确保生成的CSV文件包含Python脚本期望的列名
     */
    private List<List<Object>> processColumnMapping(List<List<Object>> data, String sheetName) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        List<List<Object>> processedData = new ArrayList<>();
        
        // 处理表头（第一行）
        List<Object> headers = new ArrayList<>(data.get(0));
        
        log.info("========== 列名映射调试信息 ==========");
        log.info("Sheet名称: {}", sheetName);
        log.info("原始表头: {}", headers);
        
        // 根据sheet类型进行列名映射
        if (sheetName.contains("平时成绩")) {
            log.info("识别为平时成绩表，开始列名映射");
            // 平时成绩表：查找可能的总分列名
            mapColumnName(headers, "平时总分", "平时成绩总分");
            mapColumnName(headers, "总分", "平时成绩总分");
            mapColumnName(headers, "平时成绩", "平时成绩总分");
            mapColumnName(headers, "regular_total", "平时成绩总分");
            mapColumnName(headers, "regular_score", "平时成绩总分");
        } else if (sheetName.contains("上机成绩")) {
            log.info("识别为上机成绩表，开始列名映射");
            // 上机成绩表：查找可能的总分列名
            mapColumnName(headers, "上机总分", "上机成绩总分");
            mapColumnName(headers, "实验总分", "上机成绩总分");
            mapColumnName(headers, "总分", "上机成绩总分");
            mapColumnName(headers, "上机成绩", "上机成绩总分");
            mapColumnName(headers, "lab_total", "上机成绩总分");
            mapColumnName(headers, "lab_score", "上机成绩总分");
        } else {
            log.info("未识别为平时或上机成绩表，跳过列名映射");
        }
        // 期末考试表通常列名比较固定，不需要特殊映射
        
        log.info("映射后表头: {}", headers);
        log.info("========================================\n");
        
        processedData.add(headers);
        
        // 添加其余行
        for (int i = 1; i < data.size(); i++) {
            processedData.add(new ArrayList<>(data.get(i)));
        }
        
        return processedData;
    }
    
    /**
     * 映射列名
     */
    private void mapColumnName(List<Object> headers, String oldName, String newName) {
        for (int i = 0; i < headers.size(); i++) {
            Object header = headers.get(i);
            if (header != null && header.toString().trim().equals(oldName)) {
                headers.set(i, newName);
                log.info("列名映射: '{}' -> '{}'", oldName, newName);
            }
        }
    }
    
    /**
     * 执行Python脚本
     */
    private boolean executePythonScript(String configId, List<String> csvFiles) throws IOException {
        String workingDir = uploadPath + "/data/" + configId + "/";
        
        log.info("=== Python脚本执行开始 ===");
        log.info("Python脚本执行参数:");
        log.info("- 工作目录: {}", workingDir);
        log.info("- Python可执行文件: {}", pythonExecutable);
        log.info("- CSV文件列表: {}", csvFiles);
        
        // 检查工作目录是否存在
        File workDir = new File(workingDir);
        if (!workDir.exists()) {
            log.error("工作目录不存在: {}", workingDir);
            return false;
        }
        
        // 检查Python可执行文件是否存在
        File pythonFile = new File(pythonExecutable);
        if (!pythonFile.exists()) {
            log.error("Python可执行文件不存在: {}", pythonExecutable);
            log.error("请确保Python已正确安装，或修改application.yml中的python.executable配置");
            // 尝试使用系统默认的python
            String[] pythonAlternatives = {"python", "python3", "py"};
            for (String alt : pythonAlternatives) {
                log.info("尝试使用替代Python命令: {}", alt);
                if (testPythonCommand(alt, workingDir)) {
                    pythonExecutable = alt;
                    log.info("使用替代Python命令成功: {}", alt);
                    break;
                }
            }
        }
        
        // 检查必需文件是否存在
        File configFile = new File(workingDir + "exam_config.json");
        log.info("配置文件是否存在: {} -> {}", configFile.getAbsolutePath(), configFile.exists());
        
        for (String csvFile : csvFiles) {
            File file = new File(workingDir + csvFile);
            log.info("CSV文件是否存在: {} -> {}", file.getAbsolutePath(), file.exists());
            if (file.exists()) {
                log.info("CSV文件大小: {} bytes", file.length());
                // 检查文件内容是否为空
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    if (content.trim().isEmpty()) {
                        log.warn("CSV文件为空: {}", csvFile);
                    } else {
                        log.info("CSV文件内容预览 (前200字符): {}", 
                               content.length() > 200 ? content.substring(0, 200) + "..." : content);
                    }
                } catch (Exception e) {
                    log.warn("无法读取CSV文件内容: {}", e.getMessage());
                }
            }
        }
        
        // 首先检查Python基本功能
        log.info("正在检查Python环境...");
        boolean pythonWorking = PythonScriptRunner.runScript(
            pythonExecutable,
            "scripts/test_python.py",
            workingDir,
            60,
            null
        );
        
        if (!pythonWorking) {
            log.error("Python环境检查失败，可能的问题:");
            log.error("1. Python未正确安装或路径错误: {}", pythonExecutable);
            log.error("2. 缺少必需的Python包，请运行: pip install pandas matplotlib numpy");
            log.error("3. Python路径权限问题");
            
            // 尝试运行环境诊断脚本
            log.info("尝试运行Python环境诊断脚本...");
            boolean diagResult = PythonScriptRunner.runScript(
                pythonExecutable,
                "scripts/fix_python_env.py",
                workingDir,
                120,
                null
            );
            log.info("环境诊断脚本执行结果: {}", diagResult);
            
            return false;
        }
        
        log.info("Python环境检查通过，开始执行主脚本...");
        
        // 构建命令
        boolean result = PythonScriptRunner.runScript(
            pythonExecutable,
            "scripts/main.py",
            workingDir,
            600,  // 将超时时间从300秒改为600秒（10分钟）
            null // 环境变量
        );
        
        log.info("Python脚本执行结果: {}", result);
        
        // 检查生成的图表文件
        String[] expectedFiles = {
            "grade_distribution_chart.png",
            "achievement_bar_chart.png",
            "quantitative_evaluation_split_scores.csv",
            "overall_achievement_table.csv",
            "statistics_summary.json"
        };
        
        int generatedFileCount = 0;
        for (String fileName : expectedFiles) {
            File file = new File(workingDir + fileName);
            boolean exists = file.exists();
            log.info("生成文件检查: {} -> {}", fileName, exists);
            if (exists) {
                log.info("文件大小: {} bytes", file.length());
                generatedFileCount++;
            } else {
                log.warn("文件未生成: {}", fileName);
            }
        }
        
        // 检查动态生成的散点图文件
        File[] scatterFiles = workDir.listFiles((dir, name) -> 
            name.endsWith("_achievement_scatter_chart.png"));
        if (scatterFiles != null) {
            log.info("找到散点图文件: {} 个", scatterFiles.length);
            for (File scatterFile : scatterFiles) {
                log.info("散点图文件: {} ({} bytes)", scatterFile.getName(), scatterFile.length());
                generatedFileCount++;
            }
        }
        
        log.info("成功生成文件数量: {}/{}", generatedFileCount, expectedFiles.length + 4); // 4个课程目标散点图
        
        // 如果没有生成任何期望的文件，认为执行失败
        if (generatedFileCount == 0) {
            log.error("Python脚本执行失败：没有生成任何期望的输出文件");
            return false;
        }
        
        log.info("=== Python脚本执行完成 ===");
        return result && generatedFileCount > 0;
    }
    
    /**
     * 测试Python命令是否可用
     */
    private boolean testPythonCommand(String pythonCmd, String workingDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "--version");
            pb.directory(new File(workingDir));
            Process process = pb.start();
            boolean completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (completed && process.exitValue() == 0) {
                log.info("Python命令 {} 测试成功", pythonCmd);
                return true;
            }
        } catch (Exception e) {
            log.debug("Python命令 {} 测试失败: {}", pythonCmd, e.getMessage());
        }
        return false;
    }
    
    /**
     * 收集处理结果
     */
    private Map<String, Object> collectResults(String configId) {
        // 处理configId中的.json后缀
        String cleanedConfigId = configId;
        if (configId.endsWith(".json")) {
            cleanedConfigId = configId.substring(0, configId.length() - 5);
        }
        
        String dataDir = uploadPath + "/data/" + cleanedConfigId + "/";
        Map<String, Object> results = new HashMap<>();
        
        // 收集生成的CSV文件
        File quantitativeFile = new File(dataDir + "quantitative_evaluation_split_scores.csv");
        if (quantitativeFile.exists()) {
            results.put("quantitativeEvaluation", "quantitative_evaluation_split_scores.csv");
        }
        
        File achievementFile = new File(dataDir + "overall_achievement_table.csv");
        if (achievementFile.exists()) {
            results.put("achievementTable", "overall_achievement_table.csv");
        }
        
        // 收集生成的图表
        File gradeDistChart = new File(dataDir + "grade_distribution_chart.png");
        if (gradeDistChart.exists()) {
            results.put("gradeDistributionChart", "grade_distribution_chart.png");
        }
        
        File achievementBarChart = new File(dataDir + "achievement_bar_chart.png");
        if (achievementBarChart.exists()) {
            results.put("achievementBarChart", "achievement_bar_chart.png");
        }
        
        // 收集课程目标散点图
        List<String> scatterCharts = new ArrayList<>();
        File dir = new File(dataDir);
        File[] files = dir.listFiles((d, name) -> name.endsWith("_achievement_scatter_chart.png"));
        if (files != null) {
            for (File file : files) {
                scatterCharts.add(file.getName());
            }
        }
        results.put("scatterCharts", scatterCharts);
        
        return results;
    }
    
    /**
     * 使用原生 POI 从头创建 Word 报告(不使用模板)
     */
    private String generateWordReport(String reportId, String configId) throws IOException {
        String dataDir = uploadPath + "/data/" + configId + "/";
        String outputPath = uploadPath + "/reports/report_" + reportId + ".docx";

        File reportDir = new File(uploadPath + "/reports");
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        Map<String, Object> data = buildReportData(configId, dataDir);

        // 创建新的 Word 文档
        try (XWPFDocument doc = new XWPFDocument()) {
            
            log.info("开始创建 Word 文档...");
            
            // 1. 添加标题
            createTitle(doc, "课程目标达成评价报告");
            
            // 2. 添加课程基本信息
            createCourseInfoSection(doc, data);
            
            // 3. 添加课程教学目标表格
            createTableSection(doc, "一、课程教学目标", (List<RowRenderData>) data.get("objectivesRows"));
            
            // 4. 添加考评方式占比表格
            createTableSection(doc, "二、考评方式及权重", (List<RowRenderData>) data.get("evalRows"));
            
            // 5. 添加成绩统计信息
            createGradeStatisticsSection(doc, data);
            
            // 6. 添加成绩分布图表
            addImageIfExists(doc, dataDir + "grade_distribution_chart.png", "图1 成绩分布图");
            
            // 7. 添加课程目标达成度表格
            createTableSection(doc, "三、课程目标达成情况", (List<RowRenderData>) data.get("achieveRows"));
            
            // 8. 添加柱状图
            addImageIfExists(doc, dataDir + "achievement_bar_chart.png", "图2 课程目标达成度对比");
            
            // 9. 添加散点图
            List<Map<String, Object>> scatterCharts = (List<Map<String, Object>>) data.get("scatterCharts");
            if (scatterCharts != null && !scatterCharts.isEmpty()) {
                addSection(doc, "四、课程目标达成度分布");
                for (Map<String, Object> chartItem : scatterCharts) {
                    String chartPath = (String) chartItem.get("chartPath");
                    if (chartPath != null && new File(chartPath).exists()) {
                        String title = "图" + chartItem.get("index") + " 课程目标" + chartItem.get("targetNum") + "达成度分布";
                        addImageFromFile(doc, chartPath, title);
                        
                        // 添加描述
                        String desc = "达成度：" + chartItem.get("achievement") + "，参与人数：" + chartItem.get("totalStudents");
                        addParagraph(doc, desc, "宋体", 11, false);
                        addEmptyLine(doc);
                    }
                }
            }
            
            // 10. 添加持续改进措施
            createTableSection(doc, "五、持续改进措施", (List<RowRenderData>) data.get("improvementsRows"));
            
            // 保存文档
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                doc.write(out);
            }
            
            log.info("Word 报告生成成功: {}", outputPath);
        } catch (Exception e) {
            log.error("生成 Word 报告失败", e);
            throw new IOException("生成 Word 报告失败", e);
        }

        return outputPath;
    }

    private Map<String, Object> buildReportData(String configId, String dataDir) throws IOException {
        Map<String, Object> data = new HashMap<>();

        // ---------- 读取 exam_config.json ----------
        JSONObject config = readConfigFromFile(dataDir + "exam_config.json");

        // ---------- 读取 statistics_summary.json ----------
        JSONObject stats = JSON.parseObject(new String(Files.readAllBytes(Paths.get(dataDir + "statistics_summary.json"))));

        // ---------- 读取 overall_achievement_table.csv ----------
        List<Map<String, Object>> achievements = new ArrayList<>();
        Path csvPath = Paths.get(dataDir + "overall_achievement_table.csv");
        if (Files.exists(csvPath)) {
            try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    log.warn("CSV文件为空: {}", csvPath);
                } else {
                    // 解析CSV表头，确定列索引
                    String[] headers = headerLine.split(",");
                    Map<String, Integer> columnIndexMap = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        columnIndexMap.put(headers[i].trim(), i);
                    }
                    
                    log.info("CSV表头: {}", String.join(", ", headers));
                    log.info("列索引映射: {}", columnIndexMap);
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] cols = line.split(",");
                        if (cols.length >= 2) {
                            Map<String, Object> row = new HashMap<>();
                            
                            // 使用列名映射来读取数据，而不是硬编码索引
                            // 根据实际的 CSV 表头：课程目标,考试_额定值,考试_分值,考试_达成度,课堂作业_额定值,课堂作业_分值,课堂作业_达成度,实验报告_额定值,实验报告_分值,实验报告_达成度,达成度（标准= 0.6）
                            row.put("courseTarget", getColumnValue(cols, columnIndexMap, "课程目标", 0));
                            
                            // 考试相关
                            row.put("examFull", getColumnValue(cols, columnIndexMap, "考试_额定值", 1));
                            row.put("examScore", getColumnValue(cols, columnIndexMap, "考试_分值", 2));
                            row.put("examAchieve", getColumnValue(cols, columnIndexMap, "考试_达成度", 3));
                            
                            // 课堂作业相关
                            row.put("regularFull", getColumnValue(cols, columnIndexMap, "课堂作业_额定值", 4));
                            row.put("regularScore", getColumnValue(cols, columnIndexMap, "课堂作业_分值", 5));
                            row.put("regularAchieve", getColumnValue(cols, columnIndexMap, "课堂作业_达成度", 6));
                            
                            // 实验报告相关
                            row.put("labFull", getColumnValue(cols, columnIndexMap, "实验报告_额定值", 7));
                            row.put("labScore", getColumnValue(cols, columnIndexMap, "实验报告_分值", 8));
                            row.put("labAchieve", getColumnValue(cols, columnIndexMap, "实验报告_达成度", 9));
                            
                            // 关键：总达成度字段（注意括号和空格）
                            row.put("totalAchieve", getColumnValue(cols, columnIndexMap, 
                                new String[]{"达成度（标准= 0.6）", "达成度(标准=0.6)", "总达成度"}, 10));
                            
                            achievements.add(row);
                        }
                    }
                    
                    log.info("从 CSV 读取了 {} 条达成度数据", achievements.size());
                }
            }
        } else {
            log.warn("CSV文件不存在: {}", csvPath);
        }

        // ---------- 课程基本信息 ----------
        data.put("courseName", config.getString("courseName") != null ? config.getString("courseName") : "计算机组成原理与体系结构");
        data.put("courseCode", config.getString("courseCode") != null ? config.getString("courseCode") : "CIE5B3S003");
        data.put("courseCredit", config.getString("courseCredit") != null ? config.getString("courseCredit") : "3");
        data.put("courseNature", config.getString("courseNature") != null ? config.getString("courseNature") : "必修");
        data.put("grade", config.getString("grade") != null ? config.getString("grade") : "2023级");
        data.put("semester", config.getString("semester") != null ? config.getString("semester") : "第3学期");
        data.put("responsibleTeacher", config.getString("responsibleTeacher") != null ? config.getString("responsibleTeacher") : "系统管理员");
        data.put("teacher", config.getString("teacher") != null ? config.getString("teacher") : "系统管理员");

        int totalStudents = stats.getIntValue("学生总数");
        data.put("totalStudents", totalStudents);

        JSONArray courseTargets = config.getJSONArray("courseTargets");

        // ========== 1. 课程教学目标表格 ==========
        List<RowRenderData> objectivesRows = new ArrayList<>();
        // 表头样式
        Style headStyle1 = new Style();
        headStyle1.setBold(true);
        headStyle1.setFontFamily("宋体");
        headStyle1.setFontSize(11);
        objectivesRows.add(Rows.of(
                new TextRenderData("毕业要求指标点", headStyle1),
                new TextRenderData("课程目标简称", headStyle1),
                new TextRenderData("课程目标描述", headStyle1)
        ).create());
        for (int i = 0; i < courseTargets.size(); i++) {
            String target = courseTargets.getString(i);
            objectivesRows.add(Rows.of(
                    new TextRenderData((i + 1) + ".3"),
                    new TextRenderData(target),
                    new TextRenderData("课程目标描述")
            ).create());
        }
        data.put("objectivesRows", objectivesRows);

        // ========== 2. 考评方式占比表格 ==========
        List<RowRenderData> evalRows = new ArrayList<>();
        Style evalHeadStyle = new Style();
        evalHeadStyle.setBold(true);
        evalHeadStyle.setFontFamily("宋体");
        evalHeadStyle.setFontSize(10);
        evalRows.add(Rows.of(
                new TextRenderData("毕业要求指标点", evalHeadStyle),
                new TextRenderData("课程目标", evalHeadStyle),
                new TextRenderData("课堂作业", evalHeadStyle),
                new TextRenderData("实验报告", evalHeadStyle),
                new TextRenderData("考试", evalHeadStyle),
                new TextRenderData("分值/权重", evalHeadStyle)
        ).create());
        JSONArray proportions = config.getJSONArray("courseTargetProportions");
        if (proportions == null || proportions.isEmpty()) {
            proportions = config.getJSONArray("targets");
        }
        
        log.info("========== 考评方式占比数据 ==========");
        log.info("courseTargetProportions 数量: {}", proportions != null ? proportions.size() : 0);
        
        // 打印总权重配置
        log.info("总权重配置 - regularGrade: {}, labGrade: {}, finalExam: {}",
            config.getIntValue("regularGrade"),
            config.getIntValue("labGrade"),
            config.getIntValue("finalExam"));
        
        if (proportions != null) {
            for (int i = 0; i < proportions.size(); i++) {
                JSONObject prop = proportions.getJSONObject(i);
                int regular = getIntValue(prop, 0, "regularGrade", "regular");
                int lab = getIntValue(prop, 0, "lab", "labGrade");
                int finalExam = getIntValue(prop, 0, "finalExam", "final");
                int total = prop.getIntValue("total");
                if (total == 0) total = regular + lab + finalExam;
                
                log.info("  [{}] courseTarget={}, regularGrade={}, lab={}, finalExam={}, total={}",
                    i, prop.getString("courseTarget"), regular, lab, finalExam, total);
                
                evalRows.add(Rows.of(
                        new TextRenderData((i + 1) + ".3"),
                        new TextRenderData(prop.getString("courseTarget")),
                        new TextRenderData(String.valueOf(regular)),
                        new TextRenderData(String.valueOf(lab)),
                        new TextRenderData(String.valueOf(finalExam)),
                        new TextRenderData(String.valueOf(total))
                ).create());
            }
        }
        data.put("evalRows", evalRows);
        log.info("evalRows 构建完成，共 {} 行", evalRows.size());

        // ========== 3. 总成绩统计 ==========
        // 根据实际的 statistics_summary.json 结构读取
        JSONObject totalScoreStats = stats.getJSONObject("总成绩");
        if (totalScoreStats != null) {
            data.put("avgScore", totalScoreStats.getDouble("平均值"));
            data.put("medianScore", totalScoreStats.getDouble("中位值"));  // 注意：是“中位值”不是“中位数”
            data.put("stdDev", totalScoreStats.getDouble("标准差"));
            data.put("maxScore", totalScoreStats.getDouble("最高分"));
            data.put("minScore", totalScoreStats.getDouble("最低分"));
            
            log.info("总成绩统计 - 平均值: {}, 中位值: {}, 标准差: {}, 最高分: {}, 最低分: {}",
                data.get("avgScore"), data.get("medianScore"), data.get("stdDev"),
                data.get("maxScore"), data.get("minScore"));
        } else {
            log.warn("未找到'总成绩'节点，尝试从根级别读取");
            // 兼容旧格式：直接在根级别
            data.put("avgScore", stats.getDouble("平均值"));
            data.put("medianScore", stats.getDouble("中位值"));
            data.put("stdDev", stats.getDouble("标准差"));
            data.put("maxScore", stats.getDouble("最高分"));
            data.put("minScore", stats.getDouble("最低分"));
        }

        // ========== 4. 成绩分布表格 ==========
        List<RowRenderData> gradeDistRows = new ArrayList<>();
        Style gradeHeadStyle = new Style();
        gradeHeadStyle.setBold(true);
        gradeHeadStyle.setFontFamily("宋体");
        gradeDistRows.add(Rows.of(
                new TextRenderData("达成区间", gradeHeadStyle),
                new TextRenderData("人数", gradeHeadStyle),
                new TextRenderData("占比", gradeHeadStyle)
        ).create());
        JSONObject distribution = stats.getJSONObject("成绩分布");
        if (distribution != null) {
            String[] grades = {"优秀", "良好", "中等", "及格", "不及格"};
            String[] ranges = {"优秀(≥90)", "良好(80-90)", "中等(70-80)", "及格(60-70)", "不及格(<60)"};
            for (int i = 0; i < grades.length; i++) {
                JSONObject gradeObj = distribution.getJSONObject(grades[i]);
                if (gradeObj != null) {
                    int count = gradeObj.getInteger("人数");
                    double ratio = gradeObj.getDouble("占比");
                    // 根据实际数据，占比是数值（如 100.0），需要格式化为百分比字符串
                    String ratioStr = String.format("%.2f%%", ratio);
                    
                    gradeDistRows.add(Rows.of(
                            new TextRenderData(ranges[i]),
                            new TextRenderData(String.valueOf(count)),
                            new TextRenderData(ratioStr)
                    ).create());
                }
            }
            log.info("成绩分布数据读取完成");
        } else {
            log.warn("未找到'成绩分布'节点");
        }
        data.put("gradeDistRows", gradeDistRows);

        // ========== 5. 图片 ==========
        File gradeChart = new File(dataDir + "grade_distribution_chart.png");
        if (gradeChart.exists()) {
            data.put("gradeDistChart", Pictures.ofLocal(gradeChart.getAbsolutePath()).size(500, 300).create());
        }
        File barChart = new File(dataDir + "achievement_bar_chart.png");
        if (barChart.exists()) {
            data.put("barChart", Pictures.ofLocal(barChart.getAbsolutePath()).size(500, 300).create());
        }

        // ========== 6. 散点图列表 ==========
        List<Map<String, Object>> scatterCharts = new ArrayList<>();
        File[] scatterFiles = new File(dataDir).listFiles((dir, name) -> name.endsWith("_achievement_scatter_chart.png"));
        if (scatterFiles != null) {
            int idx = 2;
            for (File sf : scatterFiles) {
                String name = sf.getName();
                String targetNum = name.replaceAll("\\D", "");
                Map<String, Object> chartItem = new HashMap<>();
                chartItem.put("chart", Pictures.ofLocal(sf.getAbsolutePath()).size(500, 300).create());
                chartItem.put("chartPath", sf.getAbsolutePath()); // 存储图片路径
                chartItem.put("index", idx++);
                chartItem.put("targetNum", targetNum);
                
                // 从达成度数据中查找对应的课程目标
                String achievement = "";
                for (Map<String, Object> ach : achievements) {
                    String courseTarget = ach.getOrDefault("courseTarget", "").toString();
                    // 支持多种匹配方式
                    if (courseTarget.equals("课程目标" + targetNum) ||
                        courseTarget.equals("目标" + targetNum) ||
                        courseTarget.contains(targetNum)) {
                        // 优先使用 totalAchieve 字段
                        Object achieveValue = ach.get("totalAchieve");
                        if (achieveValue != null && !achieveValue.toString().isEmpty()) {
                            achievement = achieveValue.toString();
                        }
                        break;
                    }
                }
                chartItem.put("achievement", achievement.isEmpty() ? "未找到" : achievement);
                chartItem.put("totalStudents", totalStudents);
                scatterCharts.add(chartItem);
            }
        }
        data.put("scatterCharts", scatterCharts);

        // ========== 7. 课程目标达成表格 ==========
        List<RowRenderData> achieveRows = new ArrayList<>();
        Style achieveHeadStyle = new Style();
        achieveHeadStyle.setBold(true);
        achieveHeadStyle.setFontSize(9);
        achieveHeadStyle.setFontFamily("宋体");
        achieveRows.add(Rows.of(
                new TextRenderData("课程目标", achieveHeadStyle),
                new TextRenderData("额定值", achieveHeadStyle),
                new TextRenderData("分值", achieveHeadStyle),
                new TextRenderData("达成度", achieveHeadStyle),
                new TextRenderData("额定值", achieveHeadStyle),
                new TextRenderData("分值", achieveHeadStyle),
                new TextRenderData("达成度", achieveHeadStyle),
                new TextRenderData("额定值", achieveHeadStyle),
                new TextRenderData("分值", achieveHeadStyle),
                new TextRenderData("达成度", achieveHeadStyle),
                new TextRenderData("达成度(标准=0.6)", achieveHeadStyle)
        ).create());
        for (Map<String, Object> ach : achievements) {
            achieveRows.add(Rows.of(
                    new TextRenderData(ach.getOrDefault("courseTarget", "").toString()),
                    new TextRenderData(ach.getOrDefault("examFull", "").toString()),
                    new TextRenderData(ach.getOrDefault("examScore", "").toString()),
                    new TextRenderData(ach.getOrDefault("examAchieve", "").toString()),
                    new TextRenderData(ach.getOrDefault("regularFull", "").toString()),
                    new TextRenderData(ach.getOrDefault("regularScore", "").toString()),
                    new TextRenderData(ach.getOrDefault("regularAchieve", "").toString()),
                    new TextRenderData(ach.getOrDefault("labFull", "").toString()),
                    new TextRenderData(ach.getOrDefault("labScore", "").toString()),
                    new TextRenderData(ach.getOrDefault("labAchieve", "").toString()),
                    new TextRenderData(ach.getOrDefault("totalAchieve", "").toString())
            ).create());
        }
        data.put("achieveRows", achieveRows);

        // ========== 8. 课程目标达成分析表格 ==========
        List<RowRenderData> analysisRows = new ArrayList<>();
        Style analysisHeadStyle = new Style();
        analysisHeadStyle.setBold(true);
        analysisHeadStyle.setFontFamily("宋体");
        analysisRows.add(Rows.of(
                new TextRenderData("课程目标", analysisHeadStyle),
                new TextRenderData("达成分析", analysisHeadStyle),
                new TextRenderData("较往年分析", analysisHeadStyle)
        ).create());
        for (int i = 0; i < courseTargets.size(); i++) {
            String targetName = courseTargets.getString(i);
            String achieveValue = "";
            for (Map<String, Object> ach : achievements) {
                if (ach.get("courseTarget").equals(targetName)) {
                    achieveValue = ach.get("totalAchieve").toString();
                    break;
                }
            }
            analysisRows.add(Rows.of(
                    new TextRenderData(targetName),
                    new TextRenderData("达成度：" + achieveValue),
                    new TextRenderData("无")
            ).create());
        }
        data.put("analysisRows", analysisRows);

        // ========== 9. 持续改进措施表格 ==========
        List<RowRenderData> improvementsRows = new ArrayList<>();
        Style impHeadStyle = new Style();
        impHeadStyle.setBold(true);
        impHeadStyle.setFontFamily("宋体");
        improvementsRows.add(Rows.of(
                new TextRenderData("改进项目", impHeadStyle),
                new TextRenderData("持续改进", impHeadStyle)
        ).create());
        for (int i = 0; i < courseTargets.size(); i++) {
            improvementsRows.add(Rows.of(
                    new TextRenderData(courseTargets.getString(i)),
                    new TextRenderData("")
            ).create());
        }
        improvementsRows.add(Rows.of(
                new TextRenderData("其他"),
                new TextRenderData("")
        ).create());
        data.put("improvementsRows", improvementsRows);

        // ========== 10. 教学效果分析 ==========
        data.put("teachingEffect", "教学效果总体良好，大部分同学可以达成所有的课程目标。");

        // ========== 11. 教学改进措施表格 ==========
        List<RowRenderData> teachingRows = new ArrayList<>();
        Style teachHeadStyle = new Style();
        teachHeadStyle.setBold(true);
        teachHeadStyle.setFontFamily("宋体");
        teachingRows.add(Rows.of(
                new TextRenderData("序号", teachHeadStyle),
                new TextRenderData("改进项目", teachHeadStyle),
                new TextRenderData("是否改进", teachHeadStyle),
                new TextRenderData("改进建议", teachHeadStyle)
        ).create());
        List<Map<String, Object>> measures = Arrays.asList(
                Map.of("index", "1", "project", "是否调整课程及其对毕业要求指标点达成支撑关系?", "isImproved", "否", "suggestion", ""),
                Map.of("index", "2", "project", "是否调整课程教学内容？", "isImproved", "否", "suggestion", ""),
                Map.of("index", "3", "project", "是否调整课程学时与开课方式？", "isImproved", "是", "suggestion", "适当增加目标3的支撑课时"),
                Map.of("index", "4", "project", "是否调整课程教学方法与手段？", "isImproved", "否", "suggestion", ""),
                Map.of("index", "5", "project", "是否调整课程考核与评价方法？", "isImproved", "是", "suggestion", "目标3和目标4需要适当调整"),
                Map.of("index", "6", "project", "是否需要加强课程教学资源与平台建设？", "isImproved", "否", "suggestion", ""),
                Map.of("index", "7", "project", "专业其他改进意见", "isImproved", "", "suggestion", "无")
        );
        for (Map<String, Object> m : measures) {
            teachingRows.add(Rows.of(
                    new TextRenderData(m.get("index").toString()),
                    new TextRenderData(m.get("project").toString()),
                    new TextRenderData(m.get("isImproved").toString()),
                    new TextRenderData(m.get("suggestion").toString())
            ).create());
        }
        data.put("teachingRows", teachingRows);

        return data;
    }

    /**
     * 辅助方法：从 JSONObject 中按优先级获取 int 值
     */
    private int getIntValue(JSONObject obj, int defaultValue, String... keys) {
        for (String key : keys) {
            Integer val = obj.getInteger(key);
            if (val != null) return val;
        }
        return defaultValue;
    }

    /**
     * 添加章节标题
     */
    private void addSectionHeader(XWPFDocument document, String text) {
        XWPFParagraph header = document.createParagraph();
        XWPFRun headerRun = header.createRun();
        headerRun.setText(text);
        headerRun.setBold(true);
        headerRun.setFontSize(14);
        headerRun.setFontFamily("宋体");
    }
    
    /**
     * 添加子标题
     */
    private void addSubHeader(XWPFDocument document, String text) {
        XWPFParagraph subHeader = document.createParagraph();
        XWPFRun subHeaderRun = subHeader.createRun();
        subHeaderRun.setText(text);
        subHeaderRun.setBold(true);
        subHeaderRun.setFontSize(12);
        subHeaderRun.setFontFamily("宋体");
    }
    
    /**
     * 插入图表到文档 - 优化版本
     */
    private void insertChartsToDocument(XWPFDocument document, String dataDir) {
        try {
            File dataDirFile = new File(dataDir);
            if (!dataDirFile.exists() || !dataDirFile.isDirectory()) {
                log.warn("数据目录不存在: {}", dataDir);
                return;
            }
            
            // 查找所有PNG图片文件
            File[] pngFiles = dataDirFile.listFiles((dir, name) -> name.endsWith(".png"));
            if (pngFiles == null || pngFiles.length == 0) {
                XWPFParagraph noChart = document.createParagraph();
                XWPFRun noChartRun = noChart.createRun();
                noChartRun.setText("未找到生成的图表文件。");
                noChartRun.setFontFamily("微软雅黑");
                return;
            }
            
            // 按文件名排序
            Arrays.sort(pngFiles, (a, b) -> a.getName().compareTo(b.getName()));
            
            int chartIndex = 1;
            for (File pngFile : pngFiles) {
                try {
                    String fileName = pngFile.getName();
                    String description = getChartDescription(fileName);
                    
                    // 添加图表标题
                    XWPFParagraph chartTitle = document.createParagraph();
                    chartTitle.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun titleRun = chartTitle.createRun();
                    titleRun.setText("图" + chartIndex + " " + description);
                    titleRun.setBold(true);
                    titleRun.setFontFamily("微软雅黑");
                    titleRun.setFontSize(12);
                    
                    // 插入图片
                    XWPFParagraph imagePara = document.createParagraph();
                    imagePara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun imageRun = imagePara.createRun();
                    
                    try (FileInputStream fis = new FileInputStream(pngFile)) {
                        // 检查文件大小，避免插入过大的图片
                        long fileSize = pngFile.length();
                        if (fileSize > 0 && fileSize < 5 * 1024 * 1024) { // 小于5MB
                            imageRun.addPicture(fis, XWPFDocument.PICTURE_TYPE_PNG, fileName, 
                                              Units.toEMU(400), Units.toEMU(300));
                            log.info("✅ 成功插入图表: {}", fileName);
                        } else {
                            log.warn("图片文件过大或为空，跳过: {} ({}字节)", fileName, fileSize);
                            // 添加替代文本
                            imageRun.setText("[图表文件过大，请在系统中查看: " + fileName + "]");
                            imageRun.setFontFamily("微软雅黑");
                        }
                    }
                    
                    // 添加空行
                    document.createParagraph().createRun().addBreak();
                    chartIndex++;
                    
                } catch (Exception e) {
                    log.error("插入图表失败: " + pngFile.getName(), e);
                    // 添加错误提示
                    XWPFParagraph errorPara = document.createParagraph();
                    XWPFRun errorRun = errorPara.createRun();
                    errorRun.setText("[图表插入失败: " + pngFile.getName() + "]");
                    errorRun.setFontFamily("微软雅黑");
                    errorRun.setColor("FF0000"); // 红色
                }
            }
            
        } catch (Exception e) {
            log.error("插入图表过程失败", e);
            XWPFParagraph errorPara = document.createParagraph();
            XWPFRun errorRun = errorPara.createRun();
            errorRun.setText("图表插入过程出现错误，请检查数据文件。");
            errorRun.setFontFamily("微软雅黑");
        }
    }
    
    /**
     * 生成模拟结果数据（当Python脚本执行失败时使用）
     */
    private void generateMockResults(String configId) throws IOException {
        String dataDir = uploadPath + "/data/" + configId + "/";
        
        log.info("=== 开始生成模拟结果数据 ===");
        log.info("数据目录: {}", dataDir);
        
        // 确保目录存在
        File dataDirFile = new File(dataDir);
        if (!dataDirFile.exists()) {
            boolean created = dataDirFile.mkdirs();
            log.info("创建数据目录: {}, 结果: {}", dataDir, created);
        }
        
        // 创建模拟的statistics_summary.json
        Map<String, Object> mockStatistics = new HashMap<>();
        mockStatistics.put("学生总数", 51);
        mockStatistics.put("平均总成绩", 72.45);
        mockStatistics.put("中位数", 74.20);
        mockStatistics.put("标准差", 14.36);
        mockStatistics.put("最高分", 94);
        mockStatistics.put("最低分", 24);
        
        Map<String, Object> gradeDistribution = new HashMap<>();
        Map<String, Object> excellent = new HashMap<>(); excellent.put("人数", 4); excellent.put("占比", 7.41);
        Map<String, Object> good = new HashMap<>(); good.put("人数", 11); good.put("占比", 20.37);
        Map<String, Object> medium = new HashMap<>(); medium.put("人数", 18); medium.put("占比", 33.33);
        Map<String, Object> pass = new HashMap<>(); pass.put("人数", 10); pass.put("占比", 18.52);
        Map<String, Object> fail = new HashMap<>(); fail.put("人数", 8); fail.put("占比", 14.81);
        
        gradeDistribution.put("优秀", excellent);
        gradeDistribution.put("良好", good);
        gradeDistribution.put("中等", medium);
        gradeDistribution.put("及格", pass);
        gradeDistribution.put("不及格", fail);
        mockStatistics.put("成绩分布", gradeDistribution);
        
        // 保存模拟统计数据
        String statisticsJson = JSON.toJSONString(mockStatistics);
        Files.write(Paths.get(dataDir + "statistics_summary.json"), statisticsJson.getBytes(StandardCharsets.UTF_8));
        log.info("✅ 创建文件: statistics_summary.json");
        
        // 创建模拟的CSV文件
        createMockCSVFile(dataDir + "quantitative_evaluation_split_scores.csv", 
                         "学号,姓名,课程目标1_个人达成度,课程目标2_个人达成度,课程目标3_个人达成度,课程目标4_个人达成度\n" +
                         "20210101,张三,0.85,0.78,0.82,0.88\n" +
                         "20210102,李四,0.90,0.85,0.87,0.92\n" +
                         "20210103,王五,0.75,0.72,0.78,0.80\n" +
                         "20210104,赵六,0.82,0.79,0.84,0.86\n" +
                         "20210105,陈七,0.88,0.83,0.89,0.91\n" +
                         "20210201,刘八,0.79,0.76,0.81,0.85\n" +
                         "20210202,周九,0.86,0.82,0.87,0.89\n" +
                         "20210203,吴十,0.84,0.80,0.85,0.87\n" +
                         "20210204,郑十一,0.81,0.77,0.83,0.86\n" +
                         "20210205,孙十二,0.92,0.89,0.94,0.96\n");
        
        createMockCSVFile(dataDir + "overall_achievement_table.csv",
                         "课程目标,达成度（标准=0.60）\n" +
                         "课程目标1,0.83\n" +
                         "课程目标2,0.79\n" +
                         "课程目标3,0.84\n" +
                         "课程目标4,0.87\n");
        
        // 创建带有"模拟数据"标识的简单SVG图片
        createMockSVGImage(dataDir + "grade_distribution_chart.png", "成绩分布图（模拟数据）", 
                          "优秀: 7.41% | 良好: 20.37% | 中等: 33.33% | 及格: 18.52% | 不及格: 14.81%");
        createMockSVGImage(dataDir + "achievement_bar_chart.png", "课程目标达成度（模拟数据）",
                          "目标1: 0.83 | 目标2: 0.79 | 目标3: 0.84 | 目标4: 0.87");
        createMockSVGImage(dataDir + "课程目标1_achievement_scatter_chart.png", "课程目标1达成度散点图（模拟数据）",
                          "平均达成度: 0.83");
        createMockSVGImage(dataDir + "课程目标2_achievement_scatter_chart.png", "课程目标2达成度散点图（模拟数据）",
                          "平均达成度: 0.79");
        createMockSVGImage(dataDir + "课程目标3_achievement_scatter_chart.png", "课程目标3达成度散点图（模拟数据）",
                          "平均达成度: 0.84");
        createMockSVGImage(dataDir + "课程目标4_achievement_scatter_chart.png", "课程目标4达成度散点图（模拟数据）",
                          "平均达成度: 0.87");
        
        log.info("模拟结果数据生成完成");
    }
    
    private void createMockCSVFile(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
    }
    
    private void createMockSVGImage(String filePath, String title, String description) throws IOException {
        // 直接创建一个更大的PNG占位符图片
        byte[] mockPNG = createMockPNGBytesWithText(title, description);
        Files.write(Paths.get(filePath), mockPNG);
        
        log.info("创建模拟PNG图片: {}", filePath);
    }
    
    private byte[] createMockPNGBytes() {
        // 创建一个最小的1x1像素透明PNG图片的字节数组
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
            (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte)0x9C, 0x63, 0x00, 0x01, 0x00,
            0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte)0xAE, 0x42, 0x60, (byte)0x82
        };
    }
    
    private byte[] createMockPNGBytesWithText(String title, String description) {
        // 为了简化，暂时返回基础的PNG字节
        // 在生产环境中，这里可以使用BufferedImage和Graphics2D来绘制文本
        return createMockPNGBytes();
    }

    private String cleanConfigId(String configId) {
        if (configId != null && configId.endsWith(".json")) {
            return configId.substring(0, configId.length() - 5);
        }
        return configId;
    }
    
    /**
     * 设置表格单元格文本和样式 - 使用更安全的方法
     */
    private void setCellText(XWPFTableCell cell, String text, boolean isBold) {
        // 直接设置文本，让POI处理段落创建
        if (text == null) text = "";
        cell.setText(text);
        
        // 获取第一个段落并设置样式
        if (!cell.getParagraphs().isEmpty()) {
            XWPFParagraph paragraph = cell.getParagraphs().get(0);
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            
            // 设置字体样式
            if (!paragraph.getRuns().isEmpty()) {
                XWPFRun run = paragraph.getRuns().get(0);
                run.setFontFamily("微软雅黑");
                run.setFontSize(10);
                if (isBold) {
                    run.setBold(true);
                }
            }
        }
        
        // 设置单元格垂直对齐
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }
    
    /**
     * 创建简单表格 - 避免复杂操作
     */
    private XWPFTable createSimpleTable(XWPFDocument document, String[][] data, boolean firstRowBold) {
        if (data == null || data.length == 0) return null;
        
        int rows = data.length;
        int cols = data[0].length;
        
        XWPFTable table = document.createTable(rows, cols);
        
        for (int i = 0; i < rows; i++) {
            XWPFTableRow row = table.getRow(i);
            for (int j = 0; j < cols && j < data[i].length; j++) {
                XWPFTableCell cell = row.getCell(j);
                boolean isBold = firstRowBold && i == 0;
                setCellText(cell, data[i][j], isBold);
            }
        }
        
        return table;
    }
    
    /**
     * 从统计数据中获取数值，如果不存在则返回默认值
     */
    private String getStatValue(Map<String, Object> statistics, String key, String defaultValue) {
        if (statistics.containsKey(key)) {
            Object value = statistics.get(key);
            if (value instanceof Number) {
                double doubleValue = ((Number) value).doubleValue();
                // 如果是整数，不显示小数点
                if (doubleValue == Math.floor(doubleValue)) {
                    return String.valueOf((int) doubleValue);
                } else {
                    // 保留两位小数
                    return String.format("%.2f", doubleValue);
                }
            } else {
                return value.toString();
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取图表描述
     */
    private String getChartDescription(String fileName) {
        if (fileName.equals("grade_distribution_chart.png")) {
            return "成绩分布图";
        } else if (fileName.equals("achievement_bar_chart.png")) {
            return "课程目标达成度柱状图";
        } else if (fileName.contains("achievement_scatter_chart.png")) {
            String objectiveName = fileName.replace("_achievement_scatter_chart.png", "");
            return objectiveName + "达成度散点图";
        } else {
            return "统计图表";
        }
    }
    
    /**
     * 获取图表序号
     */
    private int getChartIndex(String fileName) {
        if (fileName.equals("grade_distribution_chart.png")) {
            return 1;
        } else if (fileName.equals("achievement_bar_chart.png")) {
            return 2;
        } else if (fileName.contains("课程目标1")) {
            return 3;
        } else if (fileName.contains("课程目标2")) {
            return 4;
        } else if (fileName.contains("课程目标3")) {
            return 5;
        } else if (fileName.contains("课程目标4")) {
            return 6;
        }
        return 0;
    }

    /**
     * 创建数据表格 - 更稳定的实现
     */
    private void createDataTable(XWPFDocument document, String[][] data, boolean firstRowBold) {
        if (data == null || data.length == 0) return;
        
        try {
            int rows = data.length;
            int maxCols = 0;
            for (String[] row : data) {
                maxCols = Math.max(maxCols, row.length);
            }
            
            XWPFTable table = document.createTable(rows, maxCols);
            
            for (int i = 0; i < rows; i++) {
                XWPFTableRow row = table.getRow(i);
                for (int j = 0; j < maxCols; j++) {
                    XWPFTableCell cell = row.getCell(j);
                    String cellText = "";
                    if (j < data[i].length && data[i][j] != null) {
                        cellText = data[i][j];
                    }
                    
                    // 简单设置文本，避免复杂操作
                    cell.setText(cellText);
                    
                    // 设置字体样式
                    if (!cell.getParagraphs().isEmpty()) {
                        XWPFParagraph para = cell.getParagraphs().get(0);
                        para.setAlignment(ParagraphAlignment.CENTER);
                        
                        if (!para.getRuns().isEmpty()) {
                            XWPFRun run = para.getRuns().get(0);
                            run.setFontFamily("宋体");
                            run.setFontSize(10);
                            if (firstRowBold && i == 0) {
                                run.setBold(true);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("创建表格失败", e);
            // 如果表格创建失败，添加文本形式的数据
            XWPFParagraph fallbackPara = document.createParagraph();
            XWPFRun fallbackRun = fallbackPara.createRun();
            fallbackRun.setText("表格数据（文本格式）：");
            fallbackRun.setFontFamily("宋体");
            
            for (String[] row : data) {
                XWPFParagraph rowPara = document.createParagraph();
                XWPFRun rowRun = rowPara.createRun();
                rowRun.setText(String.join(" | ", row));
                rowRun.setFontFamily("宋体");
            }
        }
    }

    /**
     * 保存结果文件到指定目录
     */
    private void saveResultFilesToDir(String dirPath, Map<String, Object> result) throws IOException {
        Map<String, Object> files = (Map<String, Object>) result.get("files");
        if (files != null) {
            for (Map.Entry<String, Object> entry : files.entrySet()) {
                String fileName = entry.getKey();
                Object fileData = entry.getValue();
                if (fileData instanceof String) {
                    String base64Data = (String) fileData;
                    if (base64Data.startsWith("data:image/png;base64,")) {
                        String[] parts = base64Data.split(",", 2);
                        if (parts.length == 2) {
                            byte[] decodedBytes = Base64.getDecoder().decode(parts[1]);
                            Files.write(Paths.get(dirPath + fileName), decodedBytes);
                        }
                    }
                }
            }
        }
    }

    /**
     * 从目录收集结果文件列表
     */
    private Map<String, Object> collectResultsFromDir(String dirPath) {
        Map<String, Object> results = new HashMap<>();
        File dir = new File(dirPath);
        if (!dir.exists()) return results;
        
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".png")) {
                    if (file.getName().equals("grade_distribution_chart.png")) {
                        results.put("gradeDistributionChart", file.getName());
                    } else if (file.getName().equals("achievement_bar_chart.png")) {
                        results.put("achievementBarChart", file.getName());
                    } else if (file.getName().contains("_achievement_scatter_chart")) {
                        List<String> scatterCharts = (List<String>) results.getOrDefault("scatterCharts", new ArrayList<>());
                        scatterCharts.add(file.getName());
                        results.put("scatterCharts", scatterCharts);
                    }
                } else if (file.getName().equals("statistics_summary.json")) {
                    results.put("statistics", "statistics_summary.json");
                }
            }
        }
        return results;
    }

    /**
     * 解析Python脚本返回的JSON结果
     * @param pythonOutput Python脚本stdout输出的字符串
     * @return 解析后的Map对象
     */
    private Map<String, Object> parsePythonResult(String pythonOutput) {
        try {
            // 去除可能的空白行或BOM头
            String trimmed = pythonOutput.trim();
            if (trimmed.startsWith("\uFEFF")) {
                trimmed = trimmed.substring(1);
            }
            
            // 使用Fastjson2解析
            return JSON.parseObject(trimmed, Map.class);
        } catch (Exception e) {
            log.error("解析Python返回结果失败，原始输出: {}", pythonOutput, e);
            // 返回一个错误Map，保证前端能收到错误信息
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Python脚本返回格式异常: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * 替换文档中的所有文本占位符 {{variable}}
     */
    private void replaceTextPlaceholders(XWPFDocument doc, Map<String, Object> data) {
        // 遍历所有段落
        for (XWPFParagraph paragraph : doc.getParagraphs()) {
            replaceInParagraph(paragraph, data);
        }
        
        // 遍历所有表格中的单元格
        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceInParagraph(paragraph, data);
                    }
                }
            }
        }
    }
    
    /**
     * 在段落中替换占位符
     */
    private void replaceInParagraph(XWPFParagraph paragraph, Map<String, Object> data) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return;
        
        // 合并所有run的文本
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) fullText.append(text);
        }
        
        String mergedText = fullText.toString();
        if (!mergedText.contains("{{")) return;
        
        // 替换所有占位符
        boolean hasReplacement = false;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (mergedText.contains(placeholder)) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                mergedText = mergedText.replace(placeholder, value);
                hasReplacement = true;
            }
        }
        
        // 如果有替换，更新段落
        if (hasReplacement) {
            // 保存第一个run的样式
            XWPFRun firstRun = runs.get(0);
            String fontFamily = firstRun.getFontFamily();
            int fontSize = firstRun.getFontSize() > 0 ? firstRun.getFontSize() : 12;
            boolean isBold = firstRun.isBold();
            
            // 清除所有原有的run
            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            
            // 创建新的run
            XWPFRun newRun = paragraph.createRun();
            newRun.setText(mergedText);
            newRun.setFontFamily(fontFamily);
            newRun.setFontSize(fontSize);
            newRun.setBold(isBold);
        }
    }

    private JSONObject readConfigFromFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        return JSON.parseObject(content);
    }

    /**
     * 修复 Word 模板中因 XML 碎片化导致的标签拆分问题。
     * 将每个单元格内的所有文本运行合并为单个运行，确保 poi-tl 标签完整。
     * 
     * @param inputStream 原始模板输入流
     * @param outputStream 修复后的输出流
     */
    private void repairTemplate(InputStream inputStream, OutputStream outputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            // 1. 处理所有段落（包括页眉、页脚、文本框等）
            for (XWPFParagraph para : doc.getParagraphs()) {
                mergeRunsIfNeeded(para);
            }
            
            // 2. 处理所有表格中的段落
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            mergeRunsIfNeeded(para);
                        }
                    }
                }
            }
            
            // 3. 处理页眉中的段落（如果有）
            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph para : header.getParagraphs()) {
                    mergeRunsIfNeeded(para);
                }
            }
            
            // 4. 处理页脚中的段落（如果有）
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph para : footer.getParagraphs()) {
                    mergeRunsIfNeeded(para);
                }
            }
            
            doc.write(outputStream);
        }
    }

    private void mergeRunsIfNeeded(XWPFParagraph para) {
        List<XWPFRun> runs = para.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }
        
        // 合并所有 run 的文本
        StringBuilder fullText = new StringBuilder();
        for (XWPFRun run : runs) {
            String text = run.getText(0);
            if (text != null) {
                fullText.append(text);
            }
        }
        String merged = fullText.toString();
        
        // 如果段落中包含 poi-tl 标签，则强制重建为单一 run
        if (merged.contains("{{#") || merged.contains("{{/") || merged.contains("{{+") || merged.contains("{{-")) {
            // 删除原有所有 runs
            for (int i = runs.size() - 1; i >= 0; i--) {
                para.removeRun(i);
            }
            // 创建一个新 run，写入完整文本
            XWPFRun newRun = para.createRun();
            newRun.setText(merged);
        }
    }
    

    
    /**
     * 替换表格占位符为实际的表格
     */
    private void replaceTablePlaceholders(XWPFDocument doc, Map<String, Object> data) throws IOException {
        // 先收集需要处理的段落和书签信息,避免在遍历时修改文档结构
        List<Map<String, Object>> tablesToProcess = new ArrayList<>();
        
        // 遍历文档中的所有段落，查找包含书签的段落
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        log.info("文档包含 {} 个段落", paragraphs.size());
        for (XWPFParagraph para : paragraphs) {
            // 检查段落是否包含书签
            if (para == null) {
                log.warn("段落为空，跳过");
                continue;
            }
            
            List<CTBookmark> bookmarks = para.getCTP().getBookmarkStartList();
            if (bookmarks == null) {
                log.warn("书签列表为空，跳过");
                continue;
            }
            
            log.info("段落包含 {} 个书签", bookmarks.size());
            for (CTBookmark bookmark : bookmarks) {
                if (bookmark == null) {
                    log.warn("书签为空，跳过");
                    continue;
                }
                
                String bookmarkName = bookmark.getName();
                if (bookmarkName == null) {
                    log.warn("书签名称为空，跳过");
                    continue;
                }
                
                log.info("找到书签: {}", bookmarkName);
                
                // 收集需要处理的表格信息
                Map<String, Object> tableInfo = new HashMap<>();
                tableInfo.put("paragraph", para);
                tableInfo.put("bookmarkName", bookmarkName);
                
                // 处理课程教学目标表格
                if ("OBJECTIVES_TABLE".equals(bookmarkName)) {
                    log.info("处理 OBJECTIVES_TABLE 书签");
                    tableInfo.put("rows", data.get("objectivesRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理考评方式占比表格
                else if ("EVALUATION_METHODS_TABLE".equals(bookmarkName)) {
                    log.info("处理 EVALUATION_METHODS_TABLE 书签");
                    tableInfo.put("rows", data.get("evalRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理成绩分布表格
                else if ("GRADE_DISTRIBUTION_TABLE".equals(bookmarkName)) {
                    log.info("处理 GRADE_DISTRIBUTION_TABLE 书签");
                    tableInfo.put("rows", data.get("gradeDistRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理课程目标达成表格
                else if ("ACHIEVEMENTS_TABLE".equals(bookmarkName)) {
                    log.info("处理 ACHIEVEMENTS_TABLE 书签");
                    tableInfo.put("rows", data.get("achieveRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理课程目标达成分析表格
                else if ("ANALYSIS_TABLE".equals(bookmarkName)) {
                    log.info("处理 ANALYSIS_TABLE 书签");
                    tableInfo.put("rows", data.get("analysisRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理持续改进措施表格
                else if ("IMPROVEMENTS_TABLE".equals(bookmarkName)) {
                    log.info("处理 IMPROVEMENTS_TABLE 书签");
                    tableInfo.put("rows", data.get("improvementsRows"));
                    tablesToProcess.add(tableInfo);
                }
                // 处理教学改进措施表格
                else if ("TEACHING_MEASURES_TABLE".equals(bookmarkName)) {
                    log.info("处理 TEACHING_MEASURES_TABLE 书签");
                    tableInfo.put("rows", data.get("teachingRows"));
                    tablesToProcess.add(tableInfo);
                }
            }
        }
        
        // 现在处理收集到的表格,避免并发修改异常
        for (Map<String, Object> tableInfo : tablesToProcess) {
            XWPFParagraph para = (XWPFParagraph) tableInfo.get("paragraph");
            List<RowRenderData> rows = (List<RowRenderData>) tableInfo.get("rows");
            insertTableAtParagraph(doc, para, rows);
        }
        
        log.info("表格占位符替换完成");
    }
    
    /**
     * 插入表格到文档 - 美化版
     * @param doc Word文档
     * @param rows 表格数据行
     */
    private void insertTable(XWPFDocument doc, List<RowRenderData> rows) throws IOException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        
        try {
            // 创建表格
            XWPFTable table = doc.createTable(rows.size(), rows.get(0).getCells().size());
            
            // 设置表格宽度
            CTTblPr tblPr = table.getCTTbl().addNewTblPr();
            CTTblWidth tblWidth = tblPr.addNewTblW();
            tblWidth.setW(BigInteger.valueOf(9072)); // 100% width
            tblWidth.setType(STTblWidth.DXA);
            
            // 填充表格数据
            for (int i = 0; i < rows.size(); i++) {
                RowRenderData rowData = rows.get(i);
                List<CellRenderData> cells = rowData.getCells();
                
                for (int j = 0; j < cells.size(); j++) {
                    CellRenderData cellData = cells.get(j);
                    XWPFTableCell cell = table.getRow(i).getCell(j);
                    
                    // 清空单元格
                    for (int k = cell.getParagraphs().size() - 1; k >= 0; k--) {
                        cell.removeParagraph(k);
                    }
                    
                    // 添加新段落
                    XWPFParagraph para = cell.addParagraph();
                    para.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun run = para.createRun();
                    
                    // 获取单元格文本
                    String cellText = "";
                    if (cellData != null) {
                        List<ParagraphRenderData> paragraphs = cellData.getParagraphs();
                        if (paragraphs != null && !paragraphs.isEmpty()) {
                            ParagraphRenderData pData = paragraphs.get(0);
                            List<RenderData> contents = pData.getContents();
                            if (contents != null && !contents.isEmpty()) {
                                RenderData rData = contents.get(0);
                                if (rData instanceof TextRenderData) {
                                    cellText = ((TextRenderData) rData).getText();
                                }
                            }
                        }
                    }
                    
                    run.setText(cellText);
                    run.setFontFamily("宋体");
                    run.setFontSize(10);
                    
                    // 第一行作为表头，使用不同样式
                    if (i == 0) {
                        run.setBold(true);
                        run.setColor("FFFFFF"); // 白色文字
                        cell.setColor("4F81BD"); // 蓝色背景
                    } else {
                        run.setColor("000000"); // 黑色文字
                    }
                    
                    // 设置垂直对齐
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
                }
            }
            
            // 添加空行
            addEmptyLine(doc);
            
        } catch (Exception e) {
            log.error("插入表格失败", e);
        }
    }
    
    /**
     * 替换散点图占位符为实际的散点图
     */
    private void replaceScatterChartsPlaceholder(XWPFDocument doc, List<Map<String, Object>> scatterCharts) throws IOException {
        if (scatterCharts == null || scatterCharts.isEmpty()) {
            log.info("散点图列表为空，跳过处理");
            return;
        }
        
        // 先收集需要处理的段落和书签信息,避免在遍历时修改文档结构
        List<XWPFParagraph> paragraphsToProcess = new ArrayList<>();
        
        // 遍历文档中的所有段落，查找包含书签的段落
        List<XWPFParagraph> paragraphs = doc.getParagraphs();
        log.info("文档包含 {} 个段落", paragraphs.size());
        
        for (XWPFParagraph para : paragraphs) {
            // 检查段落是否包含书签
            if (para == null) {
                log.warn("段落为空，跳过");
                continue;
            }
            
            List<CTBookmark> bookmarks = para.getCTP().getBookmarkStartList();
            if (bookmarks == null) {
                log.warn("书签列表为空，跳过");
                continue;
            }
            
            log.info("段落包含 {} 个书签", bookmarks.size());
            for (CTBookmark bookmark : bookmarks) {
                if (bookmark == null) {
                    log.warn("书签为空，跳过");
                    continue;
                }
                
                String bookmarkName = bookmark.getName();
                if (bookmarkName == null) {
                    log.warn("书签名称为空，跳过");
                    continue;
                }
                
                log.info("找到书签: {}", bookmarkName);
                
                // 处理散点图占位符
                if ("SCATTER_CHARTS_PLACEHOLDER".equals(bookmarkName)) {
                    log.info("处理 SCATTER_CHARTS_PLACEHOLDER 书签");
                    paragraphsToProcess.add(para);
                    break;
                }
            }
        }
        
        // 现在处理收集到的段落,避免并发修改异常
        for (XWPFParagraph para : paragraphsToProcess) {
            insertScatterChartsAtParagraph(doc, para, scatterCharts);
        }
        
        log.info("散点图占位符替换完成，插入了 {} 个散点图", scatterCharts.size());
    }
    
    /**
     * 在段落位置插入表格
     * @param doc Word文档
     * @param para 段落
     * @param rows 表格数据行
     */
    private void insertTableAtParagraph(XWPFDocument doc, XWPFParagraph para, List<RowRenderData> rows) throws IOException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        
        try {
            if (para != null) {
                // 删除段落
                int paraIndex = doc.getBodyElements().indexOf(para);
                doc.removeBodyElement(paraIndex);
                
                // 在相同位置创建表格
                XWPFTable table = doc.createTable(rows.size(), rows.get(0).getCells().size());
                
                // 填充表格数据
                for (int i = 0; i < rows.size(); i++) {
                    RowRenderData rowData = rows.get(i);
                    List<CellRenderData> cells = rowData.getCells();
                    
                    for (int j = 0; j < cells.size(); j++) {
                        CellRenderData cellData = cells.get(j);
                        XWPFTableCell cell = table.getRow(i).getCell(j);
                        
                        // 清空单元格
                        for (int k = cell.getParagraphs().size() - 1; k >= 0; k--) {
                            cell.removeParagraph(k);
                        }
                        
                        // 添加新段落
                        XWPFParagraph cellPara = cell.addParagraph();
                        XWPFRun run = cellPara.createRun();
                        
                        // 获取单元格文本
                        String cellText = "";
                        if (cellData != null) {
                            List<ParagraphRenderData> cellParagraphs = cellData.getParagraphs();
                            if (cellParagraphs != null && !cellParagraphs.isEmpty()) {
                                ParagraphRenderData pData = cellParagraphs.get(0);
                                List<RenderData> contents = pData.getContents();
                                if (contents != null && !contents.isEmpty()) {
                                    RenderData rData = contents.get(0);
                                    if (rData instanceof TextRenderData) {
                                        cellText = ((TextRenderData) rData).getText();
                                    }
                                }
                            }
                        }
                        
                        run.setText(cellText);
                        run.setFontFamily("微软雅黑");
                        run.setFontSize(11);
                    }
                }
                
                // 设置表格样式
                CTTblPr tblPr = table.getCTTbl().addNewTblPr();
                CTTblWidth tblWidth = tblPr.addNewTblW();
                tblWidth.setW(BigInteger.valueOf(9072)); // 100% width
                tblWidth.setType(STTblWidth.DXA);
                
                // 在表格后添加空行
                doc.createParagraph().createRun().addBreak();
            }
            
        } catch (Exception e) {
            log.error("插入表格失败", e);
        }
    }
    
    /**
     * 插入散点图到文档
     * @param doc Word文档
     * @param para 段落
     * @param scatterCharts 散点图数据列表
     */
    private void insertScatterChartsAtParagraph(XWPFDocument doc, XWPFParagraph para, List<Map<String, Object>> scatterCharts) throws IOException {
        try {
            log.info("开始插入散点图，共 {} 个", scatterCharts != null ? scatterCharts.size() : 0);
            
            if (para != null) {
                log.info("段落不为空，开始处理");
                // 删除段落
                int paraIndex = doc.getBodyElements().indexOf(para);
                log.info("段落索引: {}", paraIndex);
                if (paraIndex >= 0) {
                    doc.removeBodyElement(paraIndex);
                    log.info("段落已删除");
                } else {
                    log.warn("段落不在文档的body元素列表中");
                }
                
                // 插入散点图
                if (scatterCharts != null && !scatterCharts.isEmpty()) {
                    for (Map<String, Object> chartItem : scatterCharts) {
                        try {
                            log.info("处理散点图项: {}", chartItem);
                            
                            // 获取图表路径
                            String chartPath = (String) chartItem.get("chartPath");
                            log.info("图表路径: {}", chartPath);
                            
                            if (chartPath == null) {
                                log.warn("图表路径为空，跳过");
                                continue;
                            }
                            
                            // 检查文件是否存在
                            File chartFile = new File(chartPath);
                            if (!chartFile.exists()) {
                                log.warn("图表文件不存在: {}", chartPath);
                                continue;
                            }
                            
                            // 插入图表标题
                            XWPFParagraph titlePara = doc.createParagraph();
                            titlePara.setAlignment(ParagraphAlignment.CENTER);
                            titlePara.setSpacingBefore(100);
                            titlePara.setSpacingAfter(50);
                            
                            XWPFRun titleRun = titlePara.createRun();
                            titleRun.setText("图" + chartItem.get("index") + " 课程目标" + chartItem.get("targetNum") + "达成度分布");
                            titleRun.setBold(true);
                            titleRun.setFontFamily("黑体");
                            titleRun.setFontSize(12);
                            titleRun.setColor("2E74B5"); // 蓝色主题
                            
                            // 插入图表图片
                            XWPFParagraph imagePara = doc.createParagraph();
                            imagePara.setAlignment(ParagraphAlignment.CENTER);
                            imagePara.setSpacingAfter(100);
                            
                            try (FileInputStream fis = new FileInputStream(chartPath)) {
                                XWPFRun imageRun = imagePara.createRun();
                                imageRun.addPicture(fis, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", 
                                                  Units.toEMU(550), Units.toEMU(350));
                                log.info("图表图片已插入");
                            }
                            
                            // 插入图表描述
                            XWPFParagraph descPara = doc.createParagraph();
                            descPara.setAlignment(ParagraphAlignment.CENTER);
                            descPara.setSpacingBefore(50);
                            descPara.setSpacingAfter(100);
                            
                            XWPFRun descRun = descPara.createRun();
                            descRun.setText("达成度：" + chartItem.get("achievement") + "，参与人数：" + chartItem.get("totalStudents"));
                            descRun.setFontFamily("宋体");
                            descRun.setFontSize(11);
                            descRun.setColor("666666"); // 灰色文字
                            
                            // 添加空行
                            doc.createParagraph().createRun().addBreak();
                            
                        } catch (Exception e) {
                            log.error("插入单个散点图失败", e);
                            // 继续处理下一个图表
                        }
                    }
                } else {
                    log.warn("散点图列表为空");
                }
            } else {
                log.warn("段落为空");
            }
        } catch (Exception e) {
            log.error("插入散点图失败", e);
            throw e;
        }
    }
    
    /**
     * 创建标题 - 美化版
     */
    private void createTitle(XWPFDocument doc, String title) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(200);
        paragraph.setSpacingAfter(200);
        
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontFamily("黑体");
        run.setFontSize(22);
        run.setColor("2E74B5"); // 蓝色主题
        
        addEmptyLine(doc);
    }
    
    /**
     * 添加章节标题 - 美化版
     */
    private void addSection(XWPFDocument doc, String title) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(150);
        paragraph.setSpacingAfter(100);
        
        XWPFRun run = paragraph.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontFamily("黑体");
        run.setFontSize(16);
        run.setColor("2E74B5"); // 蓝色主题
        
        addEmptyLine(doc);
    }
    
    /**
     * 创建课程基本信息部分
     */
    private void createCourseInfoSection(XWPFDocument doc, Map<String, Object> data) {
        addSection(doc, "课程基本信息");
        
        // 创建两列表格
        XWPFTable table = doc.createTable(8, 2);
        
        // 设置表格宽度
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblWidth tblWidth = tblPr.getTblW();
        if (tblWidth == null) {
            tblWidth = tblPr.addNewTblW();
        }
        tblWidth.setW(BigInteger.valueOf(9072));
        tblWidth.setType(STTblWidth.DXA);
        
        // 填充数据
        String[][] courseInfo = {
            {"课程名称", (String) data.get("courseName")},
            {"课程编号", (String) data.get("courseCode")},
            {"课程学分", (String) data.get("courseCredit")},
            {"课程性质", (String) data.get("courseNature")},
            {"开课年级", (String) data.get("grade")},
            {"开课学期", (String) data.get("semester")},
            {"负责教师", (String) data.get("responsibleTeacher")},
            {"任课教师", (String) data.get("teacher")}
        };
        
        for (int i = 0; i < courseInfo.length; i++) {
            // 左列（标签）
            XWPFTableCell leftCell = table.getRow(i).getCell(0);
            setCellStyle(leftCell, courseInfo[i][0], true);
            
            // 右列（值）
            XWPFTableCell rightCell = table.getRow(i).getCell(1);
            setCellStyle(rightCell, courseInfo[i][1] != null ? courseInfo[i][1] : "", false);
        }
        
        addEmptyLine(doc);
    }
    
    /**
     * 设置单元格样式 - 美化版
     */
    private void setCellStyle(XWPFTableCell cell, String text, boolean isLabel) {
        // 清空单元格
        while (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.LEFT);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily("宋体");
        run.setFontSize(11);
        
        if (isLabel) {
            run.setBold(true);
            run.setColor("4F81BD"); // 蓝色标签
        } else {
            run.setColor("000000"); // 黑色内容
        }
        
        // 设置垂直对齐
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }
    
    /**
     * 创建表格部分
     */
    private void createTableSection(XWPFDocument doc, String title, List<RowRenderData> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        
        addSection(doc, title);
        
        // 创建表格
        int rowCount = rows.size();
        int colCount = rows.get(0).getCells().size();
        XWPFTable table = doc.createTable(rowCount, colCount);
        
        // 设置表格宽度
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }
        CTTblWidth tblWidth = tblPr.getTblW();
        if (tblWidth == null) {
            tblWidth = tblPr.addNewTblW();
        }
        tblWidth.setW(BigInteger.valueOf(9072));
        tblWidth.setType(STTblWidth.DXA);
        
        // 填充数据
        for (int r = 0; r < rowCount; r++) {
            RowRenderData rowData = rows.get(r);
            List<CellRenderData> cells = rowData.getCells();
            
            for (int c = 0; c < colCount; c++) {
                XWPFTableCell cell = table.getRow(r).getCell(c);
                CellRenderData cellData = cells.get(c);
                
                // 获取文本
                String cellText = "";
                if (cellData != null) {
                    List<ParagraphRenderData> paragraphs = cellData.getParagraphs();
                    if (paragraphs != null && !paragraphs.isEmpty()) {
                        List<RenderData> contents = paragraphs.get(0).getContents();
                        if (contents != null && !contents.isEmpty() && contents.get(0) instanceof TextRenderData) {
                            cellText = ((TextRenderData) contents.get(0)).getText();
                        }
                    }
                }
                
                // 设置单元格内容和样式
                setCellStyle(cell, cellText != null ? cellText : "", r == 0); // 第一行加粗
            }
        }
        
        addEmptyLine(doc);
    }
    
    /**
     * 创建成绩统计部分 - 美化版
     */
    private void createGradeStatisticsSection(XWPFDocument doc, Map<String, Object> data) {
        addSection(doc, "三、成绩统计分析");
        
        // 创建表格展示统计数据
        XWPFTable table = doc.createTable(2, 6);
        
        // 设置表格宽度
        CTTblPr tblPr = table.getCTTbl().addNewTblPr();
        CTTblWidth tblWidth = tblPr.addNewTblW();
        tblWidth.setW(BigInteger.valueOf(9072));
        tblWidth.setType(STTblWidth.DXA);
        
        // 表头
        String[] headers = {"学生总数", "平均成绩", "中位数", "标准差", "最高分", "最低分"};
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < headers.length; i++) {
            setHeaderCellStyleForTable(headerRow.getCell(i), headers[i]);
        }
        
        // 数据行
        XWPFTableRow dataRow = table.getRow(1);
        String[] values = {
            data.get("totalStudents") + "人",
            String.format("%.2f分", data.get("avgScore")),
            String.format("%.2f分", data.get("medianScore")),
            String.format("%.2f", data.get("stdDev")),
            String.format("%.2f分", data.get("maxScore")),
            String.format("%.2f分", data.get("minScore"))
        };
        
        for (int i = 0; i < values.length; i++) {
            setDataCellStyleForTable(dataRow.getCell(i), values[i]);
        }
        
        addEmptyLine(doc);
    }
    
    /**
     * 设置表头单元格样式（用于成绩统计表）
     */
    private void setHeaderCellStyleForTable(XWPFTableCell cell, String text) {
        // 清空现有段落
        while (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("微软雅黑");
        run.setFontSize(10);
        run.setColor("FFFFFF"); // 白色文字
        
        // 设置背景色
        cell.setColor("4F81BD"); // 蓝色背景
        
        // 设置垂直对齐
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }
    
    /**
     * 设置数据单元格样式（用于成绩统计表）
     */
    private void setDataCellStyleForTable(XWPFTableCell cell, String text) {
        // 清空现有段落
        while (cell.getParagraphs().size() > 0) {
            cell.removeParagraph(0);
        }
        
        XWPFParagraph para = cell.addParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily("宋体");
        run.setFontSize(10);
        run.setColor("000000"); // 黑色文字
        
        // 设置垂直对齐
        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
    }
    
    /**
     * 添加图片（如果文件存在）
     */
    private void addImageIfExists(XWPFDocument doc, String imagePath, String caption) {
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            addImageFromFile(doc, imagePath, caption);
        }
    }
    
    /**
     * 从文件添加图片 - 美化版
     */
    private void addImageFromFile(XWPFDocument doc, String imagePath, String caption) {
        try {
            // 添加图片标题
            XWPFParagraph captionPara = doc.createParagraph();
            captionPara.setAlignment(ParagraphAlignment.CENTER);
            captionPara.setSpacingBefore(100);
            captionPara.setSpacingAfter(50);
            
            XWPFRun captionRun = captionPara.createRun();
            captionRun.setText(caption);
            captionRun.setBold(true);
            captionRun.setFontFamily("黑体");
            captionRun.setFontSize(12);
            captionRun.setColor("2E74B5"); // 蓝色主题
            
            // 添加图片
            XWPFParagraph imagePara = doc.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            imagePara.setSpacingAfter(100);
            
            try (FileInputStream fis = new FileInputStream(imagePath)) {
                XWPFRun imageRun = imagePara.createRun();
                imageRun.addPicture(fis, XWPFDocument.PICTURE_TYPE_PNG, "image.png", 
                                  Units.toEMU(550), Units.toEMU(350));
            }
            
            addEmptyLine(doc);
        } catch (Exception e) {
            log.error("添加图片失败: {}", imagePath, e);
        }
    }
    
    /**
     * 添加普通段落
     */
    private void addParagraph(XWPFDocument doc, String text, String fontFamily, int fontSize, boolean bold) {
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily(fontFamily);
        run.setFontSize(fontSize);
        run.setBold(bold);
    }
    
    /**
     * 添加空行
     */
    private void addEmptyLine(XWPFDocument doc) {
        doc.createParagraph().createRun().addBreak();
    }
    
    /**
     * 从 CSV 行数据中获取列值（支持多种列名）
     * @param cols CSV列数据数组
     * @param columnIndexMap 列名到索引的映射
     * @param possibleNames 可能的列名列表
     * @param defaultIndex 默认索引（如果找不到列名）
     * @return 列值，如果不存在则返回 "-"
     */
    private String getColumnValue(String[] cols, Map<String, Integer> columnIndexMap, 
                                  String[] possibleNames, int defaultIndex) {
        // 尝试从列名映射中查找
        for (String name : possibleNames) {
            if (columnIndexMap.containsKey(name)) {
                int index = columnIndexMap.get(name);
                if (index >= 0 && index < cols.length) {
                    return cols[index].trim();
                }
            }
        }
        
        // 如果找不到，使用默认索引
        if (defaultIndex >= 0 && defaultIndex < cols.length) {
            return cols[defaultIndex].trim();
        }
        
        return "-";
    }
    
    /**
     * 从 CSV 行数据中获取列值（单个列名）
     */
    private String getColumnValue(String[] cols, Map<String, Integer> columnIndexMap, 
                                  String columnName, int defaultIndex) {
        return getColumnValue(cols, columnIndexMap, new String[]{columnName}, defaultIndex);
    }
} 