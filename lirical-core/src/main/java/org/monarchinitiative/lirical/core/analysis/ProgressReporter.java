package org.monarchinitiative.lirical.core.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for pretty progress reporting using logger. The {@link ProgressReporter} is thread-safe.
 */
public class ProgressReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressReporter.class);
    private static final NumberFormat NUMBER_FORMAT;

    static {
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    /**
     * We report each n-th instance.
     */
    private final int tick;
    private final Instant start;
    private final AtomicReference<Instant> localStart;
    private final AtomicInteger count = new AtomicInteger();

    /**
     * Report each 5,000-th item.
     */
    public ProgressReporter() {
        this(5_000);
    }

    /**
     * Report each <em>n</em>-th item.
     *
     * @param tick a positive integer.
     */
    public ProgressReporter(int tick) {
        if (tick <= 0)
            throw new IllegalArgumentException("Tick must be positive: %d".formatted(tick));
        this.tick = tick;
        this.start = Instant.now();
        this.localStart = new AtomicReference<>(start);
    }

    /**
     * Log processing of an item.
     */
    public void log() {
        int current = count.incrementAndGet();
        if (current % tick == 0) {
            Instant end = Instant.now();
            Instant start = localStart.getAndSet(end);
            Duration duration = Duration.between(start, end);
            long ms = duration.toMillis();
            LOGGER.info("Processed {} items at {} items/s", NUMBER_FORMAT.format(current), NUMBER_FORMAT.format(((double) tick * 1000) / ms));
        }
    }

    /**
     * Log summary after processing.
     */
    public void summarize() {
        Duration duration = Duration.between(start, Instant.now());
        long totalMillis = duration.toMillis();
        double items = count.get();
        double itemsPerSecond = (items * 1000) / totalMillis;
        long mins = (totalMillis / 1000) / 60 % 60;
        long seconds = totalMillis / 1000 % 60;
        LOGGER.info("Processed {} items in {}m {}s ({} totalMillis) at {} items/s",
                NUMBER_FORMAT.format(count.get()),
                mins,
                seconds,
                totalMillis,
                NUMBER_FORMAT.format(itemsPerSecond));

    }
}
