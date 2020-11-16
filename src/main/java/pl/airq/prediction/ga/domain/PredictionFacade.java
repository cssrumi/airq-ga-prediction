package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.PersistentRepository;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.exception.ResourceNotFoundException;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;

@ApplicationScoped
public class PredictionFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionFacade.class);
    private final PredictionService predictionService;
    private final AppEventBus eventBus;
    private final PersistentRepository<Prediction> repository;
    private final Cache<StationId, Prediction> cache;
    private final PredictionSubject publisher;

    @Inject
    public PredictionFacade(PredictionService predictionService,
                            AppEventBus eventBus,
                            PersistentRepository<Prediction> repository,
                            Cache<StationId, Prediction> cache,
                            PredictionSubject publisher) {
        this.predictionService = predictionService;
        this.eventBus = eventBus;
        this.repository = repository;
        this.cache = cache;
        this.publisher = publisher;
    }

    public Uni<Prediction> predict(StationId stationId) {
        return predictionService.predict(stationId)
                                .onItem().ifNull().failWith(PredictionProcessingException::new)
                                .invokeUni(repository::save)
                                .invokeUni(prediction -> cache.upsert(stationId, prediction))
                                .invoke(prediction -> eventBus.publish(EventFactory.predictionCreated(prediction)))
                                .invoke(publisher::emit);
    }

    public Uni<Prediction> findPrediction(String stationId) {
        return cache.get(StationId.from(stationId))
                    .invoke(prediction -> LOGGER.info("Find result: {}", prediction))
                    .onItem().ifNull().failWith(() -> ResourceNotFoundException.fromResource(Prediction.class));
    }
}
