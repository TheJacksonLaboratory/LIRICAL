package org.monarchinitiative.lirical.core.model;

import java.util.List;
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
