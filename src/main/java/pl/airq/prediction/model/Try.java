package pl.airq.prediction.model;

import io.smallrye.mutiny.Uni;
import java.util.Objects;
import java.util.function.Supplier;

public final class Try<R> {

    private final R value;
    private final Throwable error;

    private Try(R value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    public R get() {
        sneakyThrow(error);
        return value;
    }

    public Throwable error() {
        return error;
    }

    public static <T> Uni<Try<T>> from(Uni<T> uni) {
        return uni.onItem().apply(e -> of(() -> e))
                  .onFailure().recoverWithItem(Try::from);
    }

    public static <T> Try<T> from(Throwable throwable) {
        return new Try<T>(null, throwable);
    }

    public static Uni<Try> raw(Uni uni) {
        return from(uni);
    }

    public static Try raw(Throwable throwable) {
        return new Try(null, throwable);
    }

    public static <T> Try<T> of(Supplier<T> supplier) {
        T response = null;
        Throwable throwable = null;
        try {
            response = supplier.get();
        } catch (Throwable t) {
            throwable = t;
        }

        return new Try<T>(response, throwable);
    }

    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        if (Objects.nonNull(e)) {
            throw (E) e;
        }
    }
}
