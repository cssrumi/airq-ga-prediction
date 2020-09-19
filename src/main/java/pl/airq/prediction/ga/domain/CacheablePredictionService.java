package pl.airq.prediction.ga.domain;

import javax.enterprise.context.ApplicationScoped;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.prediction.Prediction;

@ApplicationScoped
public class CacheablePredictionService implements PredictionService {

    @Override
    public Prediction predict(EnrichedData data, AirqPhenotype phenotype) {
        return null;
    }
}
