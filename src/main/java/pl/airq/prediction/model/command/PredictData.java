package pl.airq.prediction.model.command;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.OffsetDateTime;
import pl.airq.prediction.model.event.enrichment.DataEnrichedPayload;

@RegisterForReflection
public class PredictData extends Command<DataEnrichedPayload> {

    public PredictData(OffsetDateTime dateTime, DataEnrichedPayload payload) {
        super(dateTime, payload, PredictData.class.getSimpleName());
    }

    @Override
    public String toString() {
        return "PredictData{" +
                "dateTime=" + dateTime +
                ", payload=" + payload +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
