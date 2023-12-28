package org.monarchinitiative.lirical.core;

import java.util.Optional;

/**
 * Global options to parameterize LIRICAL execution.
 * <p>
 * Note, these options do <em>not</em> parameterize the analyses.
 */
public class LiricalOptions {

    private final String version; // nullable
    private final int parallelism;

    public LiricalOptions(String version, int parallelism) {
        this.version = version;
        this.parallelism = parallelism;
    }

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public int parallelism() {
        return parallelism;
    }
}
