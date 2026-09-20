package com.astro.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Cross-platform Python executable locator for Windows, macOS, and Linux.
 */
public final class PythonPathResolver {

    private static final Logger log = LoggerFactory.getLogger(PythonPathResolver.class);

    private PythonPathResolver() {}

    public static String resolve(String customPath) {
        // 1. Explicit path from config / property
        if (customPath != null && !customPath.isBlank()) {
            Path p = Paths.get(customPath).toAbsolutePath().normalize();
            if (Files.exists(p) && !Files.isDirectory(p)) {
                log.info("Using configured Python executable: {}", p);
                return p.toString();
            }
        }

        // 2. Check environment variable ASTRO_PYTHON_EXECUTABLE
        String envPath = System.getenv("ASTRO_PYTHON_EXECUTABLE");
        if (envPath != null && !envPath.isBlank()) {
            Path p = Paths.get(envPath).toAbsolutePath().normalize();
            if (Files.exists(p) && !Files.isDirectory(p)) {
                log.info("Using ASTRO_PYTHON_EXECUTABLE: {}", p);
                return p.toString();
            }
        }

        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");

        // 3. Check project virtualenv locations
        List<String> venvCandidates = isWindows
                ? List.of(
                    "target/engine-venv/Scripts/python.exe",
                    ".venv/Scripts/python.exe",
                    "venv/Scripts/python.exe",
                    "worker/.venv/Scripts/python.exe"
                )
                : List.of(
                    "target/engine-venv/bin/python",
                    "target/engine-venv/bin/python3",
                    ".venv/bin/python",
                    ".venv/bin/python3",
                    "/opt/venv/bin/python",
                    "venv/bin/python"
                );

        for (String candidate : venvCandidates) {
            Path p = Paths.get(candidate).toAbsolutePath().normalize();
            if (Files.isExecutable(p) || (isWindows && Files.exists(p))) {
                log.info("Resolved virtualenv Python: {}", p);
                return p.toString();
            }
        }

        // 4. Check system PATH executables and common Windows locations
        String userHome = System.getProperty("user.home", "");
        List<String> systemCandidates = isWindows
                ? List.of(
                    userHome + "\\AppData\\Local\\Python\\pythoncore-3.14-64\\python.exe",
                    "py.exe",
                    "python.exe",
                    "C:\\Python314\\python.exe",
                    "C:\\Python313\\python.exe",
                    "C:\\Python312\\python.exe",
                    "C:\\Python311\\python.exe",
                    "C:\\Python310\\python.exe",
                    "python3.exe",
                    "py"
                )
                : List.of("python3", "python", "/usr/bin/python3", "/usr/local/bin/python3");

        for (String candidate : systemCandidates) {
            if (isCommandAvailable(candidate)) {
                log.info("Resolved system PATH Python: {}", candidate);
                return candidate;
            }
        }

        // Fallback default
        String fallback = isWindows ? "python" : "/usr/bin/python3";
        log.warn("Could not confirm Python binary; falling back to: {}", fallback);
        return fallback;
    }

    private static boolean isCommandAvailable(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean finished = p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
            return finished && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
