package org.monarchinitiative.lirical.core.service;

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
