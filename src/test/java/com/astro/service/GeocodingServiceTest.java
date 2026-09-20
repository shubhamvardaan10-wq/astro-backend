package com.astro.service;

import com.astro.model.GeocodingRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeocodingServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private ExecutorService executor;
    private AdvancedEngineService engine;
    private GeocodingService service;
    private String url;
    private final GeocodingRequest query = new GeocodingRequest("Munger", "Bihar", "India", "IN");
    private static final String PLACE = """
        [{"osm_type":"relation","osm_id":1234,"display_name":"Munger, Bihar, India",
          "lat":"25.381","lon":"86.465","addresstype":"city",
          "address":{"city":"Munger","state":"Bihar","country":"India","country_code":"in"},
          "boundingbox":["25.3","25.5","86.4","86.6"]}]
        """;

    @BeforeEach
    void setup() throws Exception {
        engine = mock(AdvancedEngineService.class);
        when(engine.resolveTimezones(any())).thenReturn(mapper.readTree("{\"status\":\"complete\",\"timezones\":[\"Asia/Kolkata\"]}"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
        service = new GeocodingService(mapper, engine, url, 3);
    }

    @AfterEach
    void cleanup() {
        service.close();
        server.stop(0);
        executor.close();
    }

    private void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
    }

    @Test
    void structuredSearchReturnsCandidatesAndCachesWithoutChoosingOne() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> params = new AtomicReference<>();
        server.createContext("/search", e -> {
            calls.incrementAndGet();
            params.set(e.getRequestURI().getRawQuery());
            assertThat(e.getRequestHeaders().getFirst("User-Agent")).contains("astro-backend");
            reply(e, 200, PLACE);
        });
        var result = service.search(query);
        assertThat(params.get()).contains("city=Munger", "state=Bihar", "country=India", "countrycodes=in", "format=jsonv2", "addressdetails=1", "limit=5");
        assertThat(params.get()).doesNotContain("dob=", "time=", "q=");
        assertThat(result.path("selectionRequired").asBoolean()).isTrue();
        assertThat(result.at("/candidates/0/latitude").asDouble()).isEqualTo(25.381);
        assertThat(result.at("/candidates/0/longitude").asDouble()).isEqualTo(86.465);
        assertThat(result.at("/candidates/0/timezone").asText()).isEqualTo("Asia/Kolkata");
        assertThat(result.path("attribution").asText()).contains("OpenStreetMap");
        ((com.fasterxml.jackson.databind.node.ObjectNode) result.at("/candidates/0")).put("latitude", 0);
        assertThat(service.search(query).at("/candidates/0/latitude").asDouble()).isEqualTo(25.381);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void handlesUnicodeAndOptionalStateWithUrlEncoding() {
        AtomicReference<String> params = new AtomicReference<>();
        server.createContext("/search", e -> { params.set(e.getRequestURI().getRawQuery()); reply(e, 200, "[]"); });
        var result = service.search(new GeocodingRequest("東京 & q=unexpected", null, "日本", "JP"));
        assertThat(params.get()).contains("%E6%9D%B1", "%26", "countrycodes=jp").doesNotContain("&q=", "state=");
        assertThat(result.path("candidates").isEmpty()).isTrue();
        verifyNoInteractions(engine);
    }

    @Test
    void partialTimezoneFailureNeverDefaultsToIndia() {
        server.createContext("/search", e -> reply(e, 200, PLACE));
        when(engine.resolveTimezones(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Worker unavailable"));
        var result = service.search(query);
        assertThat(result.path("status").asText()).isEqualTo("partial");
        assertThat(result.at("/candidates/0/timezone").isNull()).isTrue();
        assertThat(result.at("/candidates/0/timezoneStatus").asText()).isEqualTo("unavailable");
        assertThat(result.at("/candidates/0/latitude").asDouble()).isEqualTo(25.381);
    }

    @Test
    void rejectsMissingConfigurationAndPublicServerWithoutNetwork() {
        for (String base : new String[]{"", "https://nominatim.openstreetmap.org", "file:///tmp/data", url + "?redirect=elsewhere"}) {
            try (GeocodingService bad = new GeocodingService(mapper, engine, base, 3)) {
                assertThatThrownBy(() -> bad.search(query)).isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
            }
        }
        verifyNoInteractions(engine);
    }

    @Test
    void rejectsMalformedOutOfRangeAndWrongCountryResults() {
        for (String body : new String[]{"{}", "null", PLACE.replace("25.381", "NaN"), PLACE.replace("25.381", "91"), PLACE.replace("\"in\"", "\"gb\"")}) {
            server.createContext("/search", e -> reply(e, 200, body));
            try {
                assertThatThrownBy(() -> service.search(query)).isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
            } finally { server.removeContext("/search"); }
        }
    }

    @Test
    void doesNotFollowRedirectsOrExposeProviderErrors() {
        server.createContext("/search", e -> {
            e.getResponseHeaders().add("Location", "https://example.com");
            reply(e, 302, "private-provider-detail");
        });
        assertThatThrownBy(() -> service.search(query)).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getReason()).doesNotContain("private-provider-detail"));
        verifyNoInteractions(engine);
    }

    @Test
    void boundsProviderResponseSizeAndTimeout() {
        server.createContext("/search", e -> reply(e, 200, "x".repeat(1_100_000)));
        assertThatThrownBy(() -> service.search(query)).isInstanceOf(ResponseStatusException.class);
        server.removeContext("/search");
        server.createContext("/search", e -> {
            try { Thread.sleep(1500); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            try { reply(e, 200, "[]"); } catch (IOException disconnected) { e.close(); }
        });
        try (GeocodingService limited = new GeocodingService(mapper, engine, url, 1)) {
            assertThatThrownBy(() -> limited.search(query)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(504));
        }
    }

    @Test
    void inputValidationPrecedesExternalCalls() {
        for (GeocodingRequest invalid : new GeocodingRequest[]{
                new GeocodingRequest(" ", null, "India", null), new GeocodingRequest("Munger", null, null, null),
                new GeocodingRequest("Munger", null, "India", "IND"), new GeocodingRequest("x".repeat(161), null, "India", null)}) {
            assertThatThrownBy(() -> service.search(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(engine);
    }
}
