package pl.airq.prediction.model.event;

import io.smallrye.mutiny.Uni;

public interface Consume<E extends Event> {

    Uni<Void> consume(E event);
}
