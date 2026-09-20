package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class AdvancedEngineService {

    private static final int MAX_INPUT = 32768;
    private static final int MAX_OUTPUT = 4 * 1024 * 1024;
    private final ObjectMapper mapper;
    private final CityService cities;
    private final String python;
    private final Path script;
    private final Semaphore workers = new Semaphore(2);
    private final ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();

    public AdvancedEngineService(ObjectMapper mapper, CityService cities,
            @Value("${astro.worker.python:target/engine-venv/bin/python}") String python,
            @Value("${astro.worker.script:worker/engine.py}") String script) {
        this.mapper = mapper;
        this.cities = cities;
        this.python = python;
        this.script = Path.of(script).toAbsolutePath().normalize();
    }

    public JsonNode analyze(AdvancedRequest request) {
        ObjectNode payload = mapper.valueToTree(request);
        if (payload.get("birth") instanceof ObjectNode birth) normalizeLocation(birth);
        if (payload.get("partner") instanceof ObjectNode partner) normalizeLocation(partner);
        if (payload.get("location") instanceof ObjectNode location) normalizeLocation(location);
        return invoke(payload);
    }

    public JsonNode capabilities() {
        return invoke(mapper.valueToTree(Map.of("action", "capabilities")));
    }

    public JsonNode resolveTimezones(JsonNode locations) {
        if (locations == null || !locations.isArray() || locations.isEmpty() || locations.size() > 5) {
            throw new IllegalArgumentException("Provide one to five coordinate pairs");
        }
        ObjectNode payload = mapper.createObjectNode().put("action", "timezones");
        payload.set("locations", locations);
        return invoke(payload);
    }

    private void normalizeLocation(ObjectNode birth) {
        if (birth == null) throw new IllegalArgumentException("birth is required");
        boolean hasLatitude = birth.hasNonNull("latitude");
        boolean hasLongitude = birth.hasNonNull("longitude");
        if (hasLatitude != hasLongitude) throw new IllegalArgumentException("latitude and longitude must be supplied together");
        if (!hasLatitude) {
            var city = cities.findCity(birth.path("city").asText());
            if (city == null) throw new IllegalArgumentException("Provide latitude, longitude, and timezone for an unknown city");
            birth.put("latitude", city.getLatitude());
            birth.put("longitude", city.getLongitude());
            if (!birth.hasNonNull("timezone")) birth.put("timezone", "Asia/Kolkata");
        }
    }

    private JsonNode invoke(JsonNode request) {
        if (!Files.isRegularFile(script)) throw unavailable("Advanced calculation worker is not installed");
        if (!workers.tryAcquire()) throw unavailable("Calculation capacity is busy; retry later");
        Process process = null;
        Future<byte[]> output = null;
        try {
            byte[] input = mapper.writeValueAsBytes(request);
            if (input.length > MAX_INPUT) throw new IllegalArgumentException("Request exceeds 32 KiB");
            ProcessBuilder builder = new ProcessBuilder(python, "-I", script.toString());
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            builder.environment().put("OPENBLAS_NUM_THREADS", "1");
            builder.environment().put("OMP_NUM_THREADS", "1");
            process = builder.start();
            Process child = process;
            output = readers.submit(() -> {
                byte[] bytes = child.getInputStream().readNBytes(MAX_OUTPUT + 1);
                if (bytes.length > MAX_OUTPUT) {
                    terminate(child);
                    throw new IOException("Calculation response exceeds limit");
                }
                return bytes;
            });
            try (var stdin = process.getOutputStream()) {
                stdin.write(input);
            }
            if (!process.waitFor(60, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Calculation exceeded the time limit");
            }
            byte[] bytes = output.get(2, TimeUnit.SECONDS);
            if (process.exitValue() != 0 || bytes.length == 0) throw unavailable("Calculation worker failed to start or exited unexpectedly");
            JsonNode response = mapper.readTree(bytes);
            if (response.has("error")) {
                int status = response.path("error").path("status").asInt(500);
                HttpStatus httpStatus = switch (status) {
                    case 400 -> HttpStatus.BAD_REQUEST;
                    case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
                    default -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
                throw new ResponseStatusException(httpStatus, response.path("error").path("message").asText("Calculation failed"));
            }
            return response;
        } catch (IOException | ExecutionException | TimeoutException e) {
            throw unavailable("Advanced calculation worker is unavailable");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Calculation interrupted");
        } finally {
            if (process != null && process.isAlive()) terminate(process);
            if (output != null && !output.isDone()) output.cancel(true);
            workers.release();
        }
    }

    private static void terminate(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @PreDestroy
    public void close() {
        readers.close();
    }
}
