package com.ruoyi.common.utils.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        File scriptFile = null;
        ExecutorService streamExecutor = null;
        Process process = null;

        try {
            if (scriptPath.startsWith("classpath:")) {
                String resourcePath = scriptPath.substring("classpath:".length());
                try {
                    ClassPathResource resource = new ClassPathResource(resourcePath);
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
            
            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            
            log.info("=== Python脚本执行详细信息 ===");
            log.info("Python路径: {}", pythonPath);
            log.info("脚本路径: {}", scriptPath);
            log.info("工作目录: {}", workingDir);
            log.info("完整命令: {}", String.join(" ", pb.command()));
            
            if (workingDir != null && !workingDir.isEmpty()) {
                File workDirFile = new File(workingDir);
                log.info("工作目录是否存在: {}", workDirFile.exists());
                log.info("工作目录绝对路径: {}", workDirFile.getAbsolutePath());
                pb.directory(workDirFile);
            }
            
            Map<String, String> environment = pb.environment();
            environment.put("PYTHONIOENCODING", "utf-8");
            environment.put("PYTHONUNBUFFERED", "1");
            if (env != null && !env.isEmpty()) {
                env.forEach(environment::put);
                log.info("添加环境变量: {}", env);
            }
            
            File pythonFile = new File(pythonPath);
            log.info("Python可执行文件是否存在: {}", pythonFile.exists());
            if (pythonFile.exists()) {
                log.info("Python文件大小: {} bytes", pythonFile.length());
                log.info("Python文件可执行: {}", pythonFile.canExecute());
            }
            
            if (scriptFile != null) {
                log.info("临时脚本文件: {}", scriptFile.getAbsolutePath());
                log.info("临时脚本文件是否存在: {}", scriptFile.exists());
                log.info("临时脚本文件大小: {} bytes", scriptFile.length());
            }
            
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            log.info("执行Python脚本: {} {}", pythonPath, scriptPath);
            process = pb.start();
            closeProcessInput(process);
            log.info("进程启动成功");

            streamExecutor = Executors.newFixedThreadPool(2);
            Future<String> stdoutFuture = streamExecutor.submit(streamReader(process.getInputStream(), "stdout"));
            Future<String> stderrFuture = streamExecutor.submit(streamReader(process.getErrorStream(), "stderr"));

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                log.error("Python script timed out after {} seconds", timeoutSeconds);
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);

                String stdout = getFutureOutput(stdoutFuture, "stdout", 5);
                String stderr = getFutureOutput(stderrFuture, "stderr", 5);
                log.info("Python stdout:\n{}", stdout);
                log.error("Python stderr:\n{}", stderr);
                return false;
            }
            
            int exitCode = process.exitValue();
            String stdout = getFutureOutput(stdoutFuture, "stdout", 10);
            String stderr = getFutureOutput(stderrFuture, "stderr", 10);

            log.info("Python stdout:\n{}", stdout);
            if (stderr.length() > 0) {
                log.error("Python stderr:\n{}", stderr);
            } else {
                log.info("Python stderr:\n{}", stderr);
            }
            log.info("Python script exited with code: {}", exitCode);
            log.info("Python脚本执行完成，退出码: {}", exitCode);
            return exitCode == 0;
            
        } catch (IOException | InterruptedException e) {
            log.error("执行Python脚本出错", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            if (process != null) {
                closeProcessStreams(process);
            }
            if (streamExecutor != null) {
                streamExecutor.shutdownNow();
            }
            if (scriptFile != null && scriptFile.exists() && !scriptFile.delete()) {
                log.warn("临时脚本文件删除失败: {}", scriptFile.getAbsolutePath());
            }
        }
    }

    private static Callable<String> streamReader(final InputStream inputStream, final String streamName) {
        return () -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                    if ("stderr".equals(streamName)) {
                        log.error("Python stderr: {}", line);
                    } else {
                        log.info("Python stdout: {}", line);
                    }
                }
            } catch (IOException e) {
                log.warn("读取Python {} 失败: {}", streamName, e.getMessage());
            }
            return output.toString();
        };
    }

    private static String getFutureOutput(Future<String> future, String streamName, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("读取Python {} 超时", streamName);
        } catch (Exception e) {
            log.warn("读取Python {} 失败: {}", streamName, e.getMessage());
        }
        return "";
    }

    private static void closeProcessInput(Process process) {
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            log.debug("关闭Python stdin失败: {}", e.getMessage());
        }
    }

    private static void closeProcessStreams(Process process) {
        try {
            process.getInputStream().close();
        } catch (IOException e) {
            log.debug("关闭Python stdout失败: {}", e.getMessage());
        }
        try {
            process.getErrorStream().close();
        } catch (IOException e) {
            log.debug("关闭Python stderr失败: {}", e.getMessage());
        }
        try {
            process.getOutputStream().close();
        } catch (IOException e) {
            log.debug("关闭Python stdin失败: {}", e.getMessage());
        }
    }

} 
