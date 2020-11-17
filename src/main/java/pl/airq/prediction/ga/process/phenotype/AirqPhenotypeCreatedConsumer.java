package pl.airq.prediction.ga.process.phenotype;

import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.ctx.phenotype.AirqPhenotypeCreatedEvent;

@ApplicationScoped
public class AirqPhenotypeCreatedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AirqPhenotypeCreatedConsumer.class);
    private final EventParser parser;
    private final AirqPhenotypeCreatedProcessor processor;

    @Inject
    public AirqPhenotypeCreatedConsumer(EventParser parser, AirqPhenotypeCreatedProcessor processor) {
        this.parser = parser;
        this.processor = processor;
    }

    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    @Incoming("airq-phenotype-created")
    Uni<Void> consume(String rawEvent) {
        return Uni.createFrom().item(parser.deserializeDomainEvent(rawEvent))
                  .invoke(airqEvent -> LOGGER.info("AirqEvent arrived: {}", airqEvent))
                  .onItem().castTo(AirqPhenotypeCreatedEvent.class)
                  .onItem().transformToUni(processor::process);
    }
}
