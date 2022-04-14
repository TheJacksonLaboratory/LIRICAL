package org.monarchinitiative.lirical.core.model;

public record FilteringStats(long nGoodQualityVariants, long nFilteredVariants) {

    public long variantCount() {
        return nGoodQualityVariants + nFilteredVariants;
    }

}
