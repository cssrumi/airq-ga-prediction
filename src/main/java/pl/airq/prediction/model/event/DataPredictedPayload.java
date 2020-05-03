package pl.airq.prediction.model.event;

import java.util.List;
import pl.airq.prediction.domain.Prediction;
import pl.airq.prediction.model.Payload;

public class DataPredictedPayload implements Payload {

    public final List<Prediction> predictions;

    public DataPredictedPayload(List<Prediction> predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        return "DataPredictedPayload{" +
                "predictions=" + predictions +
                '}';
    }
}
