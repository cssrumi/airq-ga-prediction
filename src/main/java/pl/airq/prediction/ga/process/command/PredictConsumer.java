package pl.airq.prediction.ga.process.command;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.process.event.Consumer;
import pl.airq.prediction.ga.domain.PredictionFacade;
import pl.airq.prediction.ga.model.TopicConstant;
import pl.airq.prediction.ga.model.command.Predict;

@ApplicationScoped
public class PredictConsumer implements Consumer<Predict> {

    private final PredictionFacade predictionFacade;

    @Inject
    public PredictConsumer(PredictionFacade predictionFacade) {
        this.predictionFacade = predictionFacade;
    }

    @ConsumeEvent(TopicConstant.PREDICT_TOPIC)
    Uni<Void> consumeEvent(Predict event) {
        return consume(event);
    }

    @Override
    public Uni<Void> consume(Predict event) {
        return predictionFacade.predict(event.payload.stationId)
                               .onItem().castTo(Void.class);
    }
}
