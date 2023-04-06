package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Gene2Genotype extends Identified {

    static Gene2Genotype of(GeneIdentifier id, Collection<LiricalVariant> variants) {
        return Gene2GenotypeDefault.of(id, variants);
    }

    // REMOVE(v2.0.0)
    @Override
    @Deprecated(forRemoval = true)
    default TermId id() {
        return geneId().id();
    }

    GeneIdentifier geneId();

    /**
     * @return HGVS gene symbol, e.g. <code>FBN2</code>
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    default String symbol() {
        return geneId().symbol();
    }

    /**
     *
     * @return list of all variants found in this gene
     */
    Stream<LiricalVariant> variants();

    int variantCount();

    default boolean hasVariants() {
        return variantCount() != 0;
    }

    default int pathogenicClinVarCount(String sampleId) {
        return variants().filter(lv -> lv.clinvarClnSig().isPathogenicOrLikelyPathogenic())
                .mapToInt(var -> var.pathogenicClinVarAlleleCount(sampleId))
                .sum();
    }

    default int pathogenicAlleleCount(String sampleId, float pathogenicityThreshold) {
        return variants().filter(var -> var.pathogenicityScore().map(f -> f >= pathogenicityThreshold).orElse(false))
                .map(var -> var.alleleCount(sampleId))
                .flatMap(Optional::stream)
                .mapToInt(AlleleCount::alt)
                .sum();
    }

    default double getSumOfPathBinScores(String sampleId, float pathogenicityThreshold) {
        return variants().filter(variant -> variant.pathogenicityScore().orElse(0f) >= pathogenicityThreshold)
                .mapToDouble(variant -> {
                    int altAlleleCount = variant.alleleCount(sampleId).map(AlleleCount::alt).orElse((byte) 0);
                    return altAlleleCount * variant.pathogenicity();
                })
                .sum();
    }

}
