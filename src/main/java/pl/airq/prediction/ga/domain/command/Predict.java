package pl.airq.prediction.ga.domain.command;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.event.AppEvent;
import pl.airq.prediction.ga.process.TopicConstant;

@RegisterForReflection
public class Predict extends AppEvent<PredictPayload> {

    public Predict(PredictPayload payload) {
        super(payload);
    }

    @Override
    public String defaultTopic() {
        return TopicConstant.PREDICT_TOPIC;
    }
}
