package be.kuleuven.stgp.mip.util;

import be.kuleuven.stgp.mip.column_generation.*;

import java.util.concurrent.*;

public class ThreadPool extends ThreadPoolExecutor {

    private final static ThreadPool singleton = new ThreadPool();

    public static ThreadPool get() {
        return singleton;
    }

    private final int maxThreads;
    public final Semaphore semaphore;
    //private AtomicInteger counter = new AtomicInteger(0);

    /**
     * Private constructor used to create the singleton object instance.
     */
    private ThreadPool() {
        super(Parameters.get().nThreads, Parameters.get().nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        maxThreads = Parameters.get().nThreads;
        semaphore = new Semaphore(maxThreads);
    }

    public void acquire() throws InterruptedException {
        //counter.incrementAndGet();
        semaphore.acquire();
    }

    public void acquire(int nThreads) throws InterruptedException {
        //counter.addAndGet(nThreads);
        semaphore.acquire(nThreads);
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void release() {
        semaphore.release();
    }

    public void release(int nThreads) {
        semaphore.release(nThreads);
    }
}
