package pl.airq.prediction.model.event.enrichment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.OffsetDateTime;
import pl.airq.prediction.model.event.Event;

@RegisterForReflection
@JsonIgnoreProperties(value = {"eventType"})
public class DataEnriched extends Event<DataEnrichedPayload> {

    public DataEnriched(OffsetDateTime dateTime, DataEnrichedPayload payload) {
        super(dateTime, payload, DataEnriched.class.getSimpleName());
    }

    @Override
    public String toString() {
        return "DataEnriched{" +
                "dateTime=" + dateTime +
                ", payload=" + payload +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
