package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.exception.ResourceNotFoundException;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.model.EventFactory;

@ApplicationScoped
public class PredictionFacade {

    private final PredictionService predictionService;
    private final AppEventBus eventBus;
    private final PersistentRepository<Prediction> repository;
    private final Cache<StationId, Prediction> cache;

    @Inject
    public PredictionFacade(PredictionService predictionService,
                            AppEventBus eventBus,
                            PersistentRepository<Prediction> repository,
                            Cache<StationId, Prediction> cache) {
        this.predictionService = predictionService;
        this.eventBus = eventBus;
        this.repository = repository;
        this.cache = cache;
    }

    public Uni<Prediction> predict(StationId stationId) {
        return predictionService.predict(stationId)
                                .invoke(repository::save)
                                .invokeUni(prediction -> cache.upsert(stationId, prediction))
                                .invoke(prediction -> eventBus.publish(EventFactory.predictionCreated(prediction)));
    }

    public Uni<Prediction> findPrediction(String stationId) {
        return cache.get(StationId.from(stationId))
                    .onItem().ifNull().failWith(() -> ResourceNotFoundException.fromResource(Prediction.class));
    }
}
