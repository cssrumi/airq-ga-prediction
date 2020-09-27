package pl.airq.prediction.ga.process.external;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.AirqDataEnrichedEvent;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.exception.DeserializationException;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.EventParser;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;
import pl.airq.prediction.ga.process.TopicConstant;

@ApplicationScoped
public class AirqDataEnrichedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqDataEnrichedEventConsumer.class);
    private final AppEventBus eventBus;
    private final EventParser parser;
    private final Cache<StationId, EnrichedData> cache;

    @Inject
    public AirqDataEnrichedEventConsumer(AppEventBus eventBus,
                                         EventParser parser,
                                         Cache<StationId, EnrichedData> cache) {
        this.eventBus = eventBus;
        this.parser = parser;
        this.cache = cache;
    }

    @Incoming(TopicConstant.DATA_ENRICHED_EXTERNAL_TOPIC)
    public void consume(String rawEvent) {
        final AirqDataEnrichedEvent event;
        try {
            event = parser.parse(rawEvent, AirqDataEnrichedEvent.class);
        } catch (DeserializationException e) {
            LOGGER.warn("Unable to process event: {}.", rawEvent, e);
            return;
        }
        final EnrichedData enrichedData = event.payload.enrichedData;
        final StationId stationId = enrichedData.station;

        cache.upsert(stationId, enrichedData);
        eventBus.sendAndForget(EventFactory.predict(stationId));
        LOGGER.info("{} handled.", event.getClass().getSimpleName());
    }
}
