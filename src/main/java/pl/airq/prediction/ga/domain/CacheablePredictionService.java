package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.domain.enriched.EnrichedData;
import pl.airq.common.domain.phenotype.AirqPhenotype;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.exception.ResourceNotFoundException;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.cache.EnrichedDataCache;
import pl.airq.prediction.ga.cache.PhenotypeCache;

@ApplicationScoped
class CacheablePredictionService implements PredictionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheablePredictionService.class);
    private static final FieldMap<EnrichedData> MAP = new FieldMap<>(EnrichedData.class);
    private final EnrichedDataCache enrichedDataCache;
    private final PhenotypeCache phenotypeCache;

    @Inject
    CacheablePredictionService(EnrichedDataCache enrichedDataCache, PhenotypeCache phenotypeCache) {
        this.enrichedDataCache = enrichedDataCache;
        this.phenotypeCache = phenotypeCache;
    }

    @Override
    public Uni<Prediction> predict(StationId stationId) {
        return enrichedDataCache.get(stationId)
                                .chain(enrichedData -> phenotypeCache.get(stationId)
                                                                     .map(phenotype -> predict(phenotype, enrichedData)));
    }

    @Override
    public Prediction predict(AirqPhenotype phenotype, EnrichedData enrichedData) {
        validatePrediction(phenotype, enrichedData);

        double predictedValue = 0;
        for (int i = 0; i < phenotype.fields.size(); i++) {
            predictedValue += MAP.getFloat(phenotype.fields.get(i), enrichedData) * phenotype.values.get(i);
        }

        Prediction prediction = new Prediction(OffsetDateTime.now(), predictedValue, phenotype.prediction, phenotype.stationId);
        LOGGER.info("New Prediction created: {}", prediction);
        return prediction;
    }

    private void validatePrediction(AirqPhenotype phenotype, EnrichedData enrichedData) {
        if (Objects.isNull(phenotype)) {
            throw new ResourceNotFoundException(AirqPhenotype.class);
        }
        if (Objects.isNull(enrichedData)) {
            throw new ResourceNotFoundException(EnrichedData.class);
        }
        if (phenotype.values.size() != phenotype.fields.size()) {
            throw new PredictionProcessingException(String.format("%s fields and values size does not match.",
                    AirqPhenotype.class.getSimpleName()));
        }
        if (!Objects.equals(phenotype.stationId, enrichedData.station)) {
            throw new PredictionProcessingException(String.format("Station for {%s: %s} and {%s: %s} does not match.",
                    AirqPhenotype.class.getSimpleName(), phenotype.stationId,
                    EnrichedData.class.getSimpleName(), enrichedData.station));
        }
    }
}
