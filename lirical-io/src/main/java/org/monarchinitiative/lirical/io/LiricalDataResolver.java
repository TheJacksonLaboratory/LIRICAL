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
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory must not be null");
        LOGGER.debug("Using Lirical directory at {}", dataDirectory.toAbsolutePath());
        checkV1Resources();
    }

    private void checkV1Resources() throws LiricalDataException {
        boolean error = false;
        List<Path> requiredFiles = List.of(hpoJson(), homoSapiensGeneInfo(), mim2geneMedgen(), phenotypeAnnotations(),
                hg19RefseqTxDatabase(), hg19UcscTxDatabase(), hg38RefseqTxDatabase(), hg38UcscTxDatabase());
        for (Path file : requiredFiles) {
            if (!Files.isRegularFile(file)) {
                LOGGER.error("Missing required file {} in {}", file.toFile().getName(), dataDirectory.toAbsolutePath());
                error = true;
            }
        }
        if (error) {
            throw new LiricalDataException("Missing one or more resource files in Lirical data directory");
        }
    }

    /**
     * @deprecated use {@link #hpoJson()}
     */
    @Deprecated(forRemoval = true)
    public Path hpoObo() {
        return dataDirectory.resolve("hp.obo");
    }

    public Path hpoJson() {
        return dataDirectory.resolve("hp.json");
    }

    public Path homoSapiensGeneInfo() {
        return dataDirectory.resolve("Homo_sapiens.gene_info.gz");
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

    private Path hg38RefseqTxDatabase() {
        return dataDirectory.resolve("hg38_refseq.ser");
    }

    private Path hg38UcscTxDatabase() {
        return dataDirectory.resolve("hg38_ucsc.ser");
    }

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
}
