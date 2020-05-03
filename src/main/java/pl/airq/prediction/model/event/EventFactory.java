package pl.airq.prediction.model.event;

import pl.airq.prediction.model.command.Command;

public interface EventFactory<T> {

    <C extends Command> Event from(C command);
}
