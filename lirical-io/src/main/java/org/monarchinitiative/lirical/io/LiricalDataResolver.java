package org.monarchinitiative.lirical.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class LiricalDataResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalDataResolver.class);

    private final Path dataDirectory;

    public static LiricalDataResolver of(Path dataDirectory) throws LiricalDataException {
        return new LiricalDataResolver(dataDirectory);
    }

    private LiricalDataResolver(Path dataDirectory) throws LiricalDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory must not be null!");
        checkV1Resources();
    }

    private void checkV1Resources() throws LiricalDataException {
        boolean error = false;
        List<Path> requiredFiles = List.of(hpoJson(), hgncCompleteSet(), mim2geneMedgen(), phenotypeAnnotations(),
                hg19RefseqTxDatabase(), hg19UcscTxDatabase(), hg38RefseqTxDatabase(), hg38UcscTxDatabase());
        for (Path file : requiredFiles) {
            if (!Files.isRegularFile(file)) {
                LOGGER.error("Missing required file `{}` in `{}`.", file.toFile().getName(), dataDirectory.toAbsolutePath());
                error = true;
            }
        }
        if (error) {
            throw new LiricalDataException("Missing one or more resource files in Lirical data directory!");
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    /**
     * @deprecated use {@link #hpoJson()}
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    public Path hpoObo() {
        return dataDirectory.resolve("hp.obo");
    }

    public Path hpoJson() {
        return dataDirectory.resolve("hp.json");
    }

    /**
     *
     * @deprecated to be removed in v2.0.0, use {@link #hgncCompleteSet()} as a source of gene identifiers.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    public Path homoSapiensGeneInfo() {
        return dataDirectory.resolve("Homo_sapiens.gene_info.gz");
    }

    public Path hgncCompleteSet() {
        return dataDirectory.resolve("hgnc_complete_set.txt");
    }

    public Path mim2geneMedgen() {
        return dataDirectory.resolve("mim2gene_medgen");
    }

    public Path phenotypeAnnotations() {
        return dataDirectory.resolve("phenotype.hpoa");
    }

    public Path hg19UcscTxDatabase() {
        return dataDirectory.resolve("hg19_ucsc.ser");
    }

    public Path hg19RefseqTxDatabase() {
        return dataDirectory.resolve("hg19_refseq.ser");
    }

    public Path hg38RefseqTxDatabase() {
        return dataDirectory.resolve("hg38_refseq.ser");
    }

    public Path hg38UcscTxDatabase() {
        return dataDirectory.resolve("hg38_ucsc.ser");
    }

    /**
     * @deprecated use {@link #transcriptCacheFor(GenomeBuild, org.monarchinitiative.lirical.core.model.TranscriptDatabase)} instead
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    public Path transcriptCacheFor(GenomeBuild genomeBuild, TranscriptDatabase txDb) {
        return switch (genomeBuild) {
            case HG19 -> switch (txDb) {
                case UCSC -> hg19UcscTxDatabase();
                case REFSEQ -> hg19RefseqTxDatabase();
            };
            case HG38 -> switch (txDb) {
                case UCSC -> hg38UcscTxDatabase();
                case REFSEQ -> hg38RefseqTxDatabase();
            };
        };
    }

    public Path transcriptCacheFor(GenomeBuild genomeBuild, org.monarchinitiative.lirical.core.model.TranscriptDatabase txDb) {
        return switch (genomeBuild) {
            case HG19 -> switch (txDb) {
                case UCSC -> hg19UcscTxDatabase();
                case REFSEQ -> hg19RefseqTxDatabase();
            };
            case HG38 -> switch (txDb) {
                case UCSC -> hg38UcscTxDatabase();
                case REFSEQ -> hg38RefseqTxDatabase();
            };
        };
    }
}
