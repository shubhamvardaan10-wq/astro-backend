package com.astro.service;

import com.astro.util.PythonPathResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * High-Performance Persistent Worker Pool for Python calculation workers.
 *
 * Eliminates cold OS process spawning by maintaining warm Python daemon
 * instances ready to process JSON requests via persistent NDJSON pipes.
 */
@Service
public class PythonWorkerPool {

    private static final Logger log = LoggerFactory.getLogger(PythonWorkerPool.class);

    private final ObjectMapper mapper;
    private final String pythonExecutable;
    private final Path daemonScript;
    private final int poolSize;
    private final int timeoutSeconds;
    private final BlockingQueue<WorkerProcess> availableWorkers;
    private final List<WorkerProcess> allWorkers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    @Autowired
    public PythonWorkerPool(
            ObjectMapper mapper,
            @Value("${astro.worker.python:}") String customPython,
            @Value("${astro.worker.script:worker/daemon.py}") String scriptPath,
            @Value("${astro.worker.pool-size:4}") int poolSize,
            @Value("${astro.worker.timeout-seconds:30}") int timeoutSeconds) {
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.pythonExecutable = PythonPathResolver.resolve(customPython);
        this.daemonScript = Paths.get(scriptPath).toAbsolutePath().normalize();
        this.poolSize = Math.max(1, poolSize);
        this.timeoutSeconds = Math.max(5, timeoutSeconds);
        this.availableWorkers = new ArrayBlockingQueue<>(this.poolSize);
    }

    @PostConstruct
    public void init() {
        if (!Files.isRegularFile(daemonScript)) {
            log.warn("Python worker daemon script not found at {}. Persistent pooling will be disabled.", daemonScript);
            return;
        }

        log.info("Initializing PythonWorkerPool: poolSize={}, script={}, python={}",
                poolSize, daemonScript, pythonExecutable);

        for (int i = 0; i < poolSize; i++) {
            try {
                WorkerProcess worker = spawnWorker(i + 1);
                if (worker != null) {
                    allWorkers.add(worker);
                    availableWorkers.offer(worker);
                }
            } catch (Exception e) {
                log.error("Failed to spawn initial Python worker {}: {}", i + 1, e.getMessage());
            }
        }

        log.info("PythonWorkerPool initialized with {} active warm workers.", availableWorkers.size());
    }

    public JsonNode execute(JsonNode request) throws Exception {
        byte[] bytes = mapper.writeValueAsBytes(request);
        String jsonLine = new String(bytes, StandardCharsets.UTF_8).replace('\n', ' ').replace('\r', ' ');
        String responseLine = sendReceive(jsonLine);
        return mapper.readTree(responseLine);
    }

    public JsonNode executeModule(String module, String function, Object... args) throws Exception {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("action", "module_call");
        payload.put("module", module);
        payload.put("function", function);
        if (args != null && args.length > 0) {
            payload.set("args", mapper.valueToTree(args));
        } else {
            payload.putArray("args");
        }
        return execute(payload);
    }

    public JsonNode analyze(JsonNode payload) throws Exception {
        ObjectNode req = mapper.createObjectNode();
        req.put("action", "analyze");
        req.set("payload", payload);
        return execute(req);
    }

    private String sendReceive(String line) throws Exception {
        if (shuttingDown.get()) {
            throw new IllegalStateException("Python worker pool is shutting down");
        }

        WorkerProcess worker = availableWorkers.poll(timeoutSeconds, TimeUnit.SECONDS);
        if (worker == null) {
            // Attempt emergency spawn if pool depleted
            worker = spawnWorker(allWorkers.size() + 1);
            if (worker == null) {
                throw new TimeoutException("All calculation workers are busy. Please retry.");
            }
            allWorkers.add(worker);
        }

        try {
            String result = worker.communicate(line, timeoutSeconds);
            availableWorkers.offer(worker);
            return result;
        } catch (Exception e) {
            log.warn("Worker error during calculation: {}. Recycling worker.", e.getMessage());
            worker.destroy();
            allWorkers.remove(worker);
            // Replace dead worker
            try {
                WorkerProcess replacement = spawnWorker(allWorkers.size() + 1);
                if (replacement != null) {
                    allWorkers.add(replacement);
                    availableWorkers.offer(replacement);
                }
            } catch (Exception spawnEx) {
                log.error("Failed to spawn replacement worker: {}", spawnEx.getMessage());
            }
            throw e;
        }
    }

    private WorkerProcess spawnWorker(int id) {
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "-u", daemonScript.toString());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.environment().put("OPENBLAS_NUM_THREADS", "1");
            pb.environment().put("OMP_NUM_THREADS", "1");
            pb.environment().put("PYTHONUNBUFFERED", "1");

            Process process = pb.start();
            WorkerProcess worker = new WorkerProcess(id, process);

            // Verify with quick ping
            String pingResponse = worker.communicate("{\"action\":\"ping\"}", 10);
            if (pingResponse != null && pingResponse.contains("pong")) {
                log.debug("Worker #{} verified healthy: {}", id, pingResponse.trim());
                return worker;
            } else {
                log.warn("Worker #{} ping failed with response: {}", id, pingResponse);
                worker.destroy();
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to start worker process #{}: {}", id, e.getMessage());
            return null;
        }
    }

    public boolean isHealthy() {
        return !availableWorkers.isEmpty();
    }

    public int getAvailableWorkerCount() {
        return availableWorkers.size();
    }

    @PreDestroy
    public void destroy() {
        shuttingDown.set(true);
        log.info("Shutting down PythonWorkerPool...");
        for (WorkerProcess worker : allWorkers) {
            try {
                worker.destroy();
            } catch (Exception ignored) {}
        }
        allWorkers.clear();
        availableWorkers.clear();
    }

    // ── Internal Worker Process Wrapper ───────────────────────────────────────
    private static class WorkerProcess {
        private final int id;
        private final Process process;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        WorkerProcess(int id, Process process) {
            this.id = id;
            this.process = process;
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        }

        synchronized String communicate(String jsonLine, int timeoutSec) throws IOException, TimeoutException {
            if (!process.isAlive()) {
                throw new IOException("Worker #" + id + " process terminated unexpectedly");
            }

            writer.write(jsonLine);
            writer.write("\n");
            writer.flush();

            // Read response line with timeout
            long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);
            while (!reader.ready() && System.currentTimeMillis() < deadline) {
                if (!process.isAlive()) {
                    throw new IOException("Worker #" + id + " died waiting for response");
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for worker response", e);
                }
            }

            if (!reader.ready() && System.currentTimeMillis() >= deadline) {
                throw new TimeoutException("Worker #" + id + " timed out after " + timeoutSec + "s");
            }

            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Worker #" + id + " reached EOF unexpectedly");
            }
            return line;
        }

        void destroy() {
            try {
                writer.close();
            } catch (Exception ignored) {}
            try {
                reader.close();
            } catch (Exception ignored) {}
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
