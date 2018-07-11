package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test on
 * an {@link HpoCase}.
 * TODO interface currently offers too many things. Probably restrict to one output once integrated into Exomiser
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CaseEvaluator {
    private static final Logger logger = LogManager.getLogger();
    /** List of abnormalities seen in the person being evaluated. */
    private final List<TermId> phenotypicAbnormalities;
    /** Map of the observed genotypes in the VCF file. Key is an EntrezGene is, and the value is the average pathogenicity score times the
     * count of all variants in the pathogenic bin.*/
    private final Map<TermId,Double> genotypeMap;
    /** key: a disease CURIE, e.g., OMIM:600100; value-corresponding disease object.*/
    private final Map<TermId,HpoDisease> diseaseMap;
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private final Multimap<TermId,TermId> disease2geneMultimap;
    /** Probability of diseases before testing (e.g., prevalence or 1/N).*/
    private final Map<TermId,Double> pretestProbabilityMap;
    /** Object used to calculate phenotype likelihood ratios. */
    private final PhenotypeLikelihoodRatio phenotypeLRevaluator;
    /** Object used to calculate genotype-based likelihood ratio. */
    private final GenotypeLikelihoodRatio genotypeLrEvalutator;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;

    private final boolean phenotypeOnly;

    /**
     * This constructor is used for phenotype-only cases.
     * @param hpoTerms List of phenotypic abnormalityes observed in the patient
     * @param ontology Reference to HPO ontology
     * @param diseaseMap key: disease CURIE, e.h., OMIM:600100; value: HpoDisease object
     * @param phenotypeLrEvaluator class to evaluate phenotype likelihood ratios.
     */
    private CaseEvaluator(List<TermId> hpoTerms,
                          HpoOntology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          PhenotypeLikelihoodRatio phenotypeLrEvaluator) {
        this.phenotypicAbnormalities=hpoTerms;
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.phenotypeLRevaluator=phenotypeLrEvaluator;
        this.genotypeMap=ImmutableMap.of();
        this.disease2geneMultimap=ImmutableMultimap.of();
        this.genotypeLrEvalutator=null;
        // For now, assume equal pretest probabilities
        this.pretestProbabilityMap =new HashMap<>();
        int n=diseaseMap.size();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.phenotypeOnly=true;
    }




    private CaseEvaluator(List<TermId> hpoTerms,
                          HpoOntology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          Multimap<TermId,TermId> disease2geneMultimap,
                          PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                          GenotypeLikelihoodRatio genotypeLrEvalutator,
                          Map<TermId,Double> genotypeMap) {
        phenotypicAbnormalities=hpoTerms;


        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.phenotypeLRevaluator =phenotypeLrEvaluator;
        this.genotypeLrEvalutator=genotypeLrEvalutator;
        this.ontology=ontology;

        // For now, assume equal pretest probabilities
        this.pretestProbabilityMap =new HashMap<>();
        int n=diseaseMap.size();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.genotypeMap=genotypeMap;
        this.phenotypeOnly=false;
    }



    /** This method evaluates the likilihood ratio for each disease in
     * {@link #diseaseMap}. After this, it sorts the results (the best hit is then at index 0, etc).
     */
    public HpoCase evaluate()  {
        assert diseaseMap.size()== pretestProbabilityMap.size();
        ImmutableMap.Builder<TermId,TestResult> mapbuilder = new ImmutableMap.Builder<>();
        for (TermId diseaseId : diseaseMap.keySet()) {
            HpoDisease disease = this.diseaseMap.get(diseaseId);
            double pretest = pretestProbabilityMap.get(diseaseId);
            // 1. get phenotype LR
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.phenotypicAbnormalities) {
                double LR = phenotypeLRevaluator.getLikelihoodRatio(tid, diseaseId);
                builder.add(LR);
            }
            // 2. get genotype LR if available
            Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
            Double LR = null;
            TermId geneId = null;
            if (associatedGenes != null && associatedGenes.size() > 0) {
                for (TermId entrezGeneId : associatedGenes) {
                    double observedWeightedPathogenicVariantCount = this.genotypeMap.getOrDefault(entrezGeneId, 0.0);
                    List<TermId> inheritancemodes = disease.getModesOfInheritance();
                    Optional<Double> opt = this.genotypeLrEvalutator.evaluateGenotype(observedWeightedPathogenicVariantCount,
                            inheritancemodes,
                            entrezGeneId);
                    if (opt.isPresent()) {
                        if (LR == null) {
                            LR = opt.get();
                            geneId = entrezGeneId;
                        } else if (LR > opt.get()) {
                            LR = opt.get();
                            geneId = entrezGeneId;
                        }
                    }
                }
                TestResult result;
                if (LR != null) {
                    result = new TestResult(builder.build(), diseaseId, LR, geneId, pretest);
                } else {
                    result = new TestResult(builder.build(), diseaseId, pretest);
                }
                mapbuilder.put(diseaseId, result);
            }
        }
        Map<TermId,TestResult> results = evaluateRanks(mapbuilder.build());
        HpoCase.Builder casebuilder = new HpoCase.Builder(phenotypicAbnormalities)
                .results(results);
        return casebuilder.build();
    }



    private Map<TermId,TestResult> evaluateRanks(Map<TermId,TestResult> resultMap) {
        List<TestResult> results = new ArrayList<>(resultMap.values());
        results.sort(Collections.reverseOrder());
        int rank=0;
        for (TestResult res : results) {
            rank++;
            res.setRank(rank);
            if (rank<11) {
                TermId diseaseCurie = res.getDiseaseCurie();
                String name = diseaseMap.get(diseaseCurie).getName();
                System.err.println(String.format("Rank #%d: %s [%s]",rank,name,diseaseCurie.getIdWithPrefix()));
            }
        }
        return resultMap;
    }







    public HpoDisease id2disease(TermId diseaseCurie) {
         return this.diseaseMap.get(diseaseCurie);
    }

    public String i2diseaseName(TermId diseaseCurie) {
        return this.diseaseMap.get(diseaseCurie).getName();
    }




    /**
     * Convenience class for building a {@link CaseEvaluator} object--mainly to avoid having
     * a constructor with an extremely long list of arguments.
     */
    public static class Builder {
        /** The abnormalities observed in the individual being investigated. */
        private final List<TermId> hpoTerms;
        private HpoOntology ontology;
        /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
        private Map<TermId,HpoDisease> diseaseMap;
        /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
        private Multimap<TermId,TermId> disease2geneMultimap;
        /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
        private PhenotypeLikelihoodRatio phenotypeLR;

        private GenotypeLikelihoodRatio genotypeLR;

        private Map<TermId,Double> genotypeMap;

        public Builder(List<TermId> hpoTerms){ this.hpoTerms=hpoTerms; }

        public Builder ontology(HpoOntology hont) { this.ontology=hont; return this;}

        public Builder diseaseMap(Map<TermId,HpoDisease> dmap) { this.diseaseMap=dmap; return this;}

        public Builder disease2geneMultimap(Multimap<TermId,TermId> d2gmmap) { this.disease2geneMultimap=d2gmmap; return this;}

        public Builder genotypeMap(Map<TermId,Double> gtmap) { this.genotypeMap=gtmap; return this;}

        public Builder phenotypeLr(PhenotypeLikelihoodRatio phenoLr) { this.phenotypeLR=phenoLr; return this; }

        public Builder genotypeLr(GenotypeLikelihoodRatio glr) { this.genotypeLR=glr; return this; }


        public CaseEvaluator build() {
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(diseaseMap);
            Objects.requireNonNull(disease2geneMultimap);
            return new CaseEvaluator(hpoTerms,ontology,diseaseMap,disease2geneMultimap,phenotypeLR,genotypeLR,genotypeMap);
        }


        public CaseEvaluator buildPhenotypeOnlyEvaluator() {
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(phenotypeLR);
            return new CaseEvaluator(hpoTerms,ontology,diseaseMap,phenotypeLR);
        }



    }
    

}
