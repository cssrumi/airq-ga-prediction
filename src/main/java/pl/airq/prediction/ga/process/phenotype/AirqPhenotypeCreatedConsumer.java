package pl.airq.prediction.ga.process.phenotype;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.failure.Failure;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;

@ApplicationScoped
public class AirqPhenotypeCreatedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeCreatedConsumer.class);
    private final AppEventBus eventBus;
    private final EventParser parser;
    private final Cache<StationId, AirqPhenotype> cache;

    @Inject
    public AirqPhenotypeCreatedConsumer(AppEventBus eventBus,
                                        EventParser parser,
                                        Cache<StationId, AirqPhenotype> cache) {
        this.eventBus = eventBus;
        this.parser = parser;
        this.cache = cache;
    }

    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    @Incoming("airq-phenotype-created")
    Uni<Void> consume(String rawEvent) {
        return Uni.createFrom().item(parser.deserializeDomainEvent(rawEvent))
                  .invoke(airqEvent -> LOGGER.info("AirqEvent arrived: {}", airqEvent))
                  .onItem().castTo(AirqPhenotypeCreatedEvent.class)
                  .map(event -> event.payload.airqPhenotype)
                  .call(airqPhenotype -> cache.upsert(airqPhenotype.stationId, airqPhenotype))
                  .invoke(airqPhenotype -> eventBus.sendAndForget(EventFactory.predict(airqPhenotype.stationId)))
                  .invoke(() -> LOGGER.info("{} handled.", AirqPhenotypeCreatedEvent.class.getSimpleName()))
                  .onFailure().invoke(throwable -> eventBus.publish(Failure.from(throwable)))
                  .onItem().ignore().andContinueWithNull();
    }
}
