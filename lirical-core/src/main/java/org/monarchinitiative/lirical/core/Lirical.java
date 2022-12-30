package org.monarchinitiative.lirical.core;

import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.impl.LiricalAnalysisRunnerImpl;
import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.output.AnalysisResultWriterFactory;
import org.monarchinitiative.lirical.core.service.*;

import java.util.Objects;
import java.util.Optional;

public class Lirical {

    private final VariantParserFactory variantParserFactory; // nullable
    private final PhenotypeService phenotypeService;
    private final VariantMetadataServiceFactory variantMetadataServiceFactory;
    private final LiricalAnalysisRunner analysisRunner;
    private final AnalysisResultWriterFactory analysisResultWriterFactory;

    /**
     * @deprecated use {@link #of(VariantParserFactory, PhenotypeService, BackgroundVariantFrequencyServiceFactory, VariantMetadataServiceFactory, AnalysisResultWriterFactory)} }
     * instead
     */
    @Deprecated(since = "2.0.0-RC3", forRemoval = true)
    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             FunctionalVariantAnnotator functionalVariantAnnotator,
                             VariantMetadataService variantMetadataService,
                             LiricalAnalysisRunner analysisRunner,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        throw new LiricalRuntimeException("Sorry, this static constructor has been deprecated!");
    }

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                             VariantMetadataServiceFactory variantMetadataService,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        return new Lirical(variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataService,
                analysisResultWriterFactory);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                    VariantMetadataServiceFactory variantMetadataServiceFactory,
                    AnalysisResultWriterFactory analysisResultWriterFactory) {
        this.variantParserFactory = variantParserFactory; // nullable
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.variantMetadataServiceFactory = Objects.requireNonNull(variantMetadataServiceFactory);
        this.analysisRunner = LiricalAnalysisRunnerImpl.of(phenotypeService, backgroundVariantFrequencyServiceFactory);
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

    @Deprecated(since = "2.0.0-RC3", forRemoval = true)
    public FunctionalVariantAnnotator functionalVariantAnnotator() {
        return null;
    }

    /**
     *
     * @deprecated use {@link #variantMetadataServiceFactory()} instead
     */
    @Deprecated(since = "2.0.0-RC3", forRemoval = true)
    public VariantMetadataService variantMetadataService() {
        return null;
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
}
