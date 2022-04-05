package org.monarchinitiative.lirical.model;

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

    @Override
    @Deprecated(forRemoval = true)
    default TermId id() {
        return geneId().id();
    }

    GeneIdentifier geneId();

    /**
     * @return HGVS gene symbol, e.g. <code>FBN2</code>
     */
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
                .map(var -> var.pathogenicClinVarAlleleCount(sampleId))
                .reduce(0, Integer::sum);
    }

    default int pathogenicAlleleCount(String sampleId, float pathogenicityThreshold) {
        return variants().filter(var -> var.pathogenicity() >= pathogenicityThreshold)
                .map(var -> var.alleleCount(sampleId))
                .flatMap(Optional::stream)
                .map(AlleleCount::alt)
                .map(c -> ((int) c))
                .reduce(0, Integer::sum);
    }

    default double getSumOfPathBinScores(String sampleId, float pathogenicityThreshold) {
        return variants().filter(variant -> variant.pathogenicity() >= pathogenicityThreshold)
                .mapToDouble(variant -> {
                    int altAlleleCount = variant.alleleCount(sampleId).map(AlleleCount::alt).orElse((byte) 0);
                    return altAlleleCount * variant.pathogenicity();
                })
                .reduce(0, Double::sum);
    }

}
