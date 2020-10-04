package pl.airq.prediction.ga.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.domain.prediction.PredictionConfig;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.process.external.AirqDataEnrichedEventConsumer;
import pl.airq.prediction.ga.process.external.AirqPhenotypeCreatedEventConsumer;
import pl.airq.prediction.ga.utils.SseTestClient2;
import pl.airq.prediction.ga.utils.TestClient;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
public class PredictionEndpointTest {

    private static final String FIND_LATEST_URI = "/api/prediction/{stationId}";
    private static final String STREAM_URI = "/api/prediction/{stationId}/stream";

    @InjectMock
    AirqDataEnrichedEventConsumer dataEnrichedEventConsumer;
    @InjectMock
    AirqPhenotypeCreatedEventConsumer phenotypeCreatedEventConsumer;
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

        final Prediction result = get(FIND_LATEST_URI, stationId.getId())
                .then()
                .statusCode(200)
                .extract().as(Prediction.class);

        assertNotNull(result);
        assertEquals(stationId, result.stationId);
    }

    @Disabled
    @Test
    void stream() {
        StationId stationId = StationId.from("station");
        final List<String> events = SseTestClient2.fromUri(STREAM_URI, stationId.getId())
                                                  .setEventEmitter(() -> {
                                                      subject.emit(prediction(stationId));
                                                      subject.emit(prediction(stationId));
                                                      subject.emit(prediction(stationId));
                                                      subject.emit(prediction(stationId));
                                                      subject.emit(prediction(stationId));
                                                  })
                                                  .runFor(Duration.ofSeconds(5))
                                                  .getCollectedEvents();

        assertEquals(5, events.size());
//        assertEquals(5, sseClient.eventCount());
    }

    @Disabled
    @Test
    void stream_oldClient() {
        StationId stationId = StationId.from("station");
        final TestClient.SseTestClient sseClient = TestClient.fromUri(STREAM_URI, stationId.getId())
                                                             .setMapper(mapper)
                                                             .intoSseClient()
                                                             .setEmitter(() -> {
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                             })
                                                             .run();

        assertEquals(5, sseClient.eventCount());
    }

    @Disabled
    @Test
    void stream_with1EmitBeforeAnd5InTheMiddleOfSubscription_expect5Events() {
        StationId stationId = StationId.from("station2");
        subject.emit(prediction(stationId));
        final TestClient.SseTestClient sseClient = TestClient.fromUri(STREAM_URI, stationId.getId())
                                                             .setMapper(mapper)
                                                             .intoSseClient()
                                                             .setEmitter(() -> {
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                                 subject.emit(prediction(stationId));
                                                             })
//                                                             .awaitEvents(Duration.ofSeconds(2), 5)
                                                             .run();

        assertEquals(5, sseClient.eventCount());
    }

    private Prediction prediction(StationId stationId) {
        return new Prediction(OffsetDateTime.now(), 5.0, new PredictionConfig(1L, ChronoUnit.HOURS, "pm10"), stationId);
    }

}
