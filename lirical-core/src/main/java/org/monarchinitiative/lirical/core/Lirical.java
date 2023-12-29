package org.monarchinitiative.lirical.core;

import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.impl.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;

import java.util.Objects;
import java.util.Optional;

public class Lirical {

    private final VariantParserFactory variantParserFactory;
    private final PhenotypeService phenotypeService;
    private final FunctionalVariantAnnotatorService functionalVariantAnnotatorService;
    private final VariantMetadataServiceFactory variantMetadataServiceFactory;
    private final LiricalAnalysisRunner analysisRunner;
    private final AnalysisResultWriterFactory analysisResultWriterFactory;
    private final LiricalOptions options;

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                             VariantMetadataServiceFactory variantMetadataService,
                             FunctionalVariantAnnotatorService functionalVariantAnnotatorService,
                             AnalysisResultWriterFactory analysisResultWriterFactory,
                             LiricalOptions options) {
        return new Lirical(variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataService,
                functionalVariantAnnotatorService,
                analysisResultWriterFactory,
                options);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                    VariantMetadataServiceFactory variantMetadataServiceFactory,
                    FunctionalVariantAnnotatorService functionalVariantAnnotatorService,
                    AnalysisResultWriterFactory analysisResultWriterFactory,
                    LiricalOptions options) {
        this.variantParserFactory = Objects.requireNonNull(variantParserFactory);
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.variantMetadataServiceFactory = Objects.requireNonNull(variantMetadataServiceFactory);
        this.functionalVariantAnnotatorService = Objects.requireNonNull(functionalVariantAnnotatorService);
        this.options = Objects.requireNonNull(options);
        this.analysisRunner = LiricalAnalysisRunnerImpl.of(phenotypeService, backgroundVariantFrequencyServiceFactory, options.parallelism());
        this.analysisResultWriterFactory = Objects.requireNonNull(analysisResultWriterFactory);
    }

    /**
     * @return variant parser factory for parsing variants for LIRICAL analysis.
     */
    public VariantParserFactory variantParserFactory() {
        return variantParserFactory;
    }

    public PhenotypeService phenotypeService() {
        return phenotypeService;
    }

    public FunctionalVariantAnnotatorService functionalVariantAnnotatorService() {
        return functionalVariantAnnotatorService;
    }

    public VariantMetadataServiceFactory variantMetadataServiceFactory() {
        return variantMetadataServiceFactory;
    }

    public LiricalAnalysisRunner analysisRunner() {
        return analysisRunner;
    }

    public AnalysisResultWriterFactory analysisResultsWriterFactory() {
        return analysisResultWriterFactory;
    }

    public Optional<String> version() {
        return options.version();
    }
}
