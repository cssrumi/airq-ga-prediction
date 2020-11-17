package pl.airq.prediction.ga.process.enriched;

import io.smallrye.mutiny.Uni;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.AppEventBus;
import pl.airq.common.process.ctx.enriched.EnrichedDataCreatedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataDeletedEvent;
import pl.airq.common.process.ctx.enriched.EnrichedDataEventPayload;
import pl.airq.common.process.ctx.enriched.EnrichedDataUpdatedEvent;
import pl.airq.common.process.event.AirqEvent;
import pl.airq.common.process.failure.Failure;
import pl.airq.common.store.key.TSKey;

@ApplicationScoped
public class EnrichedDataDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedDataDispatcher.class);
    private static final String CREATED = EnrichedDataCreatedEvent.class.getSimpleName();
    private static final String UPDATED = EnrichedDataUpdatedEvent.class.getSimpleName();
    private static final String DELETED = EnrichedDataDeletedEvent.class.getSimpleName();

    private final AppEventBus eventBus;
    private final Map<String, EnrichedDataHandler> dispatchMap;

    @Inject
    public EnrichedDataDispatcher(AppEventBus eventBus,
                                  EnrichedDataUpsertHandler upsertHandler,
                                  EnrichedDataDeleteHandler deleteHandler) {
        this.eventBus = eventBus;
        this.dispatchMap = Map.of(
                CREATED, upsertHandler,
                UPDATED, upsertHandler,
                DELETED, deleteHandler
        );
    }

    Uni<Void> dispatch(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return dispatchMap.getOrDefault(airqEvent.eventType(), this::defaultHandler).handle(key, airqEvent)
                          .onFailure().invoke(throwable -> eventBus.publish(Failure.from(throwable)));
    }

    private Uni<Void> defaultHandler(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent) {
        return Uni.createFrom().voidItem()
                  .invoke(() -> LOGGER.warn("Unhandled event: {}", airqEvent));

    }

    interface EnrichedDataHandler {
        Uni<Void> handle(TSKey key, AirqEvent<EnrichedDataEventPayload> airqEvent);
    }
}
