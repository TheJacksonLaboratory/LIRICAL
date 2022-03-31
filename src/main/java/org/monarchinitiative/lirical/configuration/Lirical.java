package org.monarchinitiative.lirical.configuration;

import org.monarchinitiative.lirical.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.lirical.service.PhenotypeService;

import java.util.Objects;
import java.util.Optional;

public class Lirical {

    private final VariantParserFactory variantParserFactory; // nullable
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
        this.variantParserFactory = variantParserFactory;
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    public Optional<VariantParserFactory> variantParserFactory() {
        return Optional.ofNullable(variantParserFactory);
    }

    public PhenotypeService phenotypeService() {
        return phenotypeService;
    }

    public LiricalAnalysisRunner analyzer() {
        return analyzer;
    }

}
