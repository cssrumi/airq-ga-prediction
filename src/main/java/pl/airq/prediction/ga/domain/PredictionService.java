package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

interface PredictionService {

    Uni<Prediction> predict(StationId stationId);

    Prediction predict(AirqPhenotype phenotype, EnrichedData enrichedData);

}
