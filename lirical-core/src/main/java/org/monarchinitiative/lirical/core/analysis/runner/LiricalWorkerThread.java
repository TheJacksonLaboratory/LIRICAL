package org.monarchinitiative.lirical.core.analysis.runner;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link ForkJoinWorkerThread} with a custom name for pretty logging output.
 */
class LiricalWorkerThread extends ForkJoinWorkerThread {

    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    LiricalWorkerThread(ForkJoinPool pool) {
        super(pool);
        setName("lirical-worker-" + WORKER_COUNTER.incrementAndGet());
    }
}
