package com.ruoyi.common.utils.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/**
 * Python脚本执行工具类
 * 
 * @author ruoyi
 */
@Component
public class PythonScriptRunner {
    
    private static final Logger log = LoggerFactory.getLogger(PythonScriptRunner.class);
    
    /**
     * 执行Python脚本
     * 
     * @param pythonPath Python可执行文件路径
     * @param scriptPath 脚本路径
     * @param workingDir 工作目录
     * @param timeoutSeconds 超时时间(秒)
     * @param env 环境变量
     * @return 是否执行成功
     */
    public static boolean runScript(String pythonPath, String scriptPath, String workingDir, 
                                   int timeoutSeconds, Map<String, String> env) {
        try {
            File scriptFile = null;
            
            // 处理classpath路径
            if (scriptPath.startsWith("classpath:")) {
                String resourcePath = scriptPath.substring("classpath:".length());
                try {
                    // 从classpath加载资源
                    ClassPathResource resource = new ClassPathResource(resourcePath);
                    
                    // 将资源复制到临时文件
                    scriptFile = File.createTempFile("python_script_", ".py");
                    scriptFile.deleteOnExit();
                    
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    scriptPath = scriptFile.getAbsolutePath();
                    log.info("从classpath加载脚本: {}", resourcePath);
                } catch (IOException e) {
                    log.error("无法从classpath加载脚本", e);
                    return false;
                }
            }
            
            // 构建命令
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            
            log.info("=== Python脚本执行详细信息 ===");
            log.info("Python路径: {}", pythonPath);
            log.info("脚本路径: {}", scriptPath);
            log.info("工作目录: {}", workingDir);
            log.info("完整命令: {}", String.join(" ", pb.command()));
            
            // 设置工作目录
            if (workingDir != null && !workingDir.isEmpty()) {
                File workDirFile = new File(workingDir);
                log.info("工作目录是否存在: {}", workDirFile.exists());
                log.info("工作目录绝对路径: {}", workDirFile.getAbsolutePath());
                pb.directory(workDirFile);
            }
            
            // 设置环境变量
            if (env != null && !env.isEmpty()) {
                Map<String, String> environment = pb.environment();
                env.forEach(environment::put);
                log.info("添加环境变量: {}", env);
            }
            
            // 检查Python可执行文件是否存在
            File pythonFile = new File(pythonPath);
            log.info("Python可执行文件是否存在: {}", pythonFile.exists());
            if (pythonFile.exists()) {
                log.info("Python文件大小: {} bytes", pythonFile.length());
                log.info("Python文件可执行: {}", pythonFile.canExecute());
            }
            
            // 检查脚本文件是否存在
            if (scriptFile != null) {
                log.info("临时脚本文件: {}", scriptFile.getAbsolutePath());
                log.info("临时脚本文件是否存在: {}", scriptFile.exists());
                log.info("临时脚本文件大小: {} bytes", scriptFile.length());
            }
            
            // 重定向错误流到标准输出
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            log.info("执行Python脚本: {} {}", pythonPath, scriptPath);
            Process process = pb.start();
            process.getOutputStream().close();
            log.info("进程启动成功");
            
            // 读取输出
            StringBuilder output = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Python输出: {}", line);
                    output.append(line).append("\n");
                }
            } catch (IOException e) {
                log.error("读取Python进程输出失败", e);
            }
            
            log.info("Python完整输出:\n{}", output.toString());
            
            // 等待进程完成
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                log.error("Python脚本执行超时");
                process.destroy();
                return false;
            }
            
            // 检查退出码
            int exitCode = process.exitValue();
            log.info("Python脚本执行完成，退出码: {}", exitCode);
            return exitCode == 0;
            
        } catch (IOException | InterruptedException e) {
            log.error("执行Python脚本出错", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

} 