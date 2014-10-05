package async.promise;

import java.util.function.Consumer;
import java.util.function.Function;

import java.util.function.Supplier;

import async.AsyncContext;
import es.darkhogg.lowendcoll.LowEndCollection;
import es.darkhogg.lowendcoll.impl.LowEndArrayBag;

public final class Promise<T> {

    private volatile T value;
    private volatile Throwable rejection;

    private volatile boolean pending = true;

    private volatile LowEndCollection<Consumer<T>> thens;
    private volatile LowEndCollection<Deferred<?>> deferreds;
    private volatile LowEndCollection<Consumer<Throwable>> excepts;
    private volatile LowEndCollection<Runnable> alwayses;

    private synchronized <R> Promise<R> thenImpl (Function<T,R> func) {
        // Create the deferred for the chained promise
        Deferred<R> next = new Deferred<>();

        // Wrap the passed function so that it fulfills or rejects the deferred
        Consumer<T> wrap = (val) -> {
            try {
                R res = func.apply(val);
                next.fulfill(res);
            } catch (Throwable reason) {
                next.reject(reason);
            }
        };

        if (pending) {
            // Add the wrapped function to the list of "thens"
            if (thens == null) {
                thens = new LowEndArrayBag<>();
            }
            thens.add(wrap);

            // Store the promise for rejection chaining
            if (deferreds == null) {
                deferreds = new LowEndArrayBag<>();
            }
            deferreds.add(next);
        } else {
            // Perform the appropriate action immediately
            if (rejection == null) {
                AsyncContext.current().event( () -> wrap.accept(value));

            } else {
                next.reject(rejection);
            }
        }

        // Return the promise
        return next.promise();
    }
    
    /**
     * Adds an action to be performed when this promise is fulfilled. The resulting promise will be fulfilled with the
     * result of the action or its thrown exception. If this promise is rejected, the returned one will also be rejected
     * with the same reason.
     * 
     * @param func Function to run after this promise is fulfilled
     * @return A promise to the return value of <tt>func</tt>
     */
    public synchronized <R> Promise<R> then (Function<T,R> func) {
        return thenImpl(func);
    }

    public synchronized <R> Promise<R> then (Supplier<R> func) {
        return thenImpl( (ignored) -> {
            return func.get();
        });
    }
    
    public synchronized Promise<Void> then (Consumer<T> func) {
        return thenImpl((val) -> {
            func.accept(val);
            return null;
        });
    }
    
    public synchronized Promise<Void> then (Runnable func) {
        return thenImpl((ignore) -> {
            func.run();
            return null;
        });
    }

    // ==================
    // === INTER-COMM ===

    /* package */synchronized void fulfill (T value) {
        if (!pending) {
            throw new IllegalStateException("already resolved");
        }

        // Resolve
        this.pending = false;

        // Store resolution value
        this.value = value;
        this.rejection = null;

        // Schedule all "thens" to be run
        AsyncContext ctx = AsyncContext.current();
        for (Consumer<T> then : thens) {
            ctx.event( () -> then.accept(value));
        }

        // Clear collections (not needed anymore)
        clearCollections();
    }

    /* package */synchronized void reject (Throwable reason) {
        if (!pending) {
            throw new IllegalStateException("already resolved");
        }
        if (reason == null) {
            throw new NullPointerException("cannot reject without a reason");
        }

        // Resolve
        this.pending = false;

        // Store resolution reason
        this.value = null;
        this.rejection = reason;

        // Schedule all "excepts" to be run
        AsyncContext ctx = AsyncContext.current();
        for (Consumer<Throwable> except : excepts) {
            ctx.event( () -> except.accept(rejection));
        }

        // Reject every pending deferred
        for (Deferred<?> deferred : deferreds) {
            deferred.reject(rejection);
        }

        // Clear collections (not needed anymore)
        clearCollections();
    }

    // =================
    // === INTERNALS ===

    private void clearCollections () {
        thens.clear();
        thens = null;

        deferreds.clear();
        deferreds = null;

        excepts.clear();
        excepts = null;

        alwayses.clear();
        alwayses = null;
    }
}
