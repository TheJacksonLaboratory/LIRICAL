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

    private final VariantParserFactory variantParserFactory;
    private final PhenotypeService phenotypeService;
    private final VariantMetadataServiceFactory variantMetadataServiceFactory;
    private final LiricalAnalysisRunner analysisRunner;
    private final AnalysisResultWriterFactory analysisResultWriterFactory;
    private final LiricalOptions options;

    /**
     * @deprecated use {@link #of(VariantParserFactory, PhenotypeService, BackgroundVariantFrequencyServiceFactory, VariantMetadataServiceFactory, AnalysisResultWriterFactory, LiricalOptions)} instead.
     * instead
     */
    // REMOVE(v2.0.0)
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             FunctionalVariantAnnotator functionalVariantAnnotator,
                             VariantMetadataService variantMetadataService,
                             LiricalAnalysisRunner analysisRunner,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        throw new LiricalRuntimeException("Sorry, this static constructor has been deprecated!");
    }

    /**
     * @deprecated use {@link #of(VariantParserFactory, PhenotypeService, BackgroundVariantFrequencyServiceFactory, VariantMetadataServiceFactory, AnalysisResultWriterFactory, LiricalOptions)} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                             VariantMetadataServiceFactory variantMetadataService,
                             AnalysisResultWriterFactory analysisResultWriterFactory) {
        return of(variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataService,
                analysisResultWriterFactory,
                (String) null);
    }

    /**
     * @deprecated use {@link #of(VariantParserFactory, PhenotypeService, BackgroundVariantFrequencyServiceFactory, VariantMetadataServiceFactory, AnalysisResultWriterFactory, LiricalOptions)} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                             VariantMetadataServiceFactory variantMetadataService,
                             AnalysisResultWriterFactory analysisResultWriterFactory,
                             String version) {
        return new Lirical(variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataService,
                analysisResultWriterFactory,
                new LiricalOptions(version, 2));
    }

    public static Lirical of(VariantParserFactory variantParserFactory,
                             PhenotypeService phenotypeService,
                             BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                             VariantMetadataServiceFactory variantMetadataService,
                             AnalysisResultWriterFactory analysisResultWriterFactory,
                             LiricalOptions options) {
        return new Lirical(variantParserFactory,
                phenotypeService,
                backgroundVariantFrequencyServiceFactory,
                variantMetadataService,
                analysisResultWriterFactory,
                options);
    }

    private Lirical(VariantParserFactory variantParserFactory,
                    PhenotypeService phenotypeService,
                    BackgroundVariantFrequencyServiceFactory backgroundVariantFrequencyServiceFactory,
                    VariantMetadataServiceFactory variantMetadataServiceFactory,
                    AnalysisResultWriterFactory analysisResultWriterFactory,
                    LiricalOptions options) {
        this.variantParserFactory = Objects.requireNonNull(variantParserFactory);
        this.phenotypeService = Objects.requireNonNull(phenotypeService);
        this.variantMetadataServiceFactory = Objects.requireNonNull(variantMetadataServiceFactory);
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

    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    // REMOVE(v2.0.0)
    public FunctionalVariantAnnotator functionalVariantAnnotator() {
        return null;
    }

    /**
     *
     * @deprecated use {@link #variantMetadataServiceFactory()} instead
     */
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    // REMOVE(v2.0.0)
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

    public Optional<String> version() {
        return options.version();
    }
}
