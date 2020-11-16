package pl.airq.prediction.ga.process.domain;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.MutinyUtils;
import pl.airq.common.process.event.Consumer;
import pl.airq.prediction.ga.process.TopicConstant;
import pl.airq.prediction.ga.domain.event.PredictionCreated;

@ApplicationScoped
public class PredictionCreatedConsumer implements Consumer<PredictionCreated> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredictionCreatedConsumer.class);

    @ConsumeEvent(TopicConstant.PREDICTION_CREATED_TOPIC)
    Uni<Void> consumeEvent(PredictionCreated event) {
        return consume(event);
    }

    @Override
    public Uni<Void> consume(PredictionCreated event) {
        return MutinyUtils.fromRunnable(() -> LOGGER.debug("New prediction for {} created.", event.payload.prediction.stationId.value()));
    }
}
