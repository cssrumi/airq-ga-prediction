package pl.airq.prediction.ga.process;

import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;
import pl.airq.prediction.ga.domain.command.Predict;
import pl.airq.prediction.ga.domain.command.PredictPayload;
import pl.airq.prediction.ga.domain.event.PredictionCreated;
import pl.airq.prediction.ga.domain.event.PredictionCreatedPayload;

public class EventFactory {

    private EventFactory() {
    }

    public static PredictionCreated predictionCreated(Prediction prediction) {
        return new PredictionCreated(new PredictionCreatedPayload(prediction));
    }

    public static Predict predict(StationId stationId) {
        return new Predict(new PredictPayload(stationId));
    }

}
