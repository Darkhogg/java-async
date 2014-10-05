package async.promise;

/**
 * An object that represents a deferred value. In essence, this class is the producer part of a {@link Promise}, used
 * only for promise creation.
 * 
 * @author Daniel Escoz
 * @param <T> Type of the deferred value
 */
public final class Deferred<T> {

    /** The promise of the deferred value */
    private final Promise<T> promise;

    public Deferred () {
        promise = new Promise<>();
    }

    /** @return A promise to the eventual value of this deferred object */
    public Promise<T> promise () {
        return promise;
    }

    /**
     * Fulfill the deferred object with the given <tt>value</tt>, which will be the completion value of the
     * {@link #promise() associated promise}.
     * 
     * @param value Fulfillment value of this deferred object
     */
    public void fulfill (T value) {
        promise.fulfill(value);
    }

    /**
     * Reject the deferred object with the given <tt>reason</tt>, which will be the rejection reason of the
     * {@link #promise() associated promise}.
     * 
     * @param reason
     */
    public void reject (Throwable reason) {
        promise.reject(reason);
    }

}
