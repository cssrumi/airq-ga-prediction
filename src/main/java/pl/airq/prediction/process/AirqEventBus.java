package pl.airq.prediction.process;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import pl.airq.prediction.model.Try;

@Dependent
public class AirqEventBus {

    private final EventBus eventBus;

    @Inject
    public AirqEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public <T> Uni<T> request(String address, Object message) {
        return eventBus.request(address, message)
                       .onItem().apply(result -> ((Try<T>) result.body()).get());
    }

    public void publish(String address, Object message) {
        eventBus.publish(address, message);
    }

    public void sendAndForget(String address, Object message) {
        eventBus.sendAndForget(address, message);
    }
}
