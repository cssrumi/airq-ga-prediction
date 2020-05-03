package pl.airq.prediction.model.event;

import java.time.OffsetDateTime;
import pl.airq.prediction.model.Payload;

public abstract class Event<P extends Payload> {

    protected final OffsetDateTime dateTime;
    protected final P payload;
    protected final String eventType;

    public Event(OffsetDateTime dateTime, P payload, String eventType) {
        this.dateTime = dateTime;
        this.payload = payload;
        this.eventType = eventType;
    }

    public OffsetDateTime getDateTime() {
        return dateTime;
    }

    public P getPayload() {
        return payload;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "Event{" +
                "dateTime=" + dateTime +
                ", payload=" + payload +
                ", eventType='" + eventType + '\'' +
                '}';
    }
}
