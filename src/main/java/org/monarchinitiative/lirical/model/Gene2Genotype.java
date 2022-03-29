package org.monarchinitiative.lirical.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.Identified;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public interface Gene2Genotype extends Identified {

    static Gene2Genotype of(GeneIdentifier id, Collection<LiricalVariant> variants) {
        return Gene2GenotypeImpl.of(id, variants);
    }

    /**
     * @return HGVS gene symbol, e.g. <code>FBN2</code>
     */
    String symbol();

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
        return variants().filter(LiricalVariant::isClinVarPathogenic)
                .map(var -> var.pathogenicClinVarAlleleCount(sampleId))
                .reduce(0, Integer::sum);
    }

    default int pathogenicAlleleCount(String sampleId, float pathogenicityThreshold) {
        return variants().filter(var -> var.pathogenicityScore() >= pathogenicityThreshold)
                .map(var -> var.alleleCount(sampleId))
                .flatMap(Optional::stream)
                .map(AlleleCount::alt)
                .map(c -> ((int) c))
                .reduce(0, Integer::sum);
    }

    default double getSumOfPathBinScores(String sampleId, float pathogenicityThreshold) {
        return variants().filter(variant -> variant.pathogenicityScore() >= pathogenicityThreshold)
                .mapToDouble(variant -> {
                    int altAlleleCount = variant.alleleCount(sampleId).map(AlleleCount::alt).orElse((byte) 0);
                    return altAlleleCount * variant.pathogenicityScore();
                })
                .reduce(0, Double::sum);
    }

}
