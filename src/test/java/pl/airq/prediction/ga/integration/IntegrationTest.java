package pl.airq.prediction.ga.integration;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.mutiny.pgclient.PgPool;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.mockito.verification.Timeout;
import pl.airq.common.domain.DataProvider;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.domain.prediction.PredictionConfig;
import pl.airq.common.infrastructure.query.AirqPhenotypeQueryPostgres;
import pl.airq.common.infrastructure.query.EnrichedDataQueryPostgres;
import pl.airq.common.infrastructure.query.PredictionQueryPostgres;
import pl.airq.common.kafka.AirqEventSerializer;
import pl.airq.common.kafka.SKeySerializer;
import pl.airq.common.kafka.TSKeySerializer;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.Key;
import pl.airq.common.store.key.SKey;
import pl.airq.common.store.key.TSKey;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.domain.PredictionSubjectPublicAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pl.airq.prediction.ga.integration.DBConstant.CREATE_PREDICTION_TABLE;
import static pl.airq.prediction.ga.integration.DBConstant.DROP_PREDICTION_TABLE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTestResource(KafkaResource.class)
@QuarkusTestResource(PostgresResource.class)
@QuarkusTest
public class IntegrationTest {

    private static final AtomicInteger STATION_NUMBER = new AtomicInteger(0);
    private static final Float DATA_TEMP = 3f;
    private static final Float DATA_WIND = 4f;
    private static final Float PHENOTYPE_TEMP = 2f;
    private static final Float PHENOTYPE_WIND = 2f;

    private final List<Prediction> predictionList = new CopyOnWriteArrayList<>();

    @InjectMock
    AirqPhenotypeQueryPostgres phenotypeQuery;
    @InjectMock
    EnrichedDataQueryPostgres enrichedDataQuery;

    @InjectSpy
    PredictionQueryPostgres predictionQuery;

    @Inject
    PgPool client;
    @Inject
    KafkaProducer<TSKey, AirqEvent<EnrichedDataEventPayload>> enrichedDataProducer;
    @Inject
    KafkaProducer<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> airqPhenotypeProducer;

    @Inject
    PredictionSubjectPublicAdapter predictionSubject;
    @Inject
    Cache<StationId, Prediction> predictionCache;
    @Inject
    Cache<StationId, EnrichedData> enrichedDataCache;
    @Inject
    Cache<StationId, AirqPhenotype> airqPhenotypeCache;

    @ConfigProperty(name = "mp.messaging.incoming.data-enriched.topic")
    String enrichedDataTopic;
    @ConfigProperty(name = "mp.messaging.incoming.airq-phenotype-created.topic")
    String airqPhenotypeTopic;

    @BeforeEach
    void beforeEach() {
        recreatePredictionTable();

        predictionCache.clearBlocking();
        enrichedDataCache.clearBlocking();
        airqPhenotypeCache.clearBlocking();
        predictionList.clear();

        when(phenotypeQuery.findLatestByStationId(any(StationId.class))).thenReturn(Uni.createFrom().nullItem());
        when(enrichedDataQuery.findLatestByStation(any(String.class))).thenReturn(Uni.createFrom().nullItem());
    }

    @AfterAll
    void clear() {
        enrichedDataProducer.close();
        airqPhenotypeProducer.close();
    }

