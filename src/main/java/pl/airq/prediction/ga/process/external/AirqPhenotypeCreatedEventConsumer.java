package pl.airq.prediction.ga.process.external;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.AirqDataEnrichedEvent;
import pl.airq.common.domain.exception.DeserializationException;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeCreatedEvent;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.EventParser;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;
import pl.airq.prediction.ga.process.TopicConstant;

@ApplicationScoped
public class AirqPhenotypeCreatedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeCreatedEventConsumer.class);
    private final AppEventBus eventBus;
    private final EventParser parser;
    private final Cache<StationId, AirqPhenotype> cache;

    @Inject
    public AirqPhenotypeCreatedEventConsumer(AppEventBus eventBus,
                                             EventParser parser,
                                             Cache<StationId, AirqPhenotype> cache) {
        this.eventBus = eventBus;
        this.parser = parser;
        this.cache = cache;
    }

    @Incoming(TopicConstant.AIRQ_PHENOTYPE_CREATED_EXTERNAL_TOPIC)
    public void consume(String rawEvent) {
        final AirqPhenotypeCreatedEvent event;
        try {
            event = parser.parse(rawEvent, AirqPhenotypeCreatedEvent.class);
        } catch (DeserializationException e) {
            LOGGER.warn("Unable to process event: {}.", rawEvent, e);
            return;
        }
        final AirqPhenotype airqPhenotype = event.payload.airqPhenotype;
        final StationId stationId = airqPhenotype.stationId;

        cache.upsert(stationId, airqPhenotype);
        eventBus.sendAndForget(EventFactory.predict(stationId));
        LOGGER.info("{} handled.", event.getClass().getSimpleName());
    }
}
