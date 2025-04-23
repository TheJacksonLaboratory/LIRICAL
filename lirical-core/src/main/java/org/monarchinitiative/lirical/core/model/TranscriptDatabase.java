package org.monarchinitiative.lirical.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Transcript database provides a collection of transcript definitions to use for functional variant annotation.
 */
public enum TranscriptDatabase {
    /**
     * Transcripts sourced from <a href="https://hgdownload.soe.ucsc.edu/downloads.html">UCSC</a> Genome Browser.
     */
    UCSC,

    /**
     * Transcripts sourced from <a href="https://www.ensembl.org/info/data/index.html">ENSEMBL</a>.
     */
    ENSEMBL,

    /**
     * RefSeq transcripts, including curated transcripts (<code>NM_</code>)
     * as well as the transcripts that are based on gene predictions (<code>XM_</code>).
     */
    REFSEQ,

    /**
     * RefSeq with curated transcripts only (<code>NM_</code>)
     * and <em>excluding</em> the transcripts that are based on gene predictions (<code>XM_</code>).
     */
    REFSEQ_CURATED;

    private static final Logger LOGGER = LoggerFactory.getLogger(TranscriptDatabase.class);

    @Override
    public String toString() {
        return switch (this) {
            case UCSC -> "UCSC";
            case ENSEMBL -> "Ensembl";
            case REFSEQ -> "RefSeq";
            case REFSEQ_CURATED -> "RefSeq curated";
        };
    }

    public static Optional<TranscriptDatabase> parse(String value) {
        return switch (value.toLowerCase()) {
            case "ucsc" -> Optional.of(UCSC);
            case "ensembl" -> Optional.of(ENSEMBL);
            case "refseq" -> Optional.of(REFSEQ);
            case "refseq_curated" -> Optional.of(REFSEQ_CURATED);
            default -> {
                LOGGER.warn("Unknown transcript database");
                yield Optional.empty();
            }
        };
    }
}
