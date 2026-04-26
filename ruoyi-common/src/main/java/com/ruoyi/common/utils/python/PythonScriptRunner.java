package com.ruoyi.common.utils.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

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
import java.util.concurrent.TimeUnit;

/**
 * Runs Python scripts and keeps timeout handling outside stream reads.
 */
@Component
public class PythonScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(PythonScriptRunner.class);

    public static boolean runScript(String pythonPath, String scriptPath, String workingDir,
                                    int timeoutSeconds, Map<String, String> env) {
        String tempScriptPath = null;

        try {
            String executableScriptPath = scriptPath;
            if (scriptPath.startsWith("classpath:") || !new File(scriptPath).exists()) {
                String resourcePath = scriptPath.startsWith("classpath:")
                        ? scriptPath.substring("classpath:".length())
                        : scriptPath;
                tempScriptPath = copyClasspathResourceToTempFile(resourcePath);
                executableScriptPath = tempScriptPath;
            }

            File workDirFile = new File(workingDir);
            log.info("=== Python script execution ===");
            log.info("Python executable: {}", pythonPath);
            log.info("Script path: {}", executableScriptPath);
            log.info("Working directory: {}", workDirFile.getAbsolutePath());
            log.info("Working directory exists: {}", workDirFile.exists());

            ProcessBuilder pb = new ProcessBuilder(pythonPath, executableScriptPath);
            if (workingDir != null && !workingDir.isEmpty()) {
                pb.directory(workDirFile);
            }

            Map<String, String> environment = pb.environment();
            environment.put("PYTHONIOENCODING", "utf-8");
            environment.put("PYTHONUNBUFFERED", "1");
            if (env != null) {
                environment.putAll(env);
            }

            Process process = pb.start();
            try {
                process.getOutputStream().close();
            } catch (IOException ignored) {
                // The scripts do not read stdin.
            }

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            Thread stdoutThread = new Thread(() -> readProcessStream(process.getInputStream(), output, false),
                    "python-stdout-reader");
            Thread stderrThread = new Thread(() -> readProcessStream(process.getErrorStream(), errorOutput, true),
                    "python-stderr-reader");
            stdoutThread.start();
            stderrThread.start();

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                log.error("Python script timed out after {} seconds", timeoutSeconds);
                process.destroyForcibly();
                stdoutThread.join(1000);
                stderrThread.join(1000);
                return false;
            }

            stdoutThread.join(1000);
            stderrThread.join(1000);

            int exitCode = process.exitValue();
            log.info("Python stdout:\n{}", output);
            if (errorOutput.length() > 0) {
                log.error("Python stderr:\n{}", errorOutput);
            }
            log.info("Python script exited with code: {}", exitCode);
            return exitCode == 0;
        } catch (Exception e) {
            log.error("Failed to execute Python script", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            if (tempScriptPath != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempScriptPath));
                } catch (Exception e) {
                    log.warn("Failed to delete temporary Python script {}: {}", tempScriptPath, e.getMessage());
                }
            }
        }
    }

    private static String copyClasspathResourceToTempFile(String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        String scriptName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        String tempScriptPath = System.getProperty("java.io.tmpdir") + File.separator
                + "python_script_" + System.nanoTime() + "_" + scriptName;

        try (InputStream is = resource.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempScriptPath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        return tempScriptPath;
    }

    private static void readProcessStream(InputStream inputStream, StringBuilder output, boolean error) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                if (error) {
                    log.error("Python stderr: {}", line);
                } else {
                    log.info("Python stdout: {}", line);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read Python process stream: {}", e.getMessage());
        }
    }
}
