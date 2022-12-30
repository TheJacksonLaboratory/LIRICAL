package org.monarchinitiative.lirical.core.service;

/**
 * @deprecated use {@link org.monarchinitiative.lirical.core.model.TranscriptDatabase} instead.
 */
@Deprecated(since = "2.0.0-RC2", forRemoval = true)
public enum TranscriptDatabase {
    UCSC, REFSEQ;

    @Override
    public String toString() {
        return switch (this) {
            case UCSC -> "ucsc";
            case REFSEQ -> "RefSeq";
        };
    }
}
