package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.phenotype.AirqPhenotypeQuery;
import pl.airq.common.vo.StationId;

@ApplicationScoped
public class PhenotypeCache extends CacheWithFallback<StationId, AirqPhenotype> {

    private final AirqPhenotypeQuery query;

    @Inject
    public PhenotypeCache(AirqPhenotypeQuery query) {
        this.query = query;
    }

    @Override
    Uni<AirqPhenotype> findLatest(StationId key) {
        return query.findLatestByStationId(key);
    }

    @Override
    String cacheName() {
        return PhenotypeCache.class.getSimpleName();
    }
}
