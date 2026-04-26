package com.ruoyi.common.utils.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
        ExecutorService streamExecutor = null;
        Process process = null;
        
        try {
            log.info("从classpath加载脚本: {}", scriptResource);
            ClassPathResource resource = new ClassPathResource(scriptResource);
            
            String scriptName = scriptResource.substring(scriptResource.lastIndexOf('/') + 1);
            tempScriptPath = System.getProperty("java.io.tmpdir") + File.separator + 
                           "python_script_" + System.nanoTime() + "_" + scriptName;
            
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
            
            File workDir = new File(workingDirectory);
            log.info("工作目录是否存在: {}", workDir.exists());
            log.info("工作目录绝对路径: {}", workDir.getAbsolutePath());
            
            if (!pythonExecutable.equals("python") && !pythonExecutable.equals("python3") && !pythonExecutable.equals("py")) {
                File pythonFile = new File(pythonExecutable);
                log.info("Python可执行文件是否存在: {}", pythonFile.exists());
                if (!pythonFile.exists()) {
                    log.warn("Python可执行文件不存在: {}", pythonExecutable);
                }
            } else {
                log.info("使用系统PATH中的Python: {}", pythonExecutable);
            }
            
            File tempScript = new File(tempScriptPath);
            log.info("临时脚本文件: {}", tempScriptPath);
            log.info("临时脚本文件是否存在: {}", tempScript.exists());
            log.info("临时脚本文件大小: {} bytes", tempScript.length());
            
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, tempScriptPath);
            pb.directory(workDir);
            
            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");
            if (environmentVars != null) {
                env.putAll(environmentVars);
            }
            
            log.info("执行Python脚本: {} {}", pythonExecutable, tempScriptPath);
            process = pb.start();
            closeProcessInput(process);
            log.info("进程启动成功");
            
            streamExecutor = Executors.newFixedThreadPool(2);
            Future<String> stdoutFuture = streamExecutor.submit(streamReader(process.getInputStream(), "stdout"));
            Future<String> stderrFuture = streamExecutor.submit(streamReader(process.getErrorStream(), "stderr"));
            
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
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
            
        } catch (Exception e) {
            log.error("执行Python脚本时发生异常", e);
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
