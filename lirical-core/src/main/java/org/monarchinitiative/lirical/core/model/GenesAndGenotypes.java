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

    static GenesAndGenotypes fromVariants(Iterable<LiricalVariant> variants) {
        // Group variants by gene id.
        Map<GeneIdentifier, List<LiricalVariant>> gene2Genotype = new HashMap<>();
        Map<GeneIdentifier, Integer> failedVariantCount = new HashMap<>();
        for (LiricalVariant variant : variants) {
            Stream<GeneIdentifier> identifiers = variant.annotations().stream()
                    .map(TranscriptAnnotation::getGeneId)
                    .distinct();
            if (variant.passedFilters())
                identifiers.forEach(geneId -> gene2Genotype.computeIfAbsent(geneId, e -> new LinkedList<>()).add(variant));
            else
                identifiers.forEach(geneId -> failedVariantCount.merge(geneId, 1, Integer::sum));
        }

        // Collect the variants into Gene2Genotype container
        List<Gene2Genotype> g2g = gene2Genotype.entrySet().stream()
                // We have 0 failed variants by default
                .map(e -> Gene2Genotype.of(e.getKey(), e.getValue(), failedVariantCount.getOrDefault(e.getKey(), 0)))
                .toList();

        if (g2g.isEmpty())
            return empty();
        else
            return new GenesAndGenotypesDefault.GenesAndGenotypesFull(g2g);
    }

    static GenesAndGenotypes of(List<Gene2Genotype> genes) {
        return genes.isEmpty()
                ? empty()
                : GenesAndGenotypesDefault.of(genes);
    }

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
