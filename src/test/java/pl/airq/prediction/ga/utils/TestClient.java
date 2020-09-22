package pl.airq.prediction.ga.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestClient.class);
    private final String targetUtl;
    private ObjectMapper mapper;
    private String clientName = "TestClient";

    private TestClient(String targetUrl) {
        this.targetUtl = targetUrl;
        LOGGER.info("[{}] -> {} created", clientName, targetUrl);
    }

    public TestClient setMapper(ObjectMapper mapper) {
        this.mapper = mapper;
        return this;
    }

    public TestClient setClientName(String clientName) {
        this.clientName = clientName;
        return this;
    }

    public WebTarget target() {
        final Client client = ClientBuilder.newClient();
        return client.target(targetUtl);
    }

    public <T> T get(Class<T> clazz) {
        final WebTarget target = target();
        String result = (String) target.request().get().getEntity();
        if (result != null && mapper != null) {
            try {
                return mapper.readValue(result, clazz);
            } catch (JsonProcessingException e) {
                LOGGER.warn("Unable to parse {} into {}", result, clazz.getSimpleName());
                return null;
            }
        }
        LOGGER.info("Empty result or mapper. Default mapper used.");
        return target.request().get(clazz);
    }

    public SseTestClient intoSseClient() {
        return new SseTestClient(target()).setMapper(mapper);
    }

    public static TestClient fromUrl(String url) {
        return new TestClient(url);
    }

    public static TestClient fromUri(String uri, String... objects) {
        String finalUri = uri;
        for (String object : objects) {
            finalUri = uri.replaceFirst("\\{(.*?)}", object);
        }
        return new TestClient("http://localhost:" + RestAssured.port + finalUri);
    }

    public static final class SseTestClient {

        private static final Logger LOGGER = LoggerFactory.getLogger(SseTestClient.class);
        private final WebTarget target;
        private ObjectMapper mapper;
        private Runnable emitter;
        private List<String> rawEvents;

        private SseTestClient(WebTarget target) {
            this.target = target;
        }

        public SseTestClient setEmitter(Runnable emitter) {
            this.emitter = emitter;
            return this;
        }

        public SseTestClient setMapper(ObjectMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        public List<String> getRawEvents() {
            return rawEvents;
        }

        public <T> List<T> getEvents(Class<T> eventClass) {
            if (mapper == null) {
                throw new IllegalArgumentException("ObjectMapper is not set");
            }

            return rawEvents.stream()
                            .map(rawEvent -> {
                                try {
                                    return mapper.readValue(rawEvent, eventClass);
                                } catch (JsonProcessingException e) {
                                    LOGGER.warn("Unable to deserialize: {}", rawEvent);
                                    return null;
                                }
                            })
                            .collect(Collectors.toUnmodifiableList());
        }

        public int eventCount() {
            return rawEvents != null ? rawEvents.size() : 0;
        }

        public SseTestClient run() {
            try (SseEventSource eventSource = SseEventSource.target(target).build()) {
                Uni<List<String>> rawEventsUni = Uni.createFrom().emitter(uniEmitter -> {
                    List<String> rawEvents = new CopyOnWriteArrayList<>();
                    eventSource.register(sseEvent -> {
                        String rawEvent = sseEvent.readData();
                        rawEvents.add(rawEvent);
                        LOGGER.info("Event arrived: {}", rawEvent);
                    }, ex -> {
                        uniEmitter.fail(new IllegalStateException("SSE failure", ex));
                    });
                    eventSource.open();
                    if (emitter != null) {
                        emitter.run();
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    uniEmitter.complete(rawEvents);
                });
                rawEvents = rawEventsUni.await().atMost(Duration.ofSeconds(60));
            }
            return this;
        }
    }
}
