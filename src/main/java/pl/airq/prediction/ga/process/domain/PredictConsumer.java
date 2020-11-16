package pl.airq.prediction.ga.process.domain;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.MutinyUtils;
import pl.airq.common.process.event.Consumer;
import pl.airq.common.process.failure.Failure;
import pl.airq.prediction.ga.domain.PredictionFacade;
import pl.airq.prediction.ga.process.TopicConstant;
import pl.airq.prediction.ga.domain.command.Predict;

@ApplicationScoped
public class PredictConsumer implements Consumer<Predict> {

    private final PredictionFacade predictionFacade;
    private final AppEventBus eventBus;

    @Inject
    public PredictConsumer(PredictionFacade predictionFacade, AppEventBus eventBus) {
        this.predictionFacade = predictionFacade;
        this.eventBus = eventBus;
    }

    @ConsumeEvent(TopicConstant.PREDICT_TOPIC)
    Uni<Void> consumeEvent(Predict event) {
        return consume(event);
    }

    @Override
    public Uni<Void> consume(Predict event) {
        return predictionFacade.predict(event.payload.stationId)
                               .onItem().transformToUni(MutinyUtils::ignoreUniResult)
                               .onFailure().invoke(throwable -> eventBus.publish(Failure.from(throwable)));
    }
}
