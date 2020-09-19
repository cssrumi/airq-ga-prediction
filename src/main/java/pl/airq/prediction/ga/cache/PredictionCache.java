package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.domain.prediction.PredictionQuery;
import pl.airq.common.vo.StationId;

@ApplicationScoped
public class PredictionCache extends CacheWithFallback<StationId, Prediction> {

    private final PredictionQuery query;

    @Inject
    public PredictionCache(PredictionQuery query) {
        this.query = query;
    }

    @Override
    Uni<Prediction> findLatest(StationId key) {
        return query.findLatest(key);
    }
}
