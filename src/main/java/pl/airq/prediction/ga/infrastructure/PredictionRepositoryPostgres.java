package pl.airq.prediction.ga.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.prediction.Prediction;

@ApplicationScoped
public class PredictionRepositoryPostgres implements PersistentRepository<Prediction> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionRepositoryPostgres.class);
    static final String INSERT_QUERY = "INSERT INTO PREDICTION (\"timestamp\", value, config, stationid) VALUES ($1, $2, $3, $4)";

    private final PgPool client;
    private final ObjectMapper mapper;

    @Inject
    public PredictionRepositoryPostgres(PgPool client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public Uni<Boolean> save(Prediction data) {
        return client.preparedQuery(INSERT_QUERY)
                     .execute(preparePredictionTuple(data))
                     .onItem()
                     .transform(result -> {
                         if (result.rowCount() != 0) {
                             LOGGER.debug("EnrichedData saved successfully.");
                             return true;
                         }

                         LOGGER.warn("Unable to save EnrichedData: " + data);
                         return false;
                     });
    }

    @Override
    public Uni<Boolean> upsert(Prediction data) {
        return save(data);
    }

    private Tuple preparePredictionTuple(Prediction prediction) {
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
}
