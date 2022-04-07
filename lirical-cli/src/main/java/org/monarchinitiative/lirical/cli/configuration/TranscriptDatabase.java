package org.monarchinitiative.lirical.cli.configuration;

public enum TranscriptDatabase {
    UCSC,REFSEQ;
    @Override
    public String toString() {
        switch (this) {
            case UCSC:return "ucsc";
            case REFSEQ:return "RefSeq";
            default:return "unknown";//should never happen.
        }
    }
}
