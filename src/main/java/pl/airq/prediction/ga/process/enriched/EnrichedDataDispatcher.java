package pl.airq.prediction.ga.process.enriched;

import io.smallrye.mutiny.Uni;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.process.failure.Failure;
import pl.airq.common.store.key.TSKey;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;

@ApplicationScoped
public class EnrichedDataDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataDispatcher.class);
    private static final String CREATED = EnrichedDataCreatedEvent.class.getSimpleName();
    private static final String UPDATED = EnrichedDataUpdatedEvent.class.getSimpleName();
    private static final String DELETED = EnrichedDataDeletedEvent.class.getSimpleName();
    private static final EnrichedDataHandler DEFAULT_HANDLER = (key, airqEvent) -> Uni.createFrom().voidItem()
                                                                                      .invoke(() -> LOGGER.warn("Unhandled event: {}", airqEvent));
    private final AppEventBus eventBus;
    private final Cache<StationId, EnrichedData> cache;
    private final Map<String, EnrichedDataHandler> dispatchMap;

    @Inject
    public EnrichedDataDispatcher(AppEventBus eventBus, Cache<StationId, EnrichedData> cache) {
        this.eventBus = eventBus;
        this.cache = cache;
        this.dispatchMap = Map.of(
                CREATED, this::upsertHandler,
                UPDATED, this::upsertHandler,
                DELETED, this::deleteHandler
        );
    }

    Uni<Void> dispatch(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return dispatchMap.getOrDefault(airqEvent.eventType(), DEFAULT_HANDLER).handle(key, airqEvent)
                          .onFailure().invoke(throwable -> eventBus.publish(Failure.from(throwable)));
    }

    private Uni<Void> upsertHandler(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return Uni.createFrom().item(airqEvent.payload.enrichedData)
                  .call(enrichedData -> cache.upsert(enrichedData.station, enrichedData))
                  .map(enrichedData -> EventFactory.predict(enrichedData.station))
                  .invoke(eventBus::sendAndForget)
                  .onItem().ignore().andContinueWithNull();
    }

    private Uni<Void> deleteHandler(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return Uni.createFrom().item(key.stationId())
                  .call(cache::remove)
                  .invoke(stationId -> LOGGER.info("{} for {} removed from cache.", EnrichedData.class.getSimpleName(), stationId.value()))
                  .onItem().ignore().andContinueWithNull();
    }

    interface EnrichedDataHandler {
        Uni<Void> handle(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent);
    }
}
