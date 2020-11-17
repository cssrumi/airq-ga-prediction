package pl.airq.prediction.ga.process.enriched;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.TSKey;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;
import pl.airq.prediction.ga.process.EventFactory;

@ApplicationScoped
class EnrichedDataUpsertHandler implements EnrichedDataDispatcher.EnrichedDataHandler {

    private final AppEventBus eventBus;
    private final Cache<StationId, EnrichedData> cache;

    @Inject
    EnrichedDataUpsertHandler(AppEventBus eventBus, Cache<StationId, EnrichedData> cache) {
        this.eventBus = eventBus;
        this.cache = cache;
    }

    @Override
    public Uni<Void> handle(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return processIfLatest(key, upsertProcess(airqEvent));
    }

    private Uni<Void> processIfLatest(TSKey key, Uni<Void> uni) {
        return isLatest(key).onItem().transformToUni(result -> result ? uni : Uni.createFrom().voidItem());
    }

    private Uni<Boolean> isLatest(TSKey key) {
        return cache.get(key.stationId())
                    .map(enrichedData -> isTimestampEqualOrAfter(enrichedData, key))
                    .onItem().ifNull().continueWith(() -> Boolean.TRUE);
    }

    private boolean isTimestampEqualOrAfter(EnrichedData enrichedData, TSKey key) {
        return enrichedData.timestamp.isEqual(key.timestamp()) || enrichedData.timestamp.isBefore(key.timestamp());
    }

    private Uni<Void> upsertProcess(AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return Uni.createFrom().item(airqEvent.payload.enrichedData)
                  .call(enrichedData -> cache.upsert(enrichedData.station, enrichedData))
                  .map(enrichedData -> EventFactory.predict(enrichedData.station))
                  .invoke(eventBus::sendAndForget)
                  .onItem().ignore().andContinueWithNull();
    }
}
