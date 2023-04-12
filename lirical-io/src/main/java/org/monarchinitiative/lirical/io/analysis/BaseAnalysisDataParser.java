package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisDataParser;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.*;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

abstract class BaseAnalysisDataParser implements AnalysisDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAnalysisDataParser.class);

    private final VariantParserFactory variantParserFactory;
    private final HpoAssociationData associationData;

    protected BaseAnalysisDataParser(VariantParserFactory variantParserFactory, HpoAssociationData associationData) {
        this.variantParserFactory = variantParserFactory; // nullable
        this.associationData = associationData; // nullable
    }

    protected GenesAndGenotypes parseGeneToGenotype(String sampleId,
                                                    Path vcfPath,
                                                    GenomeBuild genomeBuild,
                                                    TranscriptDatabase transcriptDatabase) throws LiricalParseException {
        if (vcfPath == null) {
            LOGGER.info("VCF path is not set. Performing phenotype-only analysis");
            return GenesAndGenotypes.empty();
        } else {
            if (variantParserFactory == null || associationData == null) {
                LOGGER.warn("Unable to parse VCF at {} since parser or association data is missing. Falling back to phenotype-only analysis", vcfPath.toAbsolutePath());
                return GenesAndGenotypes.empty();
            } else {
                LOGGER.debug("Getting variant parser to parse a VCF file using {} assembly and {} transcripts", genomeBuild, transcriptDatabase);
                Optional<VariantParser> vp = variantParserFactory.forPath(vcfPath, genomeBuild, transcriptDatabase);
                if (vp.isEmpty()) {
                    LOGGER.warn("Cannot obtain parser for processing the VCF file {} with {} {} due to missing resources. Falling back to phenotype-only analysis",
                            vcfPath.toAbsolutePath(), genomeBuild, transcriptDatabase);
                    return GenesAndGenotypes.empty();
                }

                try (VariantParser variantParser = vp.get()) {
                    // Ensure the VCF file contains the sample
                    if (!variantParser.sampleNames().contains(sampleId))
                        throw new LiricalParseException("The sample " + sampleId + " is not present in VCF at '" + vcfPath.toAbsolutePath() + '\'');
                    LOGGER.debug("Found sample {} in the VCF file at {}", sampleId, vcfPath.toAbsolutePath());

                    // Read variants
                    LOGGER.info("Reading variants from {}", vcfPath.toAbsolutePath());
                    AtomicInteger counter = new AtomicInteger();
                    List<LiricalVariant> variants = variantParser.variantStream()
                            .peek(logProgress(counter))
                            .toList();
                    LOGGER.info("Read {} variants", variants.size());

                    // Group variants by gene symbol. It would be better to group the variants by e.g. Entrez ID,
                    // but the ID is not available from TranscriptAnnotation
                    Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
                    for (LiricalVariant variant : variants) {
                        variant.annotations().stream()
                                .map(TranscriptAnnotation::getGeneId)
                                .distinct()
                                .forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new LinkedList<>()).add(variant));
                    }

                    // Collect the variants into Gene2Genotype container
                    List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                            .map(e -> Gene2Genotype.of(e.getKey(), e.getValue()))
                            .toList();

                    return GenesAndGenotypes.of(g2g);
                } catch (Exception e) {
                    throw new LiricalParseException(e);
                }
            }
        }
    }

    private static Consumer<LiricalVariant> logProgress(AtomicInteger counter) {
        return v -> {
            int current = counter.incrementAndGet();
            if (current % 5000 == 0)
                LOGGER.info("Read {} variants", current);
        };
    }

    protected static Age parseAge(String age) {
        if (age == null) {
            LOGGER.debug("The age was not provided");
            return Age.ageNotKnown();
        }
        try {
            Period period = Period.parse(age);
            LOGGER.info("Using age {}", period);
            return Age.parse(period);
        } catch (DateTimeParseException e) {
            LOGGER.warn("Unable to parse age '{}': {}", age, e.getMessage());
            return Age.ageNotKnown();
        }
    }
}
