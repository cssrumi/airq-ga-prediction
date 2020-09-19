package pl.airq.prediction.ga.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.platform.commons.util.StringUtils;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import pl.airq.common.domain.DataProvider;
import pl.airq.common.domain.enriched.AirqDataEnrichedEvent;
import pl.airq.common.domain.enriched.AirqDataEnrichedPayload;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedPayload;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.domain.prediction.PredictionConfig;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.infrastructure.PredictionRepositoryPostgres;

import static org.awaitility.Awaitility.await;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTest
public class IntegrationTest {

    private static final Float DATA_TEMP = 3f;
    private static final Float DATA_WIND = 4f;
    private static final Float PHENOTYPE_TEMP = 2f;
    private static final Float PHENOTYPE_WIND = 2f;

    @ConfigProperty(name = "mp.messaging.incoming.data-enriched.topic")
    private String dataEnrichedTopic;
    @ConfigProperty(name = "mp.messaging.incoming.airq-phenotype-created.topic")
    private String airqPhenotypeCreatedTopic;
    @InjectMock
    private PredictionRepositoryPostgres repository;
    @Inject
    private KafkaProducer<Void, String> client;
    @Inject
    private Cache<StationId, Prediction> predictionCache;
    @Inject
    private Cache<StationId, EnrichedData> enrichedDataCache;
    @Inject
    private Cache<StationId, AirqPhenotype> airqPhenotypeCache;
    @Inject
    private EventParser parser;

    @BeforeEach
    void beforeEach() {
        predictionCache.clearBlocking();
        enrichedDataCache.clearBlocking();
        airqPhenotypeCache.clearBlocking();
    }

    @AfterAll
    void clear() {
        client.close();
    }

    @Test
    void validate_whenEnrichedDataAndAirqPhenotypeSend_expectPredictionInCache() {
        final StationId stationId = stationId();
        final AirqPhenotypeCreatedEvent airqPhenotypeCreatedEvent = airqPhenotypeCreatedEvent(stationId);
        final AirqDataEnrichedEvent airqDataEnrichedEvent = airqDataEnrichedEvent(stationId);

        sendEvent(airqPhenotypeCreatedEvent);
        sendEvent(airqDataEnrichedEvent);

        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(predictionCache.getBlocking(stationId)));
    }

    private StationId stationId() {
        return StationId.from(RandomStringUtils.randomAlphabetic(5));
    }

    private AirqPhenotypeCreatedEvent airqPhenotypeCreatedEvent(StationId stationId) {
        AirqPhenotype phenotype = new AirqPhenotype(
                OffsetDateTime.now(),
                stationId,
                List.of("temp", "wind"),
                List.of(PHENOTYPE_TEMP, PHENOTYPE_WIND),
                new PredictionConfig(2L, ChronoUnit.DAYS, "pm10"),
                null);
        AirqPhenotypeCreatedPayload payload = new AirqPhenotypeCreatedPayload(phenotype);

        return new AirqPhenotypeCreatedEvent(OffsetDateTime.now(), payload);
    }

    private AirqDataEnrichedEvent airqDataEnrichedEvent(StationId stationId) {
        EnrichedData data = new EnrichedData(
                OffsetDateTime.now(),
                null,
                null,
                DATA_TEMP,
                DATA_WIND,
                null,
                null,
                null,
                null,
                null,
                DataProvider.AIRQ,
                stationId);
        AirqDataEnrichedPayload payload = new AirqDataEnrichedPayload(data);

        return new AirqDataEnrichedEvent(OffsetDateTime.now(), payload);
    }

    private void sendEvent(AirqEvent<?> airqEvent) {
        final String rawEvent = parser.parse(airqEvent);
        String topic = getTopic(airqEvent);
        final Future<RecordMetadata> future = client.send(new ProducerRecord<>(topic, rawEvent));
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String getTopic(AirqEvent<?> airqEvent) {
        if (airqEvent.eventType().equals(AirqDataEnrichedEvent.class.getSimpleName())) {
            return dataEnrichedTopic;
        }
        if (airqEvent.eventType().equals(AirqPhenotypeCreatedEvent.class.getSimpleName())) {
            return airqPhenotypeCreatedTopic;
        }

        throw new RuntimeException("Invalid event: " + airqEvent.eventType());
    }

    @Dependent
    static class KafkaClientConfiguration {

        @Produces
        KafkaProducer<Void, String> stringKafkaProducer(@ConfigProperty(name = "kafka.bootstrap.servers") String bootstrapServers) {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            return new KafkaProducer<>(properties);
        }

    }
}
