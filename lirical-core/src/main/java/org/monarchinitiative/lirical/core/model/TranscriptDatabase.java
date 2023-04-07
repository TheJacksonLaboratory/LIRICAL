package org.monarchinitiative.lirical.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public enum TranscriptDatabase {
    UCSC,
    REFSEQ;

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptDatabase.class);

    @Override
    public String toString() {
        return switch (this) {
            case UCSC -> "ucsc";
            case REFSEQ -> "RefSeq";
        };
    }

    public static Optional<TranscriptDatabase> parse(String value) {
        return switch (value.toLowerCase()) {
            case "ucsc" -> Optional.of(UCSC);
            case "refseq" -> Optional.of(REFSEQ);
            default -> {
                LOGGER.warn("Unknown transcript database");
                yield Optional.empty();
            }
        };
    }
}
