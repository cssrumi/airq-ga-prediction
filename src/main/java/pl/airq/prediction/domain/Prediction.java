package pl.airq.prediction.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.OffsetDateTime;

@RegisterForReflection
public final class Prediction {

    public final OffsetDateTime timestamp;
    public final Float pm10After1h;
    public final Float pm10After24h;
    public final Float lon;
    public final Float lat;

    Prediction(OffsetDateTime timestamp, Float pm10After1h, Float pm10After24h, Float lon, Float lat) {
        this.timestamp = timestamp;
        this.pm10After1h = pm10After1h;
        this.pm10After24h = pm10After24h;
        this.lon = lon;
        this.lat = lat;
    }
}
