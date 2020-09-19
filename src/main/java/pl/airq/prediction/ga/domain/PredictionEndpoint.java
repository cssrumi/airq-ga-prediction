package pl.airq.prediction.ga.domain;

import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/api/prediction")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PredictionEndpoint {

    private final PredictionFacade predictionFacade;

    @Inject
    public PredictionEndpoint(PredictionFacade predictionFacade) {
        this.predictionFacade = predictionFacade;
    }

    @GET
    @Path("/{stationId}")
    public Uni<Response> findLatest(@PathParam String stationId) {
        return predictionFacade.findPrediction(stationId)
                               .onItem().transform(prediction -> Response.ok(prediction).build());
    }
}
