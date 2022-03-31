package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.lirical.service.PhenotypeService;
import org.monarchinitiative.lirical.service.VariantMetadataService;

import java.util.Objects;

public class Lirical {

    private final VariantParserFactory variantParserFactory;
    private final PhenotypeService phenotypeService;
    private final LiricalAnalysisRunner analyzer;

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             LiricalAnalysisRunner analyzer) {
        return new Lirical(variantParserFactory, phenotypeService, analyzer);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    LiricalAnalysisRunner analyzer) {
        this.variantParserFactory = Objects.requireNonNull(variantParserFactory);
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    public VariantParserFactory variantParserFactory() {
        return variantParserFactory;
    }

    public PhenotypeService phenotypeService() {
        return phenotypeService;
    }

    public LiricalAnalysisRunner analyzer() {
        return analyzer;
    }

}
