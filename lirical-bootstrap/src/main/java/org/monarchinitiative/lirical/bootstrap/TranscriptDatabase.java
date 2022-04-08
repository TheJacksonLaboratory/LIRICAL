package org.monarchinitiative.lirical.bootstrap;

public enum TranscriptDatabase {
    UCSC, REFSEQ;

    @Override
    public String toString() {
        return switch (this) {
            case UCSC -> "ucsc";
            case REFSEQ -> "RefSeq";
            default -> "unknown";//should never happen.
        };
    }
}
