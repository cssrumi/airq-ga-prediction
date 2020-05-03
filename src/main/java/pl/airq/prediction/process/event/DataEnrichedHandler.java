package pl.airq.prediction.process.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.prediction.model.event.enrichment.DataEnriched;
import pl.airq.prediction.process.AirqEventBus;

import static pl.airq.prediction.model.TopicConstant.DATA_ENRICHED_EXTERNAL_TOPIC;
import static pl.airq.prediction.model.TopicConstant.PREDICT_TOPIC;

@ApplicationScoped
public class DataEnrichedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataEnrichedHandler.class);
    private final AirqEventBus eventBus;
    private final ObjectMapper mapper;

    @Inject
    public DataEnrichedHandler(AirqEventBus eventBus, ObjectMapper mapper) {
        this.eventBus = eventBus;
        this.mapper = mapper;
    }

    @Incoming(DATA_ENRICHED_EXTERNAL_TOPIC)
    public void predict(String rawEvent) {
        try {
            LOGGER.info(String.format("Event: %s", rawEvent));
            final DataEnriched event = mapper.readValue(rawEvent, DataEnriched.class);
            eventBus.sendAndForget(PREDICT_TOPIC, event);
            LOGGER.info("EnrichedData event handled.");
        } catch (JsonProcessingException e) {
            LOGGER.error("Error parsing DataEnriched event...", e);
        }
    }


}
