package org.monarchinitiative.lirical.core.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for pretty progress reporting using logger. The {@link ProgressReporter} is thread-safe.
 */
public class ProgressReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressReporter.class);
    private static final NumberFormat NUMBER_FORMAT;

    private static final String DEFAULT_COLLECTION_ITEM_NAME = "items";

    static {
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    /**
     * We report each n-th instance.
     */
    private final int tick;
    private final String progressTemplate;
    private final String summaryTemplate;
    private final Instant start;
    private final AtomicReference<Instant> localStart;
    private final AtomicInteger count = new AtomicInteger();

    /**
     * Report each 5,000-th item using {@link #DEFAULT_COLLECTION_ITEM_NAME}.
     */
    public ProgressReporter() {
        this(5_000);
    }

    /**
     * Report each <em>n</em>-th item using {@link #DEFAULT_COLLECTION_ITEM_NAME}.
     *
     * @param tick a positive integer.
     */
    public ProgressReporter(int tick) {
        this(tick, DEFAULT_COLLECTION_ITEM_NAME);
    }

    /**
     * Report each <em>n</em>-th item using provided progress message template.
     *
     * @param tick a positive integer used to report each <em>n</em>-th item.
     * @param itemTemplateName plural noun denoting the elements being processing (e.g. items, variants, diseases).
     */
    public ProgressReporter(int tick, String itemTemplateName) {
        if (tick <= 0)
            throw new IllegalArgumentException("Tick must be positive: %d".formatted(tick));
        this.tick = tick;
        this.progressTemplate = "Processed {} %s at {} %s/s".formatted(Objects.requireNonNull(itemTemplateName), Objects.requireNonNull(itemTemplateName));
        this.summaryTemplate = "Processed {} %s in {}m {}s ({} total ms) at {} %s/s".formatted(itemTemplateName, itemTemplateName);
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
            LOGGER.info(progressTemplate, NUMBER_FORMAT.format(current), NUMBER_FORMAT.format((tick * 1000.) / ms));
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
        LOGGER.info(summaryTemplate,
                NUMBER_FORMAT.format(count.get()),
                mins,
                seconds,
                totalMillis,
                NUMBER_FORMAT.format(itemsPerSecond));

    }
}
