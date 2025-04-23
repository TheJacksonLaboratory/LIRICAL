package org.monarchinitiative.lirical.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * A convenience class for accessing resources from LIRICAL data directory.
 * <p>
 * The resource files include:
 * <ul>
 *     <li>HPO in JSON format (<em>hp.json</em>)</li>
 *     <li>HPO annotations (<em>phenotype.hpoa</em>)</li>
 *     <li>HGNC complete set - a table with gene identifiers, symbols, and other metadata (<em>hgnc_complete_set.txt</em>)</li>
 *     <li>MIM to medgene mapping file (<em>mim2gene_medgen</em>)</li>
 *     <li>Orpha to gene mapping file (<em>en_product6.xml</em>). Optional</li>
 *     <li>Jannovar caches (<em>hg(19|38)_(ucsc|refseq).ser</em>)</li>
 * </ul>
 * <p>
 * The resolver checks if all non-optional files are present and will raise an exception otherwise.
 */
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
        List<Path> requiredFiles = List.of(
                hpoJson(), hgncCompleteSet(), mim2geneMedgen(), phenotypeAnnotations(),
                hg19RefseqTxDatabase(), hg19RefseqCuratedTxDatabase(), hg19EnsemblTxDatabase(), hg19UcscTxDatabase(),
                hg38RefseqTxDatabase(), hg38RefseqCuratedTxDatabase(), hg38EnsemblTxDatabase(), hg38UcscTxDatabase()
        );
        // Note: we do not require `orpha2gene` in all analyses, hence it is not required!
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

    public Path hpoJson() {
        return dataDirectory.resolve("hp.json");
    }

    public Path orpha2gene() {
        return dataDirectory.resolve("en_product6.xml");
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

    public Path hg19RefseqCuratedTxDatabase() {
        return dataDirectory.resolve("hg19_refseq_curated.ser");
    }

    public Path hg19EnsemblTxDatabase() {
        return dataDirectory.resolve("hg19_ensembl.ser");
    }

    public Path hg38UcscTxDatabase() {
        return dataDirectory.resolve("hg38_ucsc.ser");
    }

    public Path hg38RefseqTxDatabase() {
        return dataDirectory.resolve("hg38_refseq.ser");
    }

    public Path hg38RefseqCuratedTxDatabase() {
        return dataDirectory.resolve("hg38_refseq_curated.ser");
    }

    public Path hg38EnsemblTxDatabase() {
        return dataDirectory.resolve("hg38_ensembl.ser");
    }

    public Path transcriptCacheFor(GenomeBuild genomeBuild, TranscriptDatabase txDb) {
        return switch (genomeBuild) {
            case HG19 -> switch (txDb) {
                case UCSC -> hg19UcscTxDatabase();
                case ENSEMBL -> hg19EnsemblTxDatabase();
                case REFSEQ -> hg19RefseqTxDatabase();
                case REFSEQ_CURATED -> hg19RefseqCuratedTxDatabase();
            };
            case HG38 -> switch (txDb) {
                case UCSC -> hg38UcscTxDatabase();
                case ENSEMBL -> hg38EnsemblTxDatabase();
                case REFSEQ -> hg38RefseqTxDatabase();
                case REFSEQ_CURATED -> hg38RefseqCuratedTxDatabase();
            };
        };
    }
}
