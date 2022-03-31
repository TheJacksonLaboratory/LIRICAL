package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.model.GenomeBuild;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.lirical.service.VariantMetadataService;

import java.util.Objects;

public class Lirical {

    private final GenomeBuild genomeBuild;
    private final VariantMetadataService variantMetadataService;
    private final PhenotypeService phenotypeService;
    private final LiricalAnalysisRunner analyzer;

    public static Lirical of(GenomeBuild genomeBuild,
                             VariantMetadataService variantMetadataService,
                             PhenotypeService phenotypeService,
                             LiricalAnalysisRunner analyzer) {
        return new Lirical(genomeBuild, variantMetadataService, phenotypeService, analyzer);
    }

    private Lirical(GenomeBuild genomeBuild, VariantMetadataService variantMetadataService,
                    PhenotypeService phenotypeService,
                    LiricalAnalysisRunner analyzer) {
        this.genomeBuild = Objects.requireNonNull(genomeBuild);
        this.variantMetadataService = Objects.requireNonNull(variantMetadataService);
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    public GenomeBuild genomeBuild() {
        return genomeBuild;
    }

    public VariantMetadataService variantMetadataService() {
        return variantMetadataService;
    }

    public PhenotypeService phenotypeService() {
        return phenotypeService;
    }

    public LiricalAnalysisRunner analyzer() {
        return analyzer;
    }

}
