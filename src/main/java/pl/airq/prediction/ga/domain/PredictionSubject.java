package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;
import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@ApplicationScoped
class PredictionSubject {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionSubject.class);
    private final UnicastProcessor<Prediction> subject = UnicastProcessor.create();

    void emmit(Prediction prediction) {
        subject.onNext(prediction);
        LOGGER.info("New prediction has been emitted");
    }

    Multi<Prediction> stream() {
        return subject;
    }

    Multi<Prediction> stream(StationId stationId) {
        return stream().transform().byFilteringItemsWith(prediction -> Objects.equals(stationId, prediction.stationId));
    }
}
