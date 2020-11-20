package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Multi;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@ApplicationScoped
public class PredictionSubjectPublicAdapter {

    private final PredictionSubject subject;

    @Inject
    public PredictionSubjectPublicAdapter(PredictionSubject subject) {
        this.subject = subject;
    }

    public Multi<Prediction> stream(StationId stationId) {
        return subject.stream(stationId);
    }
}
