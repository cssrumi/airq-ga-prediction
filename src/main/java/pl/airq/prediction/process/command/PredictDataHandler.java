package pl.airq.prediction.process.command;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.time.OffsetDateTime;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.prediction.domain.Prediction;
import pl.airq.prediction.domain.PredictionService;
import pl.airq.prediction.model.command.PredictData;
import pl.airq.prediction.model.event.Consume;
import pl.airq.prediction.model.event.DataPredicted;
import pl.airq.prediction.model.event.DataPredictedPayload;
import pl.airq.prediction.process.AirqEventBus;

import static pl.airq.prediction.model.TopicConstant.PREDICTED_TOPIC;
import static pl.airq.prediction.model.TopicConstant.PREDICT_TOPIC;

@ApplicationScoped
public class PredictDataHandler implements Consume<PredictData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictDataHandler.class);
    private final PredictionService predictionService;
    private final AirqEventBus eventBus;

    @Inject
    public PredictDataHandler(PredictionService predictionService, AirqEventBus eventBus) {
        this.predictionService = predictionService;
        this.eventBus = eventBus;
    }

    @Override
    @ConsumeEvent(PREDICT_TOPIC)
    public Uni<Void> consume(PredictData event) {
        return Multi.createFrom().iterable(event.getPayload().enrichedData)
                .flatMap(enrichedData -> predictionService.predict(enrichedData).toMulti())
                .collectItems()
                .asList()
                .onItem()
                .produceUni(this::saveAndPublish);
    }

    private Uni<Void> saveAndPublish(List<Prediction> predictions) {
        return Uni.createFrom()
                  .item(predictions)
                  .onItem()
                  .produceUni(this::publishDataPredictedEvent);
    }

    private Uni<Void> publishDataPredictedEvent(List<Prediction> predictions) {
        return Uni.createFrom().item(() -> {
           LOGGER.info("Publishing DataPredicted Event...");
            DataPredictedPayload payload = new DataPredictedPayload(predictions);
            DataPredicted event = new DataPredicted(OffsetDateTime.now(), payload);
            eventBus.publish(PREDICTED_TOPIC, event);
            LOGGER.info("DataPredicted Event published.");
           return null;
        });
    }
}
