package com.astro.service;

import com.astro.model.GeocodingRequest;
import com.astro.util.LimitedHttpBody;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
public class GeocodingService implements AutoCloseable {
    private record Cached(ArrayNode candidates, long expires) {}
    private final ObjectMapper mapper;
    private final AdvancedEngineService engine;
    private final String baseUrl;
    private final int timeoutSeconds;
    private final Map<GeocodingRequest, Cached> cache = new LinkedHashMap<>(16, 0.75f, true);
    private final Semaphore capacity = new Semaphore(2);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector() {
            public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
            public void connectFailed(URI uri, SocketAddress address, IOException error) { }
        }).build();

    public GeocodingService(ObjectMapper mapper, AdvancedEngineService engine,
            @Value("${astro.geocoding.base-url:}") String baseUrl,
            @Value("${astro.geocoding.timeout-seconds:10}") int timeoutSeconds) {
        this.mapper = mapper;
        this.engine = engine;
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    public JsonNode search(GeocodingRequest request) {
        GeocodingRequest query = normalize(request);
        URI endpoint = endpoint(query);
        synchronized (cache) {
            Cached saved = cache.get(query);
            if (saved != null && saved.expires() > System.nanoTime()) return response(query, saved.candidates().deepCopy(), true);
            cache.remove(query);
        }
        if (!capacity.tryAcquire()) throw unavailable("Location search is busy; retry later");
        try {
            ArrayNode candidates = parse(fetch(endpoint), query);
            if (!candidates.isEmpty()) resolveTimezones(candidates);
            boolean complete = true;
            for (JsonNode candidate : candidates) complete &= "resolved".equals(candidate.path("timezoneStatus").asText());
            if (complete) {
                synchronized (cache) {
                    if (cache.size() >= 256) cache.remove(cache.keySet().iterator().next());
                    cache.put(query, new Cached(candidates.deepCopy(), System.nanoTime() + Duration.ofHours(24).toNanos()));
                }
            }
            return response(query, candidates, false);
        } finally {
            capacity.release();
        }
    }

    private GeocodingRequest normalize(GeocodingRequest request) {
        if (request == null) throw new IllegalArgumentException("Location search body is required");
        String city = input(request.city(), "city", true);
        String state = input(request.state(), "state", false);
        String country = input(request.country(), "country", true);
        String code = request.countryCode();
        if (code != null && !code.matches("[A-Za-z]{2}")) throw new IllegalArgumentException("countryCode must be a two-letter country code");
        return new GeocodingRequest(city, state, country, code == null ? null : code.toLowerCase(Locale.ROOT));
    }

    private String input(String value, String name, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) throw new IllegalArgumentException(name + " is required");
            return null;
        }
        if (value.length() > 160 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(name + " must contain at most 160 characters and no control characters");
        }
        return value.strip();
    }

    private URI endpoint(GeocodingRequest query) {
        if (baseUrl == null || baseUrl.isBlank()) throw unavailable("Self-hosted geocoding is not configured; set ASTRO_GEOCODER_URL");
        try {
            URI base = URI.create(baseUrl);
            if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme())) || base.getHost() == null
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || timeoutSeconds < 1 || timeoutSeconds > 30
                    || base.getHost().toLowerCase(Locale.ROOT).replaceAll("\\.$", "").equals("nominatim.openstreetmap.org")) {
                throw unavailable("Configure your own HTTP(S) Nominatim server; the public OSM server is not enabled");
            }
            Map<String, String> params = new LinkedHashMap<>();
            params.put("city", query.city());
            if (query.state() != null) params.put("state", query.state());
            params.put("country", query.country());
            if (query.countryCode() != null) params.put("countrycodes", query.countryCode());
            params.put("format", "jsonv2");
            params.put("addressdetails", "1");
            params.put("accept-language", "en");
            params.put("limit", "5");
            params.put("dedupe", "1");
            StringJoiner encoded = new StringJoiner("&");
            params.forEach((k, v) -> encoded.add(k + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8)));
            String prefix = base.toASCIIString();
            return URI.create(prefix + (prefix.endsWith("/") ? "" : "/") + "search?" + encoded);
        } catch (IllegalArgumentException e) {
            throw unavailable("Invalid self-hosted geocoder configuration");
        }
    }

    private JsonNode fetch(URI endpoint) {
        CompletableFuture<HttpResponse<byte[]>> pending = null;
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", "application/json").header("User-Agent", "astro-backend/1.0 (self-hosted geocoding)").GET().build();
            pending = http.sendAsync(request, ignored -> new LimitedHttpBody(1024 * 1024));
            HttpResponse<byte[]> response = pending.get(timeoutSeconds, TimeUnit.SECONDS);
            if (response.statusCode() == 429 || response.statusCode() >= 500) throw unavailable("Self-hosted geocoder is busy or unavailable");
            if (response.statusCode() != 200) throw invalid("Self-hosted geocoder rejected the search");
            return mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(response.body());
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Location search exceeded its time limit");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Location search interrupted");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpTimeoutException) throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Location search exceeded its time limit");
            throw unavailable("Self-hosted geocoder is unavailable or returned an oversized response");
        } catch (IOException e) {
            throw invalid("Self-hosted geocoder returned unreadable data");
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
    }

    private ArrayNode parse(JsonNode rows, GeocodingRequest query) {
        if (rows == null || !rows.isArray() || rows.size() > 5) throw invalid("Invalid geocoder result list");
        ArrayNode candidates = mapper.createArrayNode();
        Set<String> seen = new HashSet<>();
        for (JsonNode row : rows) {
            String name = row.path("display_name").asText();
            String osmType = row.path("osm_type").asText();
            if (!row.isObject() || name.isBlank() || name.length() > 2000 || !Set.of("node", "way", "relation").contains(osmType)
                    || !row.path("osm_id").isIntegralNumber() || !row.path("osm_id").canConvertToLong() || row.path("osm_id").asLong() < 1) {
                throw invalid("Invalid geocoder place record");
            }
            double lat = coordinate(row.path("lat"), -90, 90);
            double lon = coordinate(row.path("lon"), -180, 180);
            JsonNode address = row.path("address");
            String code = address.path("country_code").asText().toLowerCase(Locale.ROOT);
            if (query.countryCode() != null && !query.countryCode().equals(code)) throw invalid("Geocoder result does not match the requested country code");
            String id = osmType + ":" + row.path("osm_id").asText();
            if (!seen.add(id)) continue;
            ObjectNode candidate = candidates.addObject().put("id", id).put("displayName", name)
                .put("latitude", lat).put("longitude", lon).put("osmType", osmType).put("osmId", row.path("osm_id").asLong())
                .put("matchType", row.path("addresstype").asText(row.path("type").asText("unknown")))
                .put("coordinatePrecision", "Representative place point, not an exact birthplace");
            candidate.putNull("timezone");
            candidate.put("timezoneStatus", "unavailable");
            copyAddress(candidate, address, "city", "city", "town", "village", "hamlet", "municipality");
            copyAddress(candidate, address, "state", "state", "province", "region");
            copyAddress(candidate, address, "country", "country");
            if (!code.isEmpty()) candidate.put("countryCode", code);
            JsonNode box = row.path("boundingbox");
            if (box.isArray() && box.size() == 4) {
                candidate.putObject("boundingBox").put("south", coordinate(box.get(0), -90, 90))
                    .put("north", coordinate(box.get(1), -90, 90)).put("west", coordinate(box.get(2), -180, 180))
                    .put("east", coordinate(box.get(3), -180, 180));
            }
        }
        return candidates;
    }

    private void copyAddress(ObjectNode candidate, JsonNode address, String name, String... keys) {
        for (String key : keys) {
            if (address.path(key).isTextual() && !address.path(key).asText().isBlank()) {
                String value = address.path(key).asText();
                if (value.length() > 500) throw invalid("Invalid geocoder address component");
                candidate.put(name, value);
                return;
            }
        }
    }

    private double coordinate(JsonNode value, double min, double max) {
        try {
            if (value == null || !(value.isNumber() || value.isTextual())) throw invalid("Invalid geocoder coordinate");
            double number = Double.parseDouble(value.asText());
            if (!Double.isFinite(number) || number < min || number > max) throw invalid("Invalid geocoder coordinate");
            return number;
        } catch (NumberFormatException e) {
            throw invalid("Invalid geocoder coordinate");
        }
    }

    private void resolveTimezones(ArrayNode candidates) {
        ArrayNode points = mapper.createArrayNode();
        for (JsonNode candidate : candidates) points.addObject().put("latitude", candidate.path("latitude").asDouble())
            .put("longitude", candidate.path("longitude").asDouble());
        JsonNode resolved;
        try {
            resolved = engine.resolveTimezones(points);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() != 503 && e.getStatusCode().value() != 504) throw e;
            return;
        }
        JsonNode zones = resolved.path("timezones");
        if (!"complete".equals(resolved.path("status").asText()) || !zones.isArray() || zones.size() != candidates.size()) {
            throw invalid("Offline timezone resolver returned invalid data");
        }
        for (int i = 0; i < candidates.size(); i++) {
            ObjectNode candidate = (ObjectNode) candidates.get(i);
            JsonNode zone = zones.get(i);
            if (zone.isNull()) candidate.put("timezoneStatus", "not_found");
            else {
                if (!zone.isTextual() || zone.asText().isBlank() || zone.asText().length() > 100) throw invalid("Invalid timezone result");
                candidate.put("timezone", zone.asText()).put("timezoneStatus", "resolved");
            }
        }
    }

    private JsonNode response(GeocodingRequest query, ArrayNode candidates, boolean cached) {
        ObjectNode result = mapper.createObjectNode();
        boolean complete = true;
        for (JsonNode c : candidates) complete &= "resolved".equals(c.path("timezoneStatus").asText());
        result.put("status", complete ? "complete" : "partial").put("provider", "self-hosted-nominatim")
            .put("selectionRequired", !candidates.isEmpty()).put("cacheHit", cached).put("matchCount", candidates.size());
        result.set("query", mapper.valueToTree(query));
        result.set("candidates", candidates);
        result.put("attribution", "OpenStreetMap contributors").put("dataLicense", "ODbL")
            .put("attributionUrl", "https://www.openstreetmap.org/copyright");
        ArrayNode warnings = result.putArray("warnings");
        warnings.add("Confirm city, state and country before using a result. City coordinates are representative points; use an exact birthplace pin when known.");
        warnings.add("Timezone lookup uses current geographic boundaries. Birth-date offsets are computed separately with IANA timezone rules.");
        if (candidates.isEmpty()) warnings.add("No match in the configured dataset. Check spelling, administrative divisions and imported coverage.");
        if (!complete) warnings.add("Some timezones could not be resolved. Supply a verified IANA timezone before calculating a chart; no default zone was guessed.");
        return result;
    }

    private static ResponseStatusException unavailable(String reason) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason); }
    private static ResponseStatusException invalid(String reason) { return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason); }

    @Override
    @PreDestroy
    public void close() { http.shutdownNow(); }
}
