package pl.airq.prediction.ga.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;

@Dependent
class EventParserProducer {

    @Produces
    @Singleton
    EventParser eventParser(ObjectMapper objectMapper) {
        final EventParser eventParser = new EventParser(objectMapper);
        eventParser.registerEvents(Set.of(
                AirqPhenotypeCreatedEvent.class,
                EnrichedDataCreatedEvent.class,
                EnrichedDataUpdatedEvent.class,
                EnrichedDataDeletedEvent.class
        ));
        return eventParser;
    }

}
