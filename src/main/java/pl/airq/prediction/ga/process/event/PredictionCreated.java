package pl.airq.prediction.ga.process.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.event.AppEvent;
import pl.airq.prediction.ga.process.TopicConstant;

@RegisterForReflection
public class PredictionCreated extends AppEvent<PredictionCreatedPayload> {

    public PredictionCreated(PredictionCreatedPayload payload) {
        super(payload);
    }

    @Override
    public String defaultTopic() {
        return TopicConstant.PREDICTION_CREATED_TOPIC;
    }

    @Override
    public String toString() {
        return "PredictionCreated{" +
                "timestamp=" + timestamp +
                ", payload=" + payload +
                '}';
    }
}
