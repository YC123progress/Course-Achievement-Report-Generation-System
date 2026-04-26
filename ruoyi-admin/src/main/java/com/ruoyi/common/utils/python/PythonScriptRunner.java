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
import java.util.concurrent.TimeUnit;

/**
 * Runs Python scripts bundled in classpath resources.
 */
public class PythonScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(PythonScriptRunner.class);

    public static boolean runScript(String pythonExecutable, String scriptResource,
                                    String workingDirectory, int timeoutSeconds,
                                    Map<String, String> environmentVars) {
        String tempScriptPath = null;

        try {
            log.info("Loading Python script from classpath: {}", scriptResource);
            ClassPathResource resource = new ClassPathResource(scriptResource);

            String scriptName = scriptResource.substring(scriptResource.lastIndexOf('/') + 1);
            tempScriptPath = System.getProperty("java.io.tmpdir") + File.separator
                    + "python_script_" + System.nanoTime() + "_" + scriptName;

            try (InputStream is = resource.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempScriptPath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            File workDir = new File(workingDirectory);
            File tempScript = new File(tempScriptPath);

            log.info("=== Python script execution ===");
            log.info("Python executable: {}", pythonExecutable);
            log.info("Script path: {}", tempScriptPath);
            log.info("Working directory: {}", workDir.getAbsolutePath());
            log.info("Working directory exists: {}", workDir.exists());
            log.info("Temp script exists: {}, size: {} bytes", tempScript.exists(), tempScript.length());

            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, tempScriptPath);
            pb.directory(workDir);

            Map<String, String> env = pb.environment();
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");
            if (environmentVars != null) {
                env.putAll(environmentVars);
            }

            log.info("Starting Python process: {} {}", pythonExecutable, tempScriptPath);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            Thread stdoutThread = new Thread(() -> readProcessStream(process.getInputStream(), output, false),
                    "python-stdout-reader");
            Thread stderrThread = new Thread(() -> readProcessStream(process.getErrorStream(), errorOutput, true),
                    "python-stderr-reader");
            stdoutThread.start();
            stderrThread.start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
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
