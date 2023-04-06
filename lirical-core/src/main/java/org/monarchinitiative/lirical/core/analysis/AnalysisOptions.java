package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A container for analysis-specific settings, i.e. settings that need to be changed for analysis of each sample.
 */
public interface AnalysisOptions {

    /**
     * @deprecated to be removed in <code>2.0.0</code>, use {@link #builder()} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    static AnalysisOptions of(boolean useGlobal, PretestDiseaseProbability pretestDiseaseProbability) {
        return of(useGlobal, pretestDiseaseProbability, false);
    }

    /**
     * @deprecated to be removed in <code>2.0.0</code>, use {@link #builder()} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    static AnalysisOptions of(boolean useGlobal,
                              PretestDiseaseProbability pretestDiseaseProbability,
                              boolean disregardDiseaseWithNoDeleteriousVariants) {
        Objects.requireNonNull(pretestDiseaseProbability);
        return of(useGlobal, pretestDiseaseProbability, disregardDiseaseWithNoDeleteriousVariants, .8f);
    }

    /**
     * @deprecated to be removed in <code>2.0.0</code>, use the {@link #builder()} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    static AnalysisOptions of(boolean useGlobal,
                              PretestDiseaseProbability pretestDiseaseProbability,
                              boolean disregardDiseaseWithNoDeleteriousVariants,
                              float pathogenicityThreshold) {
        Objects.requireNonNull(pretestDiseaseProbability);
        Objects.requireNonNull(pretestDiseaseProbability);
        return new AnalysisOptionsDefault(GenomeBuild.HG38,
                TranscriptDatabase.REFSEQ,
                Set.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER),
                pathogenicityThreshold,
                .1,
                false,
                useGlobal,
                pretestDiseaseProbability,
                disregardDiseaseWithNoDeleteriousVariants);
    }

    static Builder builder() {
        return new Builder();
    }

    /**
     * @return genomic build that should be used in this analysis.
     */
    GenomeBuild genomeBuild();

    /**
     * @return the transcript database that should be used in this analysis.
     */
    TranscriptDatabase transcriptDatabase();

    /**
     * @return evaluate the patient wrt. diseases from given source(s).
     */
    Set<DiseaseDatabase> diseaseDatabases();

    /**
     * @return a variant frequency to assume for the variants with no available frequency data.
     * @deprecated the parameter has been deprecated in favor of a constant in {@link VariantMetadataService#DEFAULT_FREQUENCY}.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true, since = "2.0.0-RC2")
    default float defaultVariantAlleleFrequency() {
        return Float.NaN;
    }

    /**
     * @return threshold for determining if the variant is deleterious or not.
     * The threshold range must be in range of <code>[0,1]</code>.
     */
    float variantDeleteriousnessThreshold();

    /**
     * @return default frequency of called-pathogenic variants in the general population o use if for whatever reason,
     * data was not available in gnomAD.
     */
    double defaultVariantBackgroundFrequency();

    /**
     * @return <code>true</code> if strict penalties should be used if the genotype does not match the disease model
     * in terms of number of called pathogenic alleles.
     */
    boolean useStrictPenalties();

    /**
     * @return <code>true</code> if the <em>global</em> analysis mode should be used.
     */
    boolean useGlobal();

    /**
     * @return pretest disease probability container.
     */
    PretestDiseaseProbability pretestDiseaseProbability();

    /**
     * Disregard a disease if no known or predicted deleterious variants are found in the gene associated
     * with the disease. The option is used only if the variants are available for the investigated individual.
     *
     * @return <code>true</code> if the candidate disease should be disregarded.
     */
    boolean disregardDiseaseWithNoDeleteriousVariants();

    /**
     * Variant with pathogenicity value greater or equal to this threshold is considered deleterious.
     *
     * @return variant pathogenicity threshold value.
     * @deprecated use {@link #variantDeleteriousnessThreshold()} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(since = "2.0.0-RC2", forRemoval = true)
    default float pathogenicityThreshold() {
        return variantDeleteriousnessThreshold();
    }

    /**
     * A builder for {@link AnalysisOptions}.
     */
    class Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

        private GenomeBuild genomeBuild = GenomeBuild.HG38;
        private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;
        private final Set<DiseaseDatabase> diseaseDatabases = new HashSet<>(List.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER));
        private float variantDeleteriousnessThreshold = .8f;
        private double defaultVariantBackgroundFrequency = .1;
        private boolean useStrictPenalties = false;
        private boolean useGlobal = false;
        private PretestDiseaseProbability pretestDiseaseProbability = null;
        private boolean disregardDiseaseWithNoDeleteriousVariants = true;

        private Builder() {
        }

        public Builder genomeBuild(GenomeBuild genomeBuild) {
            if (genomeBuild == null) {
                LOGGER.warn("Cannot set genome build to `null`. Retaining {}", this.genomeBuild);
                return this;
            }
            this.genomeBuild = genomeBuild;
            return this;
        }

        public Builder transcriptDatabase(TranscriptDatabase transcriptDatabase) {
            if (transcriptDatabase == null) {
                LOGGER.warn("Cannot set transcript database to `null`. Retaining {}", this.transcriptDatabase);
                return this;
            }
            this.transcriptDatabase = transcriptDatabase;
            return this;
        }

        public Builder clearDiseaseDatabases() {
            this.diseaseDatabases.clear();
            return this;
        }

        public Builder addDiseaseDatabases(DiseaseDatabase... diseaseDatabases) {
            return addDiseaseDatabases(Arrays.asList(diseaseDatabases));
        }

        public Builder addDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
            if (diseaseDatabases == null) {
                LOGGER.warn("Disease databases should not be `null`!");
                return this;
            }
            this.diseaseDatabases.addAll(diseaseDatabases);
            return this;
        }

        public Builder setDiseaseDatabases(Collection<DiseaseDatabase> diseaseDatabases) {
            if (diseaseDatabases == null) {
                LOGGER.warn("Disease databases must not be `null`!");
                return this;
            }
            this.diseaseDatabases.clear();
            this.diseaseDatabases.addAll(diseaseDatabases);
            return this;
        }

        public Builder variantDeleteriousnessThreshold(float variantDeleteriousnessThreshold) {
            this.variantDeleteriousnessThreshold = variantDeleteriousnessThreshold;
            return this;
        }

        public Builder defaultVariantBackgroundFrequency(double defaultVariantBackgroundFrequency) {
            this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
            return this;
        }

        public Builder useStrictPenalties(boolean useStrictPenalties) {
            this.useStrictPenalties = useStrictPenalties;
            return this;
        }

        public Builder useGlobal(boolean useGlobal) {
            this.useGlobal = useGlobal;
            return this;
        }

        public Builder pretestProbability(PretestDiseaseProbability pretestDiseaseProbability) {
            this.pretestDiseaseProbability = pretestDiseaseProbability;
            return this;
        }


        public Builder disregardDiseaseWithNoDeleteriousVariants(boolean disregardDiseaseWithNoDeleteriousVariants) {
            this.disregardDiseaseWithNoDeleteriousVariants = disregardDiseaseWithNoDeleteriousVariants;
            return this;
        }

        public AnalysisOptions build() {
            return new AnalysisOptionsDefault(genomeBuild,
                    transcriptDatabase,
                    diseaseDatabases,
                    variantDeleteriousnessThreshold,
                    defaultVariantBackgroundFrequency,
                    useStrictPenalties,
                    useGlobal,
                    pretestDiseaseProbability,
                    disregardDiseaseWithNoDeleteriousVariants);
        }
    }

}
