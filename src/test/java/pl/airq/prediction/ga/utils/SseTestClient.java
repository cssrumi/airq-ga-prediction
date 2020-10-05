package pl.airq.prediction.ga.utils;

import io.restassured.RestAssured;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseTestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseTestClient.class);
    private List<String> collectedEvents = new CopyOnWriteArrayList<>();
    private final String targetUrl;
    private Runnable eventEmitter;

    public SseTestClient(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public static SseTestClient fromUri(String uri, String... objects) {
        String finalUri = uri;
        for (String object : objects) {
            finalUri = uri.replaceFirst("\\{(.*?)}", object);
        }
        return new SseTestClient("http://localhost:" + RestAssured.port + finalUri);
    }

    public SseTestClient setEventEmitter(Runnable eventEmitter) {
        this.eventEmitter = eventEmitter;
        return this;
    }

    public SseTestClient runFor(Duration duration) {
        collectedEvents.clear();
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(targetUrl);
        SseEventSource source = SseEventSource.target(target)
                                              .reconnectingEvery(60, TimeUnit.SECONDS)
                                              .build();
        source.register(this::collectEvent, this::handleError);
        source.open();
        sleep(Duration.ofMillis(100));
        eventEmitter.run();
        sleep(duration);
        source.close();
        client.close();
        return this;
    }

    public List<String> getCollectedEvents() {
        return collectedEvents;
    }

    private void collectEvent(InboundSseEvent inboundSseEvent) {
        final String rawEvent = inboundSseEvent.readData();
        collectedEvents.add(rawEvent);
        LOGGER.info("Collected event: {}", rawEvent);
    }

    private void handleError(Throwable error) {
        LOGGER.warn("Error occurred during handling events.", error);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
