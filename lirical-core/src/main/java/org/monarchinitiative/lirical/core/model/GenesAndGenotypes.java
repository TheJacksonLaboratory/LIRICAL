package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Container for {@link Gene2Genotype}s present in the analysis.
 */
public interface GenesAndGenotypes extends Iterable<Gene2Genotype> {

    static GenesAndGenotypes empty() {
        return GenesAndGenotypesDefault.empty();
    }

    static GenesAndGenotypes of(Collection<String> sampleNames, Collection<Gene2Genotype> genes) {
        return genes.isEmpty()
                ? empty()
                : GenesAndGenotypesDefault.of(sampleNames, genes);
    }

    static GenesAndGenotypes fromVariants(Collection<String> sampleNames, Iterable<LiricalVariant> variants) {
        List<Gene2Genotype> g2g = groupVariantsByGeneId(variants);
        return of(sampleNames, g2g);
    }

    private static List<Gene2Genotype> groupVariantsByGeneId(Iterable<LiricalVariant> variants) {
        // Group variants by gene id.
        Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
        Map<GeneIdentifier, Integer> failedVariantCount = new HashMap<>();
        for (LiricalVariant variant : variants) {
            Stream<GeneIdentifier> identifiers = variant.annotations().stream()
                    .map(TranscriptAnnotation::getGeneId)
                    .distinct();
            if (variant.passedFilters())
                identifiers.forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new ArrayList<>()).add(variant));
            else
                identifiers.forEach(geneId -> failedVariantCount.merge(geneId, 1, Integer::sum));
        }

        // Collect the variants into Gene2Genotype container
        return gene2Genotype.entrySet().stream()
                // We have 0 failed variants by default
                .map(e -> Gene2Genotype.of(e.getKey(), e.getValue(), failedVariantCount.getOrDefault(e.getKey(), 0)))
                .toList();
    }

    /**
     * @return a collection with sample identifiers for whom we have the genotype data.
     */
    Collection<String> sampleNames();

    /**
     * @return number of genes in the container.
     */
    int size();

    default Stream<Gene2Genotype> genes() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * @return variant filtering statistics.
     */
    default FilteringStats computeFilteringStats() {
        AtomicLong passed = new AtomicLong();
        AtomicLong failed = new AtomicLong();
        AtomicLong genesWithVariants = new AtomicLong();
        genes().forEach(g -> {
            if (g.hasVariants())
                genesWithVariants.incrementAndGet();
            passed.addAndGet(g.variantCount());
            failed.addAndGet(g.filteredOutVariantCount());
        });

        return new FilteringStats(passed.get(), failed.get(), genesWithVariants.get());
    }

}
