package pl.airq.prediction.ga.process.phenotype;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.failure.Failure;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;

@ApplicationScoped
class AirqPhenotypeCreatedProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeCreatedProcessor.class);

    private final AppEventBus eventBus;
    private final Cache<StationId, AirqPhenotype> cache;

    @Inject
    AirqPhenotypeCreatedProcessor(AppEventBus eventBus, Cache<StationId, AirqPhenotype> cache) {
        this.eventBus = eventBus;
        this.cache = cache;
    }

    Uni<Void> process(AirqPhenotypeCreatedEvent event) {
        return Uni.createFrom().item(event.payload.airqPhenotype)
                  .call(airqPhenotype -> cache.upsert(airqPhenotype.stationId, airqPhenotype))
                  .invoke(airqPhenotype -> eventBus.sendAndForget(EventFactory.predict(airqPhenotype.stationId)))
                  .invoke(() -> LOGGER.info("{} handled.", AirqPhenotypeCreatedEvent.class.getSimpleName()))
                  .onFailure().invoke(throwable -> eventBus.publish(Failure.from(throwable)))
                  .onItem().ignore().andContinueWithNull();
    }

}
