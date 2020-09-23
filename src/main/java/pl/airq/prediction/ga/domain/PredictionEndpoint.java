package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.OffsetDateTime;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.SseElementType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import pl.airq.common.domain.prediction.Prediction;
import pl.airq.common.vo.StationId;

@Path("/api/prediction")
@Produces(MediaType.APPLICATION_JSON)
public class PredictionEndpoint {

    private final PredictionFacade predictionFacade;
    private final PredictionSubject subject;

    @Inject
    public PredictionEndpoint(PredictionFacade predictionFacade, PredictionSubject subject) {
        this.predictionFacade = predictionFacade;
        this.subject = subject;
    }

    @GET
    @Path("/{stationId}")
    public Uni<Response> findLatest(@PathParam String stationId) {
        return predictionFacade.findPrediction(stationId)
                               .onItem().transform(prediction -> Response.ok(prediction).build());
    }

    @GET
    @Path("/{stationId}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @SseElementType(MediaType.APPLICATION_JSON)
    public Multi<Prediction> stream(@PathParam String stationId) {
        return subject.stream(StationId.from(stationId));
    }
}
