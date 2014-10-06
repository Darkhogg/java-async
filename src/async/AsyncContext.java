package async;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import async.promise.Deferred;
import async.promise.Promise;

/**
 * Runtime context which includes the event loop and worker threads.
 * 
 * @author Daniel Escoz
 */
public final class AsyncContext {

    /* package */static final ThreadLocal<AsyncContext> threadContexts = new ThreadLocal<>();

    private volatile ThreadPoolExecutor eventLoop = AsyncExecutors.newEventExecutor(new AsyncContextThreadFactory());;
    private volatile ThreadPoolExecutor workerPool = AsyncExecutors.newWorkerExecutor(new AsyncContextThreadFactory());;

    private AsyncContext () {

    }

    /**
     * Checks the state of the internal executors and acts accordingly
     */
    private synchronized void checkExecutors () {
        int pendingEvents = eventLoop.getActiveCount() + eventLoop.getQueue().size();
        int pendingWorkers = workerPool.getActiveCount() + workerPool.getQueue().size();

        /* If there are no pending workers, set worker threads to terminate early */
        if (pendingWorkers == 0) {
            workerPool.setKeepAliveTime(1, TimeUnit.SECONDS);
        } else {
            workerPool.setKeepAliveTime(10, TimeUnit.SECONDS);
        }

        /* If there are no pending workers or events, set event thread to terminate early */
        if (pendingWorkers + pendingEvents == 0) {
            eventLoop.setKeepAliveTime(1, TimeUnit.MILLISECONDS);
            workerPool.setKeepAliveTime(1, TimeUnit.MILLISECONDS);
        } else {
            eventLoop.setKeepAliveTime(Integer.MAX_VALUE, TimeUnit.DAYS);
        }
    }

    /**
     * Runs the given <tt>task</tt> in the event thread of this context and returns a promise of its result.
     * 
     * @param task The task to be run
     * @return A promise of the task result
     */
    public <T> Promise<T> event (Supplier<T> task) {
        Deferred<T> def = new Deferred<T>();
        eventLoop.submit( () -> {
            try {
                T ret = task.get();
                def.fulfill(ret);
            } catch (Throwable thr) {
                def.reject(thr);
            } finally {
                checkExecutors();
            }
        });
        return def.promise();
    }

    /**
     * Runs the given <tt>task</tt> in the event thread of this context and returns a promise of its completion.
     * 
     * @param task The task to be run
     * @return A promise of the task completion
     */
    public Promise<Void> event (Runnable task) {
        return event( () -> {
            task.run();
            return null;
        });
    }

    /**
     * Runs the given <tt>task</tt> in a worker thread and returns a promise of its result.
     * 
     * @param task The task to be run
     * @return A promise of the task result
     */
    public <T> Promise<T> worker (Supplier<T> task) {
        Deferred<T> def = new Deferred<T>();
        workerPool.submit( () -> {
            try {
                T ret = task.get();
                def.fulfill(ret);
            } catch (Throwable thr) {
                def.reject(thr);
            } finally {
                checkExecutors();
            }
        });
        return def.promise();
    }

    /**
     * Runs the given <tt>task</tt> in a worker thread and returns a promise of its completion.
     * 
     * @param task The task to be run
     * @return A promise of the task completion
     */
    public Promise<Void> worker (Runnable task) {
        return worker( () -> {
            task.run();
            return null;
        });
    }

    public Promise<Void> delay (long time, TimeUnit unit) {
        return worker( () -> {
            try {
                unit.sleep(time);
            } catch (InterruptedException e) {
                /* FIXME should throw */
            }
        });
    }

    /** @return the context associated with the current thread */
    public static AsyncContext current () {
        return threadContexts.get();
    }

    /**
     * Create a new <tt>AsyncContext</tt> and run the given task on the event thread.
     * 
     * @param main The first event to be run
     * @return A new context in which <tt>task</tt> will be run
     */
    public static AsyncContext bootstrap (Runnable main) {
        AsyncContext ctx = new AsyncContext();
        ctx.event(main);
        return ctx;
    }

    /**
     * A thread factory that decorates the passed runnables to store the {@link AsyncContext} related to the created
     * thread for correct operation of {@link #current}.
     * 
     * @author Daniel Escoz
     */
    private class AsyncContextThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread (Runnable r) {
            Thread t = new Thread( () -> {
                threadContexts.set(AsyncContext.this);
                System.out.println(">> " + Thread.currentThread().getName());
                try {
                    r.run();
                } finally {
                    System.out.println("<< " + Thread.currentThread().getName());
                    checkExecutors();
                }
            });
            return t;
        }

    }
}
