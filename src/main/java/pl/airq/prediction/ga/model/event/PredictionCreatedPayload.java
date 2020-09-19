package pl.airq.prediction.ga.model.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.process.Payload;

@RegisterForReflection
public class PredictionCreatedPayload implements Payload {

    public final Prediction prediction;

    public PredictionCreatedPayload(Prediction prediction) {
        this.prediction = prediction;
    }

    @Override
    public String toString() {
        return "PredictionCreatedPayload{" +
                "prediction=" + prediction +
                '}';
    }
}
