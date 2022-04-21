package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.core.analysis.AnalysisDataParser;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Gene2Genotype;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.lirical.io.VariantParserFactory;
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
import java.util.stream.Collectors;

abstract class BaseAnalysisDataParser implements AnalysisDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAnalysisDataParser.class);

    private final VariantParserFactory variantParserFactory;
    private final HpoAssociationData associationData;

    protected BaseAnalysisDataParser(VariantParserFactory variantParserFactory, HpoAssociationData associationData) {
        this.variantParserFactory = variantParserFactory; // nullable
        this.associationData = associationData; // nullable
    }

    protected GenesAndGenotypes parseGeneToGenotype(String sampleId, Path vcfPath) throws LiricalParseException {
        if (vcfPath == null) {
            return GenesAndGenotypes.empty();
        } else {
            if (variantParserFactory == null || associationData == null) {
                LOGGER.warn("Unable to parse VCF at {} since parser or association data is missing", vcfPath.toAbsolutePath());
                return GenesAndGenotypes.empty();
            } else {
                // TODO - RNR1 is an example of a gene with 2 NCBIGene IDs.
                Map<String, List<GeneIdentifier>> symbolToGeneId = associationData.geneIdentifiers().stream()
                        .collect(Collectors.groupingBy(GeneIdentifier::symbol));

                try (VariantParser variantParser = variantParserFactory.forPath(vcfPath)) {
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
                                .map(TranscriptAnnotation::getGeneSymbol)
                                .distinct()
                                .forEach(geneSymbol -> {
                                    List<GeneIdentifier> identifiers = symbolToGeneId.getOrDefault(geneSymbol, List.of());
                                    if (identifiers.isEmpty()) {
                                        LOGGER.warn("Skipping unknown gene {}", geneSymbol);
                                        return;
                                    }
                                    for (GeneIdentifier identifier : identifiers) {
                                        gene2Genotype.computeIfAbsent(identifier, e -> new LinkedList<>()).add(variant);
                                    }
                                });
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
