package org.monarchinitiative.lirical.bootstrap;

import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.io.VariantParserFactory;

import java.util.Objects;
import java.util.Optional;

public class Lirical {

    private final VariantParserFactory variantParserFactory; // nullable
    private final PhenotypeService phenotypeService;
    private final LiricalAnalysisRunner analyzer;
    private final AnalysisResultWriterFactory analysisResultWriterFactory;

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             LiricalAnalysisRunner analyzer,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        return new Lirical(variantParserFactory, phenotypeService, analyzer, analysisResultWriterFactory);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    LiricalAnalysisRunner analyzer,
                    AnalysisResultWriterFactory analysisResultWriterFactory) {
        this.variantParserFactory = variantParserFactory;
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.analyzer = Objects.requireNonNull(analyzer);
        this.analysisResultWriterFactory = Objects.requireNonNull(analysisResultWriterFactory);
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

    public AnalysisResultWriterFactory analysisResultsWriterFactory() {
        return analysisResultWriterFactory;
    }
}
