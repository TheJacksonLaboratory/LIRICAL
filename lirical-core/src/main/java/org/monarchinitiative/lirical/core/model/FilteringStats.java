package org.monarchinitiative.lirical.core.model;

/**
 * A summary of variant input and functional annotation.
 *
 * @param nPassingVariants number of variants that passed the input filtering and were subject to LIRICAL analysis.
 * @param nFilteredVariants number of variants that failed the filtering and were <em>not</em> included in the analysis.
 * @param genesWithVariants number of genes with one or more passing variant.
 */
public record FilteringStats(long nPassingVariants,
                             long nFilteredVariants,
                             long genesWithVariants) {

    /**
     * @return the total number of variants (good quality + filtered).
     */
    public long variantCount() {
        return nPassingVariants + nFilteredVariants;
    }

}
