package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.enriched.EnrichedDataQuery;
import pl.airq.common.vo.StationId;

@ApplicationScoped
public class EnrichedDataCache extends CacheWithFallback<StationId, EnrichedData> {

    private final EnrichedDataQuery query;

    @Inject
    public EnrichedDataCache(EnrichedDataQuery query) {
        this.query = query;
    }

    @Override
    Uni<EnrichedData> findLatest(StationId key) {
        return query.findLatestByStation(key.value());
    }

    @Override
    String cacheName() {
        return EnrichedDataCache.class.getSimpleName();
    }
}
