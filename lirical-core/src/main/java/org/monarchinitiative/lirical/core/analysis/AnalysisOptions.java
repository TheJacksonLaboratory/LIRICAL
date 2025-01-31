package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A container for analysis-specific settings, i.e. settings that need to be changed for analysis of each sample.
 */
public interface AnalysisOptions {

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
     * Limit the analysis to specific diseases.
     *
     * @return a collection of disease IDs of the diseases of interest or {@code null}
     * if <em>all</em> diseases should be tested.
     */
    Collection<TermId> targetDiseases();

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
     * Include a disease if no known or predicted deleterious variants are found in the gene associated
     * with the disease. The option is used only if the variants are available for the investigated individual.
     *
     * @return <code>true</code> if the candidate disease should be disregarded.
     */
    boolean includeDiseasesWithNoDeleteriousVariants();

    /**
     * A builder for {@link AnalysisOptions}.
     * <p>
     * The builder is <em>NOT</em> thread safe!
     */
    class Builder {

        private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);
        private GenomeBuild genomeBuild = GenomeBuild.HG38;
        private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;
        private final Set<DiseaseDatabase> diseaseDatabases = new HashSet<>(List.of(DiseaseDatabase.OMIM, DiseaseDatabase.DECIPHER));
        private Set<TermId> targetDiseases = null;  // null = test all diseases
        private float variantDeleteriousnessThreshold = .8f;
        private double defaultVariantBackgroundFrequency = .1;
        private boolean useStrictPenalties = false;
        private boolean useGlobal = false;
        private PretestDiseaseProbability pretestDiseaseProbability = null;
        private boolean includeDiseasesWithNoDeleteriousVariants = false;

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
                LOGGER.warn("Disease databases must not be `null`!");
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

        public Builder clearTargetDiseases() {
            if (this.targetDiseases != null)
                this.targetDiseases.clear();
            return this;
        }

        public Builder addTargetDiseases(TermId... diseaseIds) {
            return addTargetDiseases(Arrays.asList(diseaseIds));
        }

        public Builder addTargetDiseases(Collection<TermId> diseaseIds) {
            if (diseaseIds == null) {
                LOGGER.warn("Target disease IDs must not be `null`!");
                return this;
            }

            if (this.targetDiseases == null) this.targetDiseases = new HashSet<>();

            this.targetDiseases.addAll(diseaseIds);

            return this;
        }

        public Builder setTargetDiseases(Collection<TermId> diseaseIds) {
            if (diseaseIds == null) {
                LOGGER.warn("Target disease IDs must not be `null`!");
                return this;
            }

            if (this.targetDiseases == null) this.targetDiseases = new HashSet<>();

            this.targetDiseases.clear();
            this.targetDiseases.addAll(diseaseIds);
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

        public Builder includeDiseasesWithNoDeleteriousVariants(boolean includeDiseasesWithNoDeleteriousVariants) {
            this.includeDiseasesWithNoDeleteriousVariants = includeDiseasesWithNoDeleteriousVariants;
            return this;
        }

        public AnalysisOptions build() {
            return new AnalysisOptionsDefault(genomeBuild,
                    transcriptDatabase,
                    diseaseDatabases,
                    targetDiseases,
                    variantDeleteriousnessThreshold,
                    defaultVariantBackgroundFrequency,
                    useStrictPenalties,
                    useGlobal,
                    pretestDiseaseProbability,
                    includeDiseasesWithNoDeleteriousVariants);
        }
    }

}
