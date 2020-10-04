package pl.airq.prediction.ga.utils;

import io.restassured.RestAssured;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseTestClient2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(SseTestClient2.class);
    private final AtomicBoolean isEventSourceReady = new AtomicBoolean(false);
    private List<String> collectedEvents = new CopyOnWriteArrayList<>();
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final String targetUrl;
    private Runnable eventEmitter;

    public SseTestClient2(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public static SseTestClient2 fromUri(String uri, String... objects) {
        String finalUri = uri;
        for (String object : objects) {
            finalUri = uri.replaceFirst("\\{(.*?)}", object);
        }
        return new SseTestClient2("http://localhost:" + RestAssured.port + finalUri);
    }

    public SseTestClient2 setEventEmitter(Runnable eventEmitter) {
        this.eventEmitter = eventEmitter;
        return this;
    }

    public SseTestClient2 runFor(Duration duration) {
        collectedEvents.clear();
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(targetUrl);

        executorService.submit(() -> {
            SseEventSource source = SseEventSource.target(target)
                                                  .reconnectingEvery(60, TimeUnit.SECONDS)
                                                  .build();
            source.register(this::collectEvent, this::handleError);
            source.open();
            isEventSourceReady.set(true);
            sleep(duration);
            source.close();
        });

        Awaitility.await().untilTrue(isEventSourceReady);
        executorService.submit(eventEmitter);
        sleep(duration);
        client.close();
        return this;
    }

    public List<String> getCollectedEvents() {
        return collectedEvents;
    }

    private void collectEvent(InboundSseEvent inboundSseEvent) {
        LOGGER.info("Event received...");
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
