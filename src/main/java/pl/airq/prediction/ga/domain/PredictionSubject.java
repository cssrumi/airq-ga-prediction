package pl.airq.prediction.ga.domain;

import io.reactivex.Scheduler;
import io.reactivex.internal.schedulers.SingleScheduler;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.converters.multi.MultiRxConverters;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@ApplicationScoped
class PredictionSubject {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionSubject.class);
    private final ReplaySubject<Prediction> rxSubject = ReplaySubject.createWithSize(1);

    void emit(Prediction prediction) {
        rxSubject.onNext(prediction);
        LOGGER.info("New prediction has been emitted");
    }

    Multi<Prediction> stream() {
        return Multi.createFrom().converter(MultiRxConverters.fromObservable(), rxSubject);
    }

    Multi<Prediction> stream(StationId stationId) {
        return stream().transform().byFilteringItemsWith(prediction -> Objects.equals(stationId, prediction.stationId));
    }
}
