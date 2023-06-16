package org.monarchinitiative.lirical.core.model;

import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Stream;

/**
 * {@linkplain Gene2Genotype} represents variants that have been annotated to a single gene. The gene data includes
 * the identifier of a gene, the variants annotated with respect to the gene, and convenience methods for using
 * in the {@code LIRICAL} algorithm.
 * <p>
 * Note, we only need the variants that passed the filtering for the analysis.
 */
public interface Gene2Genotype extends Identified {

    /**
     * Create {@linkplain Gene2Genotype} from a collection of variants that can include the variants
     * that failed the initial filtering.
     * <p>
     * The failing variants will not be retained.
     *
     * @deprecated the method has been deprecated and will be removed in {@code v2.0.0}.
     * Use {@link #of(GeneIdentifier, Collection, int)} instead.
     * @param id the gene credentials.
     * @param variants a collection of variants that passed/failed the initial filtering.
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC3")
    static Gene2Genotype of(GeneIdentifier id, Collection<LiricalVariant> variants) {
        int filteredOutVariantCount = 0;
        List<LiricalVariant> passingVariants = new ArrayList<>(variants.size());
        for (LiricalVariant variant : variants) {
            if (variant.passedFilters())
                passingVariants.add(variant);
            else
                filteredOutVariantCount++;
        }
        return of(id, passingVariants, filteredOutVariantCount);
    }

    /**
     * Create {@linkplain Gene2Genotype} from provided data.
     *
     * @param geneId the gene credentials.
     * @param passingVariants a collection of variants that passed the initial filtering.
     * @param filteredOutVariantCount the number of variants that failed the initial filtering.
     */
    static Gene2Genotype of(GeneIdentifier geneId,
                            Collection<LiricalVariant> passingVariants,
                            int filteredOutVariantCount) {
        Objects.requireNonNull(geneId, "Gene ID must not be null");
        Objects.requireNonNull(passingVariants, "Variants must not be null");
        if (passingVariants.isEmpty()) {
            return new Gene2GenotypeDefault.Gene2GenotypeNoVariants(geneId, filteredOutVariantCount);
        } else {
            return new Gene2GenotypeDefault.Gene2GenotypeFull(geneId, passingVariants, filteredOutVariantCount);
        }
    }

    // REMOVE(v2.0.0)
    @Override
    @Deprecated(forRemoval = true)
    default TermId id() {
        return geneId().id();
    }

    /**
     * Get the credentials of the gene.
     */
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
     * Get a {@linkplain Stream} of variants annotated to this gene.
     *
     * @return a stream of variants found in this gene.
     */
    Stream<LiricalVariant> variants();

    /**
     * Get the count of variants annotated to this gene that passed the filtering.
     */
    int variantCount();

    /**
     * @return {@code true} if the gene is annotated with 1 or more variants that passed the filtering.
     */
    default boolean hasVariants() {
        return variantCount() != 0;
    }

    /**
     * Get the count of variants annotated to this gene which failed the filtering.
     */
    default int filteredOutVariantCount() {
        // This can explode if the number of variants overflows int.
        // However, this is super unlikely to happen in practice.
        return Math.toIntExact(variants().filter(LiricalVariant::failedFilters).count());
    }

    /**
     * Get the number of predicted pathogenic/deleterious alleles in the gene for the {@code sampleId}.
     * <p>
     * Note, only the variant that passed the filtering are considered.
     */
    default int pathogenicClinVarCount(String sampleId) {
        return variants()
                .filter(lv -> lv.clinVarAlleleData()
                        .map(cv -> cv.getClinvarClnSig().isPathogenicOrLikelyPathogenic())
                        .orElse(false))
                .mapToInt(var -> var.pathogenicClinVarAlleleCount(sampleId))
                .sum();
    }

    /**
     * @deprecated the method was deprecated and will be removed in <code>v3.0.0</code>.
     * Use {@link #deleteriousAlleleCount(String, float)} instead.
     * @see #deleteriousAlleleCount(String, float)
     */
    @Deprecated(forRemoval = true, since = "2.0.0-RC3")
    default int pathogenicAlleleCount(String sampleId, float pathogenicityThreshold) {
        // REMOVE(v3.0.0)
        return deleteriousAlleleCount(sampleId, pathogenicityThreshold);
    }

    /**
     * Get the count of alleles of predicted pathogenic/deleterious variants in the gene for the {@code sampleId}.
     * The variants that are both <em>not</em> labeled as benign or likely benign by ClinVar and have the
     * {@link LiricalVariant#pathogenicityScore()} at or above the provided {@code pathogenicityThreshold}
     * are deemed to be predicted pathogenic/deleterious.
     * <p>
     * Note, we take specific precautions to not clash with ClinVar variant interpretation and consider ClinVar benign
     * or likely benign variants as deleterious.
     */
    default int deleteriousAlleleCount(String sampleId, float pathogenicityThreshold) {
        // The first part of the filter clause ensures we do not clash with ClinVar variant interpretation.
        // In other words, a ClinVar benign or likely benign variant CANNOT be interpreted as deleterious
        // based on in silico pathogenicity scores.
        return variants()
                .filter(var -> var.clinVarAlleleData()
                        .map(cv -> cv.getClinvarClnSig().notBenignOrLikelyBenign())
                        .orElse(true)
                        && var.pathogenicityScore().map(f -> f >= pathogenicityThreshold).orElse(false))
                .map(var -> var.alleleCount(sampleId))
                .flatMap(Optional::stream)
                .mapToInt(AlleleCount::alt)
                .sum();
    }

    default double getSumOfPathBinScores(String sampleId, float pathogenicityThreshold) {
        // Same as in `pathogenicAlleleCount(..)` above, the first part of the filter clause ensures
        // we do not clash with ClinVar variant interpretation.
        return variants()
                .filter(variant -> variant.clinVarAlleleData()
                        .map(cv -> cv.getClinvarClnSig().notBenignOrLikelyBenign())
                        .orElse(true)
                        && variant.pathogenicityScore().orElse(0f) >= pathogenicityThreshold)
                .mapToDouble(variant -> {
                    int altAlleleCount = variant.alleleCount(sampleId).map(AlleleCount::alt).orElse((byte) 0);
                    return altAlleleCount * variant.pathogenicity();
                })
                .sum();
    }

}
