package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.lirical.service.VariantMetadataService;

public class Lirical {

    private final VariantMetadataService variantMetadataService;
    private final PhenotypeService phenotypeService;
    private final LiricalAnalysisRunner analyzer;

    public static Lirical of(VariantMetadataService variantMetadataService,
                             PhenotypeService phenotypeService,
                             LiricalAnalysisRunner analyzer) {
        return new Lirical(variantMetadataService, phenotypeService, analyzer);
    }

    private Lirical(VariantMetadataService variantMetadataService,
                    PhenotypeService phenotypeService,
                    LiricalAnalysisRunner analyzer) {
        this.variantMetadataService = variantMetadataService;
        this.phenotypeService = phenotypeService;
        this.analyzer = analyzer;
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
