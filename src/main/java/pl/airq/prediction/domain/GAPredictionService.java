package pl.airq.prediction.domain;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import pl.airq.prediction.model.event.enrichment.EnrichedData;

@ApplicationScoped
public class GAPredictionService implements PredictionService {

    @Override
    public Uni<Prediction> predict(EnrichedData enrichedData) {
        // TODO: Implement predict method
        return null;
    }

    @Override
    public Uni<Void> save(Prediction prediction) {
        // TODO: Implement save method
        return null;
    }
}
