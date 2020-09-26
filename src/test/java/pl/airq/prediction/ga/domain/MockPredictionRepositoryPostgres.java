package pl.airq.prediction.ga.domain;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.prediction.Prediction;

@Mock
@ApplicationScoped
public class MockPredictionRepositoryPostgres extends PredictionRepositoryPostgres {

    private Boolean result = Boolean.TRUE;

    public MockPredictionRepositoryPostgres() {
        super(null, null);
    }

    @Override
    public Uni<Boolean> save(Prediction data) {
        return Uni.createFrom().item(result);
    }

    @Override
    public Uni<Boolean> upsert(Prediction data) {
        return Uni.createFrom().item(result);
    }

    public void setSaveAndUpsertResult(Boolean result) {
        this.result = result;
    }
}
