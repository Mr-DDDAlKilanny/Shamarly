package kilanny.shamarlymushaf.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class AppExecutors {

    private static AppExecutors instance;

    public static synchronized AppExecutors getInstance() {
        if (instance == null)
            instance = new AppExecutors();
        return instance;
    }

    private final Executor mCachedExecutor;

    public synchronized void executeOnCachedExecutor(Runnable runnable) {
        mCachedExecutor.execute(runnable);
    }

    private AppExecutors() {
        mCachedExecutor = Executors.newCachedThreadPool();
    }
}
