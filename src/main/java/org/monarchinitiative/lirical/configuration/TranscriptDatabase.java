package org.monarchinitiative.lirical.configuration;

public enum TranscriptDatabase {
    UCSC,ENSEMBL,REFSEQ;
    @Override
    public String toString() {
        switch (this) {
            case UCSC:return "ucsc";
            case REFSEQ:return "RefSeq";
            case ENSEMBL:return "Ensembl";
            default:return "unknown";//should never happen.
        }
    }
}
