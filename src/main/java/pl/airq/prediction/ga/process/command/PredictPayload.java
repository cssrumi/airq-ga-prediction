package pl.airq.prediction.ga.process.command;

import io.quarkus.runtime.annotations.RegisterForReflection;
import pl.airq.common.process.Payload;
import pl.airq.common.vo.StationId;

@RegisterForReflection
public class PredictPayload implements Payload {

    public final StationId stationId;

    public PredictPayload(StationId stationId) {
        this.stationId = stationId;
    }
}
