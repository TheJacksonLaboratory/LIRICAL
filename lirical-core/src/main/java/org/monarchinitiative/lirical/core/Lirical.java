package org.monarchinitiative.lirical.core;

import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.util.Objects;
import java.util.Optional;

public class Lirical {

    private final VariantParserFactory variantParserFactory;
    private final PhenotypeService phenotypeService;
    private final FunctionalVariantAnnotator functionalVariantAnnotator;
    private final VariantMetadataService variantMetadataService;
    private final LiricalAnalysisRunner analysisRunner;
    private final AnalysisResultWriterFactory analysisResultWriterFactory;

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             FunctionalVariantAnnotator functionalVariantAnnotator,
                             VariantMetadataService variantMetadataService,
                             LiricalAnalysisRunner analysisRunner,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        return new Lirical(variantParserFactory,
                phenotypeService,
                functionalVariantAnnotator,
                variantMetadataService,
                analysisRunner,
                analysisResultWriterFactory);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    FunctionalVariantAnnotator functionalVariantAnnotator,
                    VariantMetadataService variantMetadataService,
                    LiricalAnalysisRunner analysisRunner,
                    AnalysisResultWriterFactory analysisResultWriterFactory) {
        this.variantParserFactory = variantParserFactory; // nullable
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.functionalVariantAnnotator = Objects.requireNonNull(functionalVariantAnnotator);
        this.variantMetadataService = Objects.requireNonNull(variantMetadataService);
        this.analysisRunner = Objects.requireNonNull(analysisRunner);
        this.analysisResultWriterFactory = Objects.requireNonNull(analysisResultWriterFactory);
    }

    /**
     * @return variant parser factory if Exomiser variant database is present. Otherwise, an empty optional is returned.
     */
    public Optional<VariantParserFactory> variantParserFactory() {
        return Optional.ofNullable(variantParserFactory);
    }

    public PhenotypeService phenotypeService() {
        return phenotypeService;
    }

    public FunctionalVariantAnnotator functionalVariantAnnotator() {
        return functionalVariantAnnotator;
    }

    public VariantMetadataService variantMetadataService() {
        return variantMetadataService;
    }

    public LiricalAnalysisRunner analysisRunner() {
        return analysisRunner;
    }

    public AnalysisResultWriterFactory analysisResultsWriterFactory() {
        return analysisResultWriterFactory;
    }
}
