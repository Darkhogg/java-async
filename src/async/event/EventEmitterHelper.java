package async.event;

import java.util.function.Consumer;

import async.AsyncContext;
import es.darkhogg.lowendcoll.LowEndMap;
import es.darkhogg.lowendcoll.LowEndSet;
import es.darkhogg.lowendcoll.impl.LowEndHashMap;
import es.darkhogg.lowendcoll.impl.LowEndHashSet;

public class EventEmitterHelper implements EventEmitter {

    private final LowEndMap<Class<? extends Event>,LowEndSet<Consumer<? extends Event>>> listeners =
        new LowEndHashMap<>();

    @Override
    public <E extends Event> void on (Class<E> event, Consumer<E> listener) {
        if (!listeners.containsKey(event)) {
            listeners.put(event, new LowEndHashSet<>());
        }

        listeners.get(event).add(listener);
    }

    @Override
    public void off () {
        listeners.clear();
    }

    @Override
    public void off (Class<Event> event) {
        listeners.remove(event);
    }

    @Override
    public <E extends Event> void off (Class<E> event, Consumer<E> listener) {
        if (listeners.containsKey(event)) {
            listeners.get(event).remove(listener);
        }
    }
    
    
    public void fire (Event event) {
        Class<? extends Event> type = event.getClass();
        
        if (listeners.containsKey(type)) {
            for (Consumer<? extends Event> listener : listeners.get(type)) {
                // Not a problem, as types were checked at insertion time
                @SuppressWarnings("unchecked")
                Consumer<Event> consumer = (Consumer<Event>) listener;
                
                AsyncContext.current().event(() -> consumer.accept(event));
            }
        }
    }

}