    @Test
    void EnrichedDataCreatedAndAirqPhenotypeArrived_withNoEnrichedDataAndPhenotypeInCache_expectPrediction() {
        final StationId stationId = stationId();
        final OffsetDateTime timestamp = OffsetDateTime.now();
        final AirqPhenotypeCreatedEvent airqPhenotypeCreatedEvent = airqPhenotypeCreated(stationId);
        final EnrichedDataCreatedEvent enrichedDataCreatedEvent = enrichedDataCreated(stationId, timestamp);
        Double expectedPredictionValue = (double) (DATA_TEMP * PHENOTYPE_TEMP + DATA_WIND * PHENOTYPE_WIND);

        Cancellable subscription = predictionSubject.stream(stationId).subscribe().with(predictionList::add);
        sendEvent(airqPhenotypeCreatedEvent);
        sleep(Duration.ofSeconds(1));
        sendEvent(enrichedDataCreatedEvent);

        await().atMost(Duration.ofSeconds(20)).until(() -> Objects.nonNull(airqPhenotypeCache.getBlocking(stationId)));
        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(enrichedDataCache.getBlocking(stationId)));
        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(predictionCache.getBlocking(stationId)));

        final Prediction result = predictionCache.getBlocking(stationId);

        assertThat(result.value).isEqualTo(expectedPredictionValue);
        assertThat(result.stationId).isEqualTo(stationId);
        assertThat(predictionList).containsOnly(result);
        verifyPredictionCount(1, stationId);
        subscription.cancel();
    }

    @Test
    void EnrichedDataCreatedArrived_withNoAirqPhenotypeInCache_expectNoPrediction() {
        final StationId stationId = stationId();
        final OffsetDateTime timestamp = OffsetDateTime.now();
        final EnrichedDataCreatedEvent enrichedDataCreatedEvent = enrichedDataCreated(stationId, timestamp);
        // additional call to phenotypeQuery
        assertThat(airqPhenotypeCache.getBlocking(stationId)).isNull();

        Cancellable subscription = predictionSubject.stream(stationId).subscribe().with(predictionList::add);
        sendEvent(enrichedDataCreatedEvent);

        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(enrichedDataCache.getBlocking(stationId)));

        verify(phenotypeQuery, new Timeout(Duration.ofSeconds(2).toMillis(), times(2))).findLatestByStationId(stationId);
        assertThat(predictionList).isEmpty();
        verifyPredictionCount(0, stationId);
        subscription.cancel();
    }

    @Test
    void EnrichedDataSend_withAirqPhenotypeInCache_expectPrediction() {
        final StationId stationId = stationId();
        final OffsetDateTime timestamp = OffsetDateTime.now();
        final EnrichedDataCreatedEvent enrichedDataCreatedEvent = enrichedDataCreated(stationId, timestamp);
        final AirqPhenotype phenotype = airqPhenotypeCreated(stationId).payload.airqPhenotype;
        // every check increase query invocation
        assertNull(airqPhenotypeCache.getBlocking(stationId));
        assertNull(enrichedDataCache.getBlocking(stationId));
        assertNull(predictionCache.getBlocking(stationId));
        when(phenotypeQuery.findLatestByStationId(stationId)).thenReturn(Uni.createFrom().item(phenotype));

        Cancellable subscription = predictionSubject.stream(stationId).subscribe().with(predictionList::add);
        sendEvent(enrichedDataCreatedEvent);

        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(enrichedDataCache.getBlocking(stationId)));
        verify(phenotypeQuery, new Timeout(Duration.ofSeconds(2).toMillis(), times(2))).findLatestByStationId(stationId);
        await().atMost(Duration.ofSeconds(10)).until(() -> Objects.nonNull(predictionCache.getBlocking(stationId)));

        final Prediction result = predictionCache.getBlocking(stationId);

        assertThat(predictionList).containsOnly(result);
        verifyPredictionCount(1, stationId);
        subscription.cancel();
    }

    private void verifyPredictionCount(int value, StationId stationId) {
        Set<Prediction> data = predictionQuery.findAll(stationId).await().atMost(Duration.ofSeconds(2));
        assertThat(data).hasSize(value);
    }

    private void recreatePredictionTable() {
        client.query(DROP_PREDICTION_TABLE).execute()
              .flatMap(r -> client.query(CREATE_PREDICTION_TABLE).execute())
              .await().atMost(Duration.ofSeconds(5));
    }

    private StationId stationId() {
        return StationId.from("Station" + STATION_NUMBER.incrementAndGet());
    }

    private AirqPhenotypeCreatedEvent airqPhenotypeCreated(StationId stationId) {
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

    private EnrichedDataCreatedEvent enrichedDataCreated(StationId stationId, OffsetDateTime timestamp) {
        return new EnrichedDataCreatedEvent(OffsetDateTime.now(), new EnrichedDataEventPayload(enrichedData(stationId, timestamp)));
    }

    private EnrichedDataUpdatedEvent enrichedDataUpdated(StationId stationId, OffsetDateTime timestamp) {
        return new EnrichedDataUpdatedEvent(OffsetDateTime.now(), new EnrichedDataEventPayload(enrichedData(stationId, timestamp)));
    }

    private EnrichedDataDeletedEvent enrichedDataDeleted(StationId stationId, OffsetDateTime timestamp) {
        return new EnrichedDataDeletedEvent(OffsetDateTime.now(), new EnrichedDataEventPayload(enrichedData(stationId, timestamp)));
    }

    private EnrichedData enrichedData(StationId stationId, OffsetDateTime timestamp) {
        return new EnrichedData(
                timestamp,
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
                stationId
        );
    }

    @SuppressWarnings("unchecked")
    private Key sendEvent(AirqEvent<?> event) {
        if (event.payload instanceof EnrichedDataEventPayload) {
            return sendEnrichedDataEvent((AirqEvent<EnrichedDataEventPayload>) event);
        }
        if (event.payload instanceof AirqPhenotypeCreatedPayload) {
            return sendAirqPhenotypeEvent((AirqEvent<AirqPhenotypeCreatedPayload>) event);
        }

        throw new RuntimeException("Invalid event payload type: " + event.eventType());
    }

    private TSKey sendEnrichedDataEvent(AirqEvent<EnrichedDataEventPayload> event) {
        EnrichedData enrichedData = event.payload.enrichedData;
        TSKey key = TSKey.from(enrichedData.timestamp, enrichedData.station.value());
        final Future<RecordMetadata> future = enrichedDataProducer.send(new ProducerRecord<>(enrichedDataTopic, key, event));
        try {
            future.get(5, TimeUnit.SECONDS);
            return key;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private SKey sendAirqPhenotypeEvent(AirqEvent<AirqPhenotypeCreatedPayload> event) {
        AirqPhenotype airqPhenotype = event.payload.airqPhenotype;
        SKey key = new SKey(airqPhenotype.stationId);
        final Future<RecordMetadata> future = airqPhenotypeProducer.send(new ProducerRecord<>(airqPhenotypeTopic, key, event));
        try {
            future.get(5, TimeUnit.SECONDS);
            return key;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignore) {
        }
    }

    @Dependent
    static class KafkaClientConfiguration {

        @ConfigProperty(name = "kafka.bootstrap.servers")
        String bootstrapServers;

        @Inject
        EventParser parser;

        @Produces
        KafkaProducer<TSKey, AirqEvent<EnrichedDataEventPayload>> enrichedDataProducer() {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);

            return new KafkaProducer<>(properties, new TSKeySerializer(), new AirqEventSerializer<>(parser));
        }

        @Produces
        KafkaProducer<SKey, AirqEvent<AirqPhenotypeCreatedPayload>> airqPhenotypeProducer() {
            Properties properties = new Properties();
            properties.put("bootstrap.servers", bootstrapServers);

            return new KafkaProducer<>(properties, new SKeySerializer(), new AirqEventSerializer<>(parser));
        }

    }
}
