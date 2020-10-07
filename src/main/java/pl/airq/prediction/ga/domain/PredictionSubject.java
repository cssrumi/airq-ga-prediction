package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@ApplicationScoped
class PredictionSubject {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionSubject.class);
    private final BroadcastProcessor<Prediction> subject = BroadcastProcessor.create();

    void emit(@NotNull Prediction prediction) {
        subject.onNext(prediction);
        LOGGER.info("New prediction has been emitted");
    }

    Multi<Prediction> stream(StationId stationId) {
        return subject.filter(prediction -> Objects.equals(stationId, prediction.stationId))
                      .onOverflow().buffer(5);
    }
}
