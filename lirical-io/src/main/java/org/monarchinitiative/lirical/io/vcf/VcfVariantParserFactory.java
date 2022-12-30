package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotatorService;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.lirical.core.service.VariantMetadataServiceFactory;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class VcfVariantParserFactory implements VariantParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfVariantParserFactory.class);

    private final FunctionalVariantAnnotatorService variantAnnotatorService;
    private final VariantMetadataServiceFactory metadataServiceFactory;

    public static VcfVariantParserFactory of(FunctionalVariantAnnotatorService variantAnnotatorService,
                                             VariantMetadataServiceFactory metadataServiceFactory) {
        return new VcfVariantParserFactory(variantAnnotatorService, metadataServiceFactory);
    }

    private VcfVariantParserFactory(FunctionalVariantAnnotatorService variantAnnotatorService,
                                    VariantMetadataServiceFactory metadataServiceFactory) {
        this.variantAnnotatorService = Objects.requireNonNull(variantAnnotatorService, "Variant annotator must not be null!");
        this.metadataServiceFactory = Objects.requireNonNull(metadataServiceFactory, "Metadata service factory must not be null!");
    }

    @Override
    public Optional<VariantParser> forPath(Path path, GenomeBuild genomeBuild, TranscriptDatabase transcriptDatabase) {
        GenomicAssembly genomicAssembly = parseSvartGenomicAssembly(genomeBuild);
        Optional<FunctionalVariantAnnotator> annotator = variantAnnotatorService.getFunctionalAnnotator(genomeBuild, transcriptDatabase);
        Optional<VariantMetadataService> metadataService = metadataServiceFactory.getVariantMetadataService(genomeBuild);

        if (annotator.isEmpty())
            LOGGER.warn("Cannot configure functional variant annotator for {} {}", genomeBuild, transcriptDatabase);
        if (metadataService.isEmpty())
            LOGGER.warn("Cannot configure variant metadata service for {}", genomeBuild);

        return annotator.isPresent() && metadataService.isPresent()
                ? Optional.of(new VcfVariantParser(path, genomicAssembly, genomeBuild, annotator.get(), metadataService.get()))
                : Optional.empty();
    }

    static GenomicAssembly parseSvartGenomicAssembly(GenomeBuild genomeAssembly) {
        return switch (genomeAssembly) {
            case HG19 -> GenomicAssemblies.GRCh37p13();
            case HG38 -> GenomicAssemblies.GRCh38p13();
        };
    }
}
