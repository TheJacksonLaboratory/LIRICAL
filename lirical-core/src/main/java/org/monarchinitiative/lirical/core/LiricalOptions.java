package org.monarchinitiative.lirical.core;

import java.util.Optional;

/**
 * Options of the LIRICAL process that live independent of the analyses.
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
