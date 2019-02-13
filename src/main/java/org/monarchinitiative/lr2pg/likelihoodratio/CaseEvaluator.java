package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.exception.Lr2PgRuntimeException;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test
 *  and returns one {@link HpoCase} object with the results by the method {@link #evaluate()}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CaseEvaluator {
    private static final Logger logger = LogManager.getLogger();
    /** List of abnormalities seen in the person being evaluated. */
    private final List<TermId> phenotypicAbnormalities;
    /** List of abnormalities excluded in the person being evaluated. */
    private final List<TermId> negatedPhenotypicAbnormalities;
    /** Map of the observed genotypes in the VCF file. Key is an EntrezGene is, and the value is the average pathogenicity score times the
     * count of all variants in the pathogenic bin.*/
    private final Map<TermId,Gene2Genotype> genotypeMap;
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
    private final Ontology ontology;
    /** retain candidates even if no candidate variant is found */
    private boolean keepIfNoCandidateVariant;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    private Map<TermId,String> geneId2symbol;

    private static final double DEFAULT_POSTERIOR_PROBABILITY_THRESHOLD=0.01;

    private final double threshold;
    /** If true, then genotype information is available for the analysis. Otherwise, skip it. */
    private final boolean useGenotypeAnalysis;

    private boolean verbose=true;

    /**
     * This constructor is used for phenotype-only cases.
     * @param hpoTerms List of phenotypic abnormalityes observed in the patient
     * @param ontology Reference to HPO ontology
     * @param diseaseMap key: disease CURIE, e.h., OMIM:600100; value: HpoDisease object
     * @param phenotypeLrEvaluator class to evaluate phenotype likelihood ratios.
     */
    private CaseEvaluator(List<TermId> hpoTerms,
                          List<TermId> negatedHpoTerms,
                          Ontology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          PhenotypeLikelihoodRatio phenotypeLrEvaluator) {
        this.phenotypicAbnormalities=hpoTerms;
        this.negatedPhenotypicAbnormalities=negatedHpoTerms;
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
        this.useGenotypeAnalysis =false;
        this.threshold=DEFAULT_POSTERIOR_PROBABILITY_THRESHOLD;
        this.keepIfNoCandidateVariant=true; // needs to be true for phenotype-only analysis!
    }


    /**
     * Constructor for LR2PG anaysis with a VCF file.
     * @param hpoTerms list of observed abnormalities
     * @param negatedHpoTerms list of excluded abnormalities
     * @param ontology reference to HPO ontology
     * @param diseaseMap map to HPO disease objects
     * @param disease2geneMultimap map from disease id to the corresponding gene symbols
     * @param phenotypeLrEvaluator reference to object that evaluates the phenotype LR
     * @param genotypeLrEvalutator reference to object that evaluates the genotype LR
     * @param genotypeMap Map of gene symbol to genotype evaluations
     * @param thres threshold posterior probability
     * @param keep if true, do not discard candidates if they do not have a candidate variant
     */
    private CaseEvaluator(List<TermId> hpoTerms,
                          List<TermId> negatedHpoTerms,
                          Ontology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          Multimap<TermId,TermId> disease2geneMultimap,
                          PhenotypeLikelihoodRatio phenotypeLrEvaluator,
                          GenotypeLikelihoodRatio genotypeLrEvalutator,
                          Map<TermId,Gene2Genotype> genotypeMap,
                          double thres,
                          boolean keep,
                          Map<TermId,String> geneId2symbol) {
        this.phenotypicAbnormalities=hpoTerms;
        this.negatedPhenotypicAbnormalities=negatedHpoTerms;
        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.phenotypeLRevaluator =phenotypeLrEvaluator;
        this.genotypeLrEvalutator=genotypeLrEvalutator;
        this.ontology=ontology;
        this.threshold=thres;
        this.keepIfNoCandidateVariant=keep;
        this.geneId2symbol=geneId2symbol;

        // For now, assume equal pretest probabilities
        this.pretestProbabilityMap =new HashMap<>();
        int n=diseaseMap.size();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.genotypeMap=genotypeMap;
        this.useGenotypeAnalysis =true;
    }

    public void setVerbosity(boolean v) { this.verbose=v;}


    private TestResult getResultNoDiseaseGene(HpoDisease disease,double pretest, List<Double> observed, List<Double> excluded) {
        return new TestResult(observed,excluded,disease,pretest);
    }

    private TestResult getResultDiseaseGeneNoVariantsFound(HpoDisease disease,double pretest, List<Double> observed, List<Double> excluded) {
        return new TestResult(observed,excluded,disease,pretest);
    }


    private List<Double> observedPhenotypesLikelihoodRatios(TermId diseaseId) {
        ImmutableList.Builder<Double> builderObserved = new ImmutableList.Builder<>();
        for (TermId tid : this.phenotypicAbnormalities) {
            double LR = phenotypeLRevaluator.getLikelihoodRatio(tid, diseaseId);
            builderObserved.add(LR);
        }
        return builderObserved.build();
    }

    private List<Double> excludedPhenotypesLikelihoodRatios(TermId diseaseId) {
        ImmutableList.Builder<Double> builderExcluded = new ImmutableList.Builder<>();
        for (TermId negated : this.negatedPhenotypicAbnormalities) {
            double LR = phenotypeLRevaluator.getLikelihoodRatioForExcludedTerm(negated, diseaseId);
            builderExcluded.add(LR);
        }
        return builderExcluded.build();
    }


    /**
     * Perform the evaluation of the current case based only on phenotype evidence
     * @return map with key=disease idea and value=corresponding {@link TestResult}
     */
    private  Map<TermId,TestResult>  phenotypeOnlyEvaluation() {
        ImmutableMap.Builder<TermId,TestResult> mapbuilder = new ImmutableMap.Builder<>();
        for (TermId diseaseId : diseaseMap.keySet()) {
            HpoDisease disease = this.diseaseMap.get(diseaseId);
            double pretest = pretestProbabilityMap.get(diseaseId);
            List<Double> observedLR = observedPhenotypesLikelihoodRatios(diseaseId);
            List<Double> excludedLR = excludedPhenotypesLikelihoodRatios(diseaseId);
            TestResult result = new TestResult(observedLR,excludedLR, disease, pretest);
            mapbuilder.put(diseaseId, result);
        }
        return mapbuilder.build();
    }

    /**
     * Perform the evaluation of the current case based on phenotype and genotype evidence
     * If {@link #keepIfNoCandidateVariant} is true, then we also rank differential diagnoses even
     * if (i) no disease gene is known or (ii) the disease gene is known but we did not find a
     * pathogenic variant. In the latter case, the candidate will be downranked, but can still score
     * highly if the phenotype evidence is very strong.
     * @return map with key=disease idea and value=corresponding {@link TestResult}
     */
    private Map<TermId,TestResult> phenoGenoEvaluation() {
        ImmutableMap.Builder<TermId,TestResult> mapbuilder = new ImmutableMap.Builder<>();
        for (TermId diseaseId : diseaseMap.keySet()) {
            HpoDisease disease = this.diseaseMap.get(diseaseId);
            double pretest = pretestProbabilityMap.get(diseaseId);
            List<Double> observedLR = observedPhenotypesLikelihoodRatios(diseaseId);
            List<Double> excludedLR = excludedPhenotypesLikelihoodRatios(diseaseId);
            TestResult result;
            // 2. get genotype LR if available
            Double genotypeLR=null;
            TermId geneId = null;
            Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
            if (!keepIfNoCandidateVariant && associatedGenes.isEmpty()) {
                continue; // this is a disease with no known disease gene -- we will skip it unless the user
                // indicates to keep it.
            }
            List<TermId> inheritancemodes= disease.getModesOfInheritance();
            List<String> noVariantAtAllFoundInGeneList = new ArrayList<>();
            boolean foundVariantInAtLeastOneGene=false;
            if (associatedGenes != null && associatedGenes.size() > 0) {
                for (TermId entrezGeneId : associatedGenes) {
                    Gene2Genotype g2g = this.genotypeMap.get(entrezGeneId);
                    Optional<Double> opt = this.genotypeLrEvalutator.evaluateGenotype(g2g,
                            inheritancemodes,
                            entrezGeneId);
                    if (g2g==null) {
                        String symbol = this.geneId2symbol.get(entrezGeneId);
                        noVariantAtAllFoundInGeneList.add(symbol);
                    } else {
                        foundVariantInAtLeastOneGene=true;
                    }
                    if (opt.isPresent()) {
                        if (genotypeLR == null) {
                            genotypeLR = opt.get();
                            geneId = entrezGeneId;
                        } else if (genotypeLR < opt.get()) { // if the new genotype LR is better, replace!
                            genotypeLR = opt.get();
                            geneId = entrezGeneId;
                        }
                    }
                }
            }else if (keepIfNoCandidateVariant) {
                System.out.println("BBBBBBBBBBBBBBB");
                result = new TestResult(observedLR, excludedLR, disease, pretest);
            }
            if (genotypeLR != null) {
                result = new TestResult(observedLR, excludedLR,disease, genotypeLR, geneId, pretest);
                if (!foundVariantInAtLeastOneGene) {
                    if (keepIfNoCandidateVariant) {
                        String expl = String.format("No variants found in disease-associated gene: ",
                                String.join("; ", noVariantAtAllFoundInGeneList));
                        result.appendToExplanation(expl);
                    } else {
                        continue; // skip because no variants were found.
                    }
                }

            } else {
                System.out.println("BBBBBBBBBBBBBBB   SHOULD NEVER GET HERE");
                System.out.println(String.join("; ",noVariantAtAllFoundInGeneList));
                System.out.println(disease.getName());
                continue;
            }
            if (result.getPosttestProbability() > this.threshold) {
                Gene2Genotype g2g = this.genotypeMap.get(geneId);
                double observedWeightedPathogenicVariantCount=0.0;
                if (g2g!=null) {
                    observedWeightedPathogenicVariantCount = g2g.getSumOfPathBinScores();
                }
                if (useGenotypeAnalysis) {
                    String exp = this.genotypeLrEvalutator.explainGenotypeScore(observedWeightedPathogenicVariantCount, inheritancemodes, geneId);
                    result.appendToExplanation(exp);
                }
            }
            mapbuilder.put(diseaseId, result);
        }
        return mapbuilder.build();
    }


    /** This method evaluates the likilihood ratio for each disease in
     * {@link #diseaseMap}. After this, it sorts the results (the best hit is then at index 0, etc).
     */
    public HpoCase evaluate()  {
        assert diseaseMap.size()== pretestProbabilityMap.size();
        Map<TermId,TestResult> evaluationmap;
        if (useGenotypeAnalysis) {
            evaluationmap = phenoGenoEvaluation();
        } else {
            evaluationmap = phenotypeOnlyEvaluation();
        }
        Map<TermId,TestResult> results = evaluateRanks(evaluationmap);
        HpoCase.Builder casebuilder = new HpoCase.Builder(phenotypicAbnormalities)
                .excluded(negatedPhenotypicAbnormalities)
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
            if (verbose && rank<11) {
                TermId diseaseCurie = res.getDiseaseCurie();
                String name = diseaseMap.get(diseaseCurie).getName();
                System.err.println(String.format("Rank #%d: %s [%s]",rank,name,diseaseCurie.getValue()));
            }
        }
        return resultMap;
    }



    /**
     * Convenience class for building a {@link CaseEvaluator} object--mainly to avoid having
     * a constructor with an extremely long list of arguments.
     */
    public static class Builder {
        /** The abnormalities observed in the individual being investigated. */
        private final List<TermId> hpoTerms;
        /** These abnormalities were excluded in the proband (i.e., normal). */
        private List<TermId> negatedHpoTerms=null;

        private Ontology ontology;
        /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
        private Map<TermId,HpoDisease> diseaseMap;
        /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
        private Multimap<TermId,TermId> disease2geneMultimap;
        /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
        private PhenotypeLikelihoodRatio phenotypeLR;

        private GenotypeLikelihoodRatio genotypeLR;
        /** Key: geneId (e.g., NCBI Entrez Gene); value: observed variants/genotypes as {@link org.monarchinitiative.lr2pg.analysis.Gene2Genotype} object.*/
        private Map<TermId,Gene2Genotype> genotypeMap;
        /** retain candidates even if no candidate variant is found (default: false)*/
        private boolean keepIfNoCandidateVariant=false;
        /** Key: an EntrezGene id; value: corresponding gene symbol. */
        private Map<TermId,String> geneId2symbol;

        private double threshold=DEFAULT_POSTERIOR_PROBABILITY_THRESHOLD;

        public Builder(List<TermId> hpoTerms){ this.hpoTerms=hpoTerms; }

        public Builder ontology(Ontology hont) { this.ontology=hont; return this;}

        public Builder diseaseMap(Map<TermId,HpoDisease> dmap) { this.diseaseMap=dmap; return this;}

        public Builder disease2geneMultimap(Multimap<TermId,TermId> d2gmmap) { this.disease2geneMultimap=d2gmmap; return this;}

        public Builder genotypeMap(Map<TermId,Gene2Genotype> gtmap) { this.genotypeMap=gtmap; return this;}

        public Builder phenotypeLr(PhenotypeLikelihoodRatio phenoLr) { this.phenotypeLR=phenoLr; return this; }

        public Builder genotypeLr(GenotypeLikelihoodRatio glr) { this.genotypeLR=glr; return this; }

        public Builder threshold(double t) { this.threshold=t; return this;}

        public Builder keepCandidates(boolean keep) {
            this.keepIfNoCandidateVariant=keep;
            return this;
        }
        public Builder negated(List<TermId> negated) {
            this.negatedHpoTerms=negated;
            return this;
        }

        public Builder gene2idMap( Map<TermId,String> geneId2symbol) {
            this.geneId2symbol=geneId2symbol;
            return this;
        }


        public CaseEvaluator build() {
            if (hpoTerms==null) {
                throw new Lr2PgRuntimeException("[ERROR] No HPO terms found. At least one HPO term required to run LR2PG");
            }
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(diseaseMap);
            Objects.requireNonNull(disease2geneMultimap);
            if (negatedHpoTerms==null) {
                negatedHpoTerms=ImmutableList.of();
            }
            return new CaseEvaluator(hpoTerms,
                    negatedHpoTerms,
                    ontology,
                    diseaseMap,
                    disease2geneMultimap,
                    phenotypeLR,
                    genotypeLR,
                    genotypeMap,
                    threshold,
                    keepIfNoCandidateVariant,
                    this.geneId2symbol);
        }


        public CaseEvaluator buildPhenotypeOnlyEvaluator() {
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(phenotypeLR);
            if (negatedHpoTerms==null) {
                negatedHpoTerms=ImmutableList.of();
            }
            return new CaseEvaluator(hpoTerms,negatedHpoTerms,ontology,diseaseMap,phenotypeLR);
        }
    }
    

}
