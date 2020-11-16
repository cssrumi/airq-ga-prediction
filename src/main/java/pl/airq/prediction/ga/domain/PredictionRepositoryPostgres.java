package pl.airq.prediction.ga.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres;

import static pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres.Default.NEVER_EXIST_QUERY;
import static pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres.Default.NEVER_EXIST_TUPLE;

@Singleton
public class PredictionRepositoryPostgres extends PersistentRepositoryPostgres<Prediction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionRepositoryPostgres.class);
    static final String INSERT_QUERY = "INSERT INTO PREDICTION (\"timestamp\", value, config, stationid) VALUES ($1, $2, $3, $4)";

    private final ObjectMapper mapper;

    @Inject
    public PredictionRepositoryPostgres(PgPool client, ObjectMapper mapper) {
        super(client);
        this.mapper = mapper;
    }

    @PostConstruct
    void postConstruct() {
        LOGGER.info("{} initialized...", PredictionRepositoryPostgres.class.getSimpleName());
    }

    @Override
    protected String insertQuery() {
        return INSERT_QUERY;
    }

    @Override
    protected String updateQuery() {
        return INSERT_QUERY;
    }

    @Override
    protected String isAlreadyExistQuery() {
        return NEVER_EXIST_QUERY;
    }

    @Override
    protected Tuple insertTuple(Prediction data) {
        String config = null;
        try {
            config = mapper.writeValueAsString(data.config);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to stringify Prediction: {}", data);
        }

        return Tuple.of(data.timestamp)
                    .addDouble(data.value)
                    .addString(config)
                    .addString(data.stationId.value());
    }

    @Override
    protected Tuple updateTuple(Prediction data) {
        return insertTuple(data);
    }

    @Override
    protected Tuple isAlreadyExistTuple(Prediction data) {
        return NEVER_EXIST_TUPLE;
    }

    @Override
    protected Uni<Void> postProcessAction(Result result, Prediction data) {
        return Uni.createFrom().voidItem()
                  .invoke(() -> logResult(result, data));
    }


    private void logResult(Result result, Prediction data) {
        if (result.isSuccess()) {
            LOGGER.info("Prediction has been {}.", result);
            return;
        }

        LOGGER.warn("Insertion result: {} for {}", result, data);
    }
}
