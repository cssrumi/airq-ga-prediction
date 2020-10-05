package pl.airq.prediction.ga.domain;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@ApplicationScoped
class PredictionSubject {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionSubject.class);
    private final PublishProcessor<Prediction> rxSubject = PublishProcessor.create();

    void emit(Prediction prediction) {
        rxSubject.onNext(prediction);
        LOGGER.info("New prediction has been emitted");
    }

    Multi<Prediction> stream(StationId stationId) {
        return Multi.createFrom().converter(MultiRxConverters.fromFlowable(), filterSubject(stationId));
    }

    private Flowable<Prediction> filterSubject(StationId stationId) {
        return rxSubject.filter(prediction -> Objects.equals(stationId, prediction.stationId))
                        .onBackpressureBuffer(5);
    }
}
