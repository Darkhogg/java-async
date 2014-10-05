package async.event;

import java.util.function.Consumer;

public interface EventEmitter {

    public abstract <E extends Event> void on (Class<E> event, Consumer<E> listener);

    public default <E extends Event> void once (Class<E> event, Consumer<E> listener) {
        on(event, (evt) -> {
            listener.accept(evt);
            off(event, listener);
        });
    }

    public abstract void off ();

    public abstract void off (Class<Event> event);

    public abstract <E extends Event> void off (Class<E> event, Consumer<E> listener);
    
}
