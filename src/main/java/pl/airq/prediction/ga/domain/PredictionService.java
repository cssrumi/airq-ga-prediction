package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.prediction.Prediction;

public interface PredictionService {

    Prediction predict(EnrichedData data, AirqPhenotype phenotype);

}
