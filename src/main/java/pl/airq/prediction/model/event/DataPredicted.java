package pl.airq.prediction.model.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.OffsetDateTime;

@RegisterForReflection
public class DataPredicted extends Event<DataPredictedPayload>{

    public DataPredicted(OffsetDateTime dateTime, DataPredictedPayload payload) {
        super(dateTime, payload, DataPredicted.class.getSimpleName());
    }

    @Override
    public String toString() {
        return "DataPredicted{" +
                "dateTime=" + dateTime +
                ", payload=" + payload +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
