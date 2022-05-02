package org.monarchinitiative.lirical.core.model;

import java.util.List;

public interface LiricalVariant extends GenotypedVariant, VariantMetadata, FunctionalAnnotationAware {

    static LiricalVariant of(GenotypedVariant variant, List<TranscriptAnnotation> annotations, VariantMetadata metadata) {
        return new LiricalVariantDefault(variant, annotations, metadata);
    }

    /**
     * Count the number of ClinVar-pathogenic alleles. If this variant is not called Pathogenic in ClinVar, then
     * the count is always zero. If the variant is ClinVar-pathogenic, then the count depends on variant genotype.
     * Homozygous is 2, heterozygous is 1, homozygous reference and not observed are 0.
     *
     * @return number of pathogenic alleles that are registered in ClinVar
     */
    default int pathogenicClinVarAlleleCount(String sampleId) {
        if (!clinvarClnSig().isPathogenicOrLikelyPathogenic()) {
            return 0;
        } else {
            return alleleCount(sampleId)
                    .map(AlleleCount::alt)
                    .orElse((byte) 0);
        }
    }

}
