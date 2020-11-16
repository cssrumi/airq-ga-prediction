package pl.airq.prediction.ga.process.enriched;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.EventParser;
import pl.airq.common.process.MutinyUtils;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.store.key.TSKey;

@ApplicationScoped
public class EnrichedDataConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataConsumer.class);
    private final EventParser parser;
    private final EnrichedDataDispatcher dispatcher;

    @Inject
    public EnrichedDataConsumer(EventParser parser, EnrichedDataDispatcher dispatcher) {
        this.parser = parser;
        this.dispatcher = dispatcher;
    }

    @SuppressWarnings("unchecked")
    @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 10)
    @Incoming("data-enriched")
    Uni<Void> consume(Message<String> message) {
        return Uni.createFrom().item(parser.deserializeDomainEvent(message.getPayload()))
                  .invoke(airqEvent -> LOGGER.info("AirqEvent arrived: {}, key: {}", airqEvent, getKey(message).value()))
                  .flatMap(event -> dispatch(getKey(message), event));
    }

    @SuppressWarnings("unchecked")
    private TSKey getKey(Message<String> message) {
        return ((IncomingKafkaRecord<TSKey, String>) message.unwrap(IncomingKafkaRecord.class)).getKey();
    }

    private Uni<Void> dispatch(TSKey key, AirqEvent<EnrichedDataEventPayload> event) {
        return dispatcher.dispatch(key, event)
                         .invoke(result -> LOGGER.info("Dispatched: {} - {}.", key.value(), event.eventType()))
                         .onItem().transformToUni(MutinyUtils::ignoreUniResult);
    }
}
