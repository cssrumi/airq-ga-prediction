package pl.airq.prediction.model.command;

import io.smallrye.mutiny.Uni;
import pl.airq.prediction.model.Try;

public interface Respond<C extends Command> {

    Uni<Try> respond(C command);

}
