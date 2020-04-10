package kilanny.shamarlymushaf.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static AppExecutors instance;

    public static synchronized AppExecutors getInstance() {
        if (instance == null)
            instance = new AppExecutors();
        return instance;
    }

    private final Executor mCachedExecutor, mCpuCoresExecutor;

    public void executeOnCachedExecutor(Runnable runnable) {
        mCachedExecutor.execute(runnable);
    }

    public void executeOnCpuCoresExecutor(Runnable runnable) {
        mCpuCoresExecutor.execute(runnable);
    }

    private AppExecutors() {
        mCachedExecutor = Executors.newCachedThreadPool();
        int nThreads = Utils.getCpuCoreCount(true);
        mCpuCoresExecutor = Executors.newFixedThreadPool(nThreads);
    }
}
