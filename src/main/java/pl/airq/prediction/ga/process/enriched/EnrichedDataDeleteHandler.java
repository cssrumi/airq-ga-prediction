package pl.airq.prediction.ga.process.enriched;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.TSKey;
import pl.airq.common.util.Timestamp;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.Cache;

@ApplicationScoped
class EnrichedDataDeleteHandler implements EnrichedDataDispatcher.EnrichedDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataDeleteHandler.class);

    private final Cache<StationId, EnrichedData> cache;

    @Inject
    EnrichedDataDeleteHandler(Cache<StationId, EnrichedData> cache) {
        this.cache = cache;
    }

    @Override
    public Uni<Void> handle(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return processIfTimestampIsEq(key, deleteProcess(key, airqEvent));
    }

    private Uni<Void> processIfTimestampIsEq(TSKey key, Uni<Void> uni) {
        return cache.get(key.stationId())
                    .map(enrichedData -> enrichedData.timestamp)
                    .map(timestamp -> Timestamp.isEqual(timestamp, key.timestamp()))
                    .onItem().transformToUni(result -> result ? uni : Uni.createFrom().voidItem());
    }

    private Uni<Void> deleteProcess(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return Uni.createFrom().item(key.stationId())
                  .call(cache::remove)
                  .invoke(stationId -> LOGGER.info("{} for {} removed from cache.", EnrichedData.class.getSimpleName(), stationId.value()))
                  .onItem().ignore().andContinueWithNull();
    }

}
