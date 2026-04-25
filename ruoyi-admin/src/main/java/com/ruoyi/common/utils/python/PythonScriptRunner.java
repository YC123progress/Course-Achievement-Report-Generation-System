package com.ruoyi.common.utils.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python脚本运行工具类
 */
public class PythonScriptRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PythonScriptRunner.class);

    /**
     * 运行Python脚本
     * @param pythonExecutable Python可执行文件路径
     * @param scriptResource 脚本资源路径
     * @param workingDirectory 工作目录
     * @param timeoutSeconds 超时时间（秒）
     * @param environmentVars 环境变量
     * @return 是否执行成功
     */
    public static boolean runScript(String pythonExecutable, String scriptResource, 
                                   String workingDirectory, int timeoutSeconds, 
                                   Map<String, String> environmentVars) {
        String tempScriptPath = null;
        
        try {
            // 1. 从classpath加载脚本到临时文件
            log.info("从classpath加载脚本: {}", scriptResource);
            ClassPathResource resource = new ClassPathResource(scriptResource);
            
            // 创建临时文件
            String scriptName = scriptResource.substring(scriptResource.lastIndexOf('/') + 1);
            tempScriptPath = System.getProperty("java.io.tmpdir") + File.separator + 
                           "python_script_" + System.nanoTime() + "_" + scriptName;
            
            // 复制脚本内容到临时文件（Java 8兼容方式）
            try (InputStream is = resource.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempScriptPath)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("=== Python脚本执行详细信息 ===");
            log.info("Python路径: {}", pythonExecutable);
            log.info("脚本路径: {}", tempScriptPath);
            log.info("工作目录: {}", workingDirectory);
            log.info("完整命令: {} {}", pythonExecutable, tempScriptPath);
            
            // 检查工作目录
            File workDir = new File(workingDirectory);
            log.info("工作目录是否存在: {}", workDir.exists());
            log.info("工作目录绝对路径: {}", workDir.getAbsolutePath());
            
            // 检查Python可执行文件
            if (!pythonExecutable.equals("python") && !pythonExecutable.equals("python3") && !pythonExecutable.equals("py")) {
                File pythonFile = new File(pythonExecutable);
                log.info("Python可执行文件是否存在: {}", pythonFile.exists());
                if (!pythonFile.exists()) {
                    log.warn("Python可执行文件不存在: {}", pythonExecutable);
                }
            } else {
                log.info("使用系统PATH中的Python: {}", pythonExecutable);
            }
            
            // 检查临时脚本文件
            File tempScript = new File(tempScriptPath);
            log.info("临时脚本文件: {}", tempScriptPath);
            log.info("临时脚本文件是否存在: {}", tempScript.exists());
            log.info("临时脚本文件大小: {} bytes", tempScript.length());
            
            // 2. 执行Python脚本
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, tempScriptPath);
            pb.directory(workDir);
            
            // 设置环境变量，确保Python输出使用UTF-8编码
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");
            if (environmentVars != null) {
                env.putAll(environmentVars);
            }
            
            log.info("执行Python脚本: {} {}", pythonExecutable, tempScriptPath);
            
            Process process = pb.start();
            log.info("进程启动成功");
            
            // 读取输出和错误流
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // 使用UTF-8编码读取输出
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("Python输出: {}", line);
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
                log.error("Python错误: {}", line);
            }
            
            // 等待进程完成
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Python脚本执行超时");
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            log.info("Python完整输出:\n{}", output.toString());
            if (errorOutput.length() > 0) {
                log.error("Python完整错误输出:\n{}", errorOutput.toString());
            }
            log.info("Python脚本执行完成，退出码: {}", exitCode);
            
            return exitCode == 0;
            
        } catch (Exception e) {
            log.error("执行Python脚本时发生异常", e);
            return false;
        } finally {
            // 清理临时文件
            if (tempScriptPath != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempScriptPath));
                    log.debug("临时脚本文件已删除: {}", tempScriptPath);
                } catch (Exception e) {
                    log.warn("删除临时脚本文件失败: {}", e.getMessage());
                }
            }
        }
    }
} 