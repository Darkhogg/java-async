package async.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import async.promise.Deferred;
import async.promise.Promise;

public final class Promises {
    
    /**
     * Returns a promise that is already fulfilled with the passed <tt>value</tt>.
     * 
     * @param value The value to which the promise is fulfilled
     * @return An already fulfilled promise
     */
    public static <T> Promise<T> fulfilled (T value) {
        Deferred<T> deferred = new Deferred<>();
        deferred.fulfill(value);
        return deferred.promise();
    }
    
    /**
     * Returns a promise that is already rejected with the passed <tt>reason</tt>
     * 
     * @param reason The rejection reason of the promise
     * @return An already rejected promise
     */
    public static <T> Promise<T> rejected (Throwable reason) {
        Deferred<T> deferred = new Deferred<>();
        deferred.reject(reason);
        return deferred.promise();
    }

    /**
     * Returns a promise to the list of the result of 
     * @param promises
     * @return
     */
    @SafeVarargs
    public static <T> Promise<List<T>> all (Promise<? extends T>... promises) {
        if (promises.length == 0) {
            return fulfilled(Collections.emptyList());
        }
        
        @SuppressWarnings("unchecked")
        List<T> values = Arrays.asList((T[]) new Object[promises.length]);

        Deferred<List<T>> deferred = new Deferred<>();
        AtomicInteger remaining = new AtomicInteger(promises.length);

        for (int i = 0; i < promises.length; i++) {
            final int n = i;
            promises[i].then((T res) -> {
                values.set(n, res);
                
                if (remaining.decrementAndGet() == 0) {
                    deferred.fulfill(values);
                }
            }).except((reason) -> {
                deferred.reject(reason);
            });
        }
        
        return deferred.promise();
    }
    
}
