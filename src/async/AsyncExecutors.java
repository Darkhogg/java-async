package async;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class AsyncExecutors {

    public static ThreadPoolExecutor newEventExecutor (ThreadFactory threadFactory) {
        ThreadPoolExecutor exec =
            new ThreadPoolExecutor(1, 1, Integer.MAX_VALUE, TimeUnit.DAYS, new LinkedBlockingDeque<>(), threadFactory);
        exec.allowCoreThreadTimeOut(true);
        return exec;
    }

    public static ThreadPoolExecutor newWorkerExecutor (ThreadFactory threadFactory) {
        ThreadPoolExecutor exec =
            new ThreadPoolExecutor(
                0, Integer.MAX_VALUE, 10, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory);
        return exec;
    }

}
