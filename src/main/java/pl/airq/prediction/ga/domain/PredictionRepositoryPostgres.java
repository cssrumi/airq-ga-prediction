package pl.airq.prediction.ga.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.infrastructure.persistance.PersistentRepositoryPostgres;

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

    @Override
    protected String insertQuery() {
        return INSERT_QUERY;
    }

    @Override
    protected Tuple prepareTuple(Prediction prediction) {
        String config = null;
        try {
            config = mapper.writeValueAsString(prediction.config);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Unable to stringify Prediction: {}", prediction);
        }

        return Tuple.of(prediction.timestamp)
                    .addDouble(prediction.value)
                    .addString(config)
                    .addString(prediction.stationId.getId());
    }

    @Override
    protected void postSaveAction(RowSet<Row> saveResult) {
    }

    @Override
    protected void postProcessAction(Boolean result, Prediction data) {
        if (Boolean.TRUE.equals(result)) {
            LOGGER.info("Prediction saved successfully.");
            return;
        }

        LOGGER.warn("Unable to save Prediction: {}", data);
    }
}
