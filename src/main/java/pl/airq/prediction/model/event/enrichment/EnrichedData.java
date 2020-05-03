package pl.airq.prediction.model.event.enrichment;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.OffsetDateTime;

@RegisterForReflection
public final class EnrichedData {

    public final OffsetDateTime timestamp;
    public final Float pm10;
    public final Float pm25;
    public final Float temp;
    public final Float wind;
    public final Float windDirection;
    public final Float humidity;
    public final Float pressure;
    public final Float lon;
    public final Float lat;
    public final DataProvider provider;
    public final String station;


    EnrichedData(OffsetDateTime timestamp, Float pm10, Float pm25, Float temp, Float wind, Float windDirection, Float humidity, Float pressure,
                        Float lon, Float lat, DataProvider provider, String station) {
        this.timestamp = timestamp;
        this.pm10 = pm10;
        this.pm25 = pm25;
        this.temp = temp;
        this.wind = wind;
        this.windDirection = windDirection;
        this.humidity = humidity;
        this.pressure = pressure;
        this.lon = lon;
        this.lat = lat;
        this.provider = provider;
        this.station = station;
    }

    @Override
    public String toString() {
        return "EnrichedData{" +
                "timestamp=" + timestamp +
                ", pm10=" + pm10 +
                ", pm25=" + pm25 +
                ", temp=" + temp +
                ", wind=" + wind +
                ", windDirection=" + windDirection +
                ", humidity=" + humidity +
                ", pressure=" + pressure +
                ", lon=" + lon +
                ", lat=" + lat +
                ", provider=" + provider +
                ", station='" + station + '\'' +
                '}';
    }
}
