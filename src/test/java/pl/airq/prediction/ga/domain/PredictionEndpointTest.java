package pl.airq.prediction.ga.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.domain.prediction.PredictionConfig;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.process.enriched.EnrichedDataConsumer;
import pl.airq.prediction.ga.process.phenotype.AirqPhenotypeCreatedConsumer;
import pl.airq.prediction.ga.utils.SseTestClient;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PredictionEndpointTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionEndpointTest.class);
    private static final AtomicInteger CURRENT_PREDICTION_VALUE = new AtomicInteger(0);
    private static final String FIND_LATEST_URI = "/api/prediction/{stationId}";
    private static final String STREAM_URI = "/api/prediction/{stationId}/stream";

    @InjectMock
    EnrichedDataConsumer dataEnrichedEventConsumer;
    @InjectMock
    AirqPhenotypeCreatedConsumer phenotypeCreatedEventConsumer;
    @InjectMock
    PredictionFacade facade;

    @Inject
    ObjectMapper mapper;
    @Inject
    PredictionSubject subject;

    @Test
    void findLatest() {
        StationId stationId = StationId.from("station");
        Prediction prediction = prediction(stationId);
        when(facade.findPrediction(any())).thenReturn(Uni.createFrom().item(prediction));

        final Prediction result = get(FIND_LATEST_URI, stationId.value())
                .then()
                .statusCode(200)
                .extract().as(Prediction.class);

        assertNotNull(result);
        assertEquals(stationId, result.stationId);
    }

    @Test
    void stream_with5EventsEmitted_expect5Events() {
        StationId stationId = StationId.from("station");
        final List<String> events = SseTestClient.fromUri(STREAM_URI, stationId.value())
                                                 .setEventEmitter(() -> {
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                 })
                                                 .runFor(Duration.ofSeconds(1))
                                                 .getCollectedEvents();

        assertEquals(5, events.size());
    }

    @Test
    void stream_with1EmitBeforeAnd5InTheMiddleOfSubscription_expect5Events() {
        StationId stationId = StationId.from("station2");
        subject.emit(prediction(stationId));
        final List<String> events = SseTestClient.fromUri(STREAM_URI, stationId.value())
                                                 .setEventEmitter(() -> {
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                     emit(prediction(stationId), 0);
                                                 })
                                                 .runFor(Duration.ofSeconds(1))
                                                 .getCollectedEvents();

        assertEquals(5, events.size());
    }

    private void emit(Prediction prediction) {
        emit(prediction, 50);
    }

    private void emit(Prediction prediction, long delayInMillis) {
        subject.emit(prediction);
        if (delayInMillis > 0) {
            sleep(Duration.ofMillis(delayInMillis));
        }
    }

    private Prediction prediction(StationId stationId) {
        final int value = CURRENT_PREDICTION_VALUE.getAndIncrement();
        LOGGER.info("Prediction {} created.", value);
        return new Prediction(OffsetDateTime.now(), Integer.valueOf(value)
                                                           .doubleValue(), new PredictionConfig(1L, ChronoUnit.HOURS, "pm10"), stationId);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
