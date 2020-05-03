package pl.airq.prediction.domain;

import io.smallrye.mutiny.Uni;
import pl.airq.prediction.model.event.enrichment.EnrichedData;

public interface PredictionService {

    Uni<Prediction> predict(EnrichedData enrichedData);

    Uni<Void> save(Prediction prediction);

}
