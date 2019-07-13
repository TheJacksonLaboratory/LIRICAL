package org.monarchinitiative.lirical.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test
 *  and returns one {@link HpoCase} object with the results by the method {@link #evaluate()}.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class CaseEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(CaseEvaluator.class);
    /** List of abnormalities seen in the person being evaluated. */
    private List<TermId> phenotypicAbnormalities;
    /** List of abnormalities excluded in the person being evaluated. */
    private List<TermId> negatedPhenotypicAbnormalities;
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
    private final boolean keepIfNoCandidateVariant;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    private Map<TermId,String> geneId2symbol;
    /** If true, then genotype information is available for the analysis. Otherwise, skip it. */
    private final boolean useGenotypeAnalysis;

    private boolean verbose=true;

    private List<LrWithExplanation> currentPhenotypeExplanation;

    private List<String> errors;

    private final static String EMPTY_STRING="";

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
        this.keepIfNoCandidateVariant=true; // needs to be true for phenotype-only analysis!
        this.errors=new ArrayList<>();
    }


    /**
     * Constructor for LIRICAL anaysis with a VCF file.
     * @param hpoTerms list of observed abnormalities
     * @param negatedHpoTerms list of excluded abnormalities
     * @param ontology reference to HPO ontology
     * @param diseaseMap map to HPO disease objects
     * @param disease2geneMultimap map from disease id to the corresponding gene symbols
     * @param phenotypeLrEvaluator reference to object that evaluates the phenotype LR
     * @param genotypeLrEvalutator reference to object that evaluates the genotype LR
     * @param genotypeMap Map of gene symbol to genotype evaluations
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
                          boolean keep,
                          Map<TermId,String> geneId2symbol) {
        this.phenotypicAbnormalities=hpoTerms;
        this.negatedPhenotypicAbnormalities=negatedHpoTerms;
        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.phenotypeLRevaluator =phenotypeLrEvaluator;
        this.genotypeLrEvalutator=genotypeLrEvalutator;
        this.ontology=ontology;
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
        this.errors=new ArrayList<>();
    }

    public void setVerbosity(boolean v) { this.verbose=v;}


    private List<Double> observedPhenotypesLikelihoodRatios(TermId diseaseId) {
        ImmutableList.Builder<Double> builderObserved = new ImmutableList.Builder<>();
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        InducedDiseaseGraph idg = new InducedDiseaseGraph(disease,ontology);
        for (TermId tid : this.phenotypicAbnormalities) {
            try {
                LrWithExplanation lrwe = phenotypeLRevaluator.getLikelihoodRatio(tid, idg);
                builderObserved.add(lrwe.getLR());
                this.currentPhenotypeExplanation.add(lrwe);
            } catch (Exception e) {
                String errormsg = String.format("%s (%s/%s)",e.getMessage(),diseaseMap.get(diseaseId).getName(),tid.getValue() );
                this.errors.add(errormsg);
            }
            /*logger.error("{}: {} {} [{}]",
                    ,
                    ontology.getTermMap().get(tid).getName(),
                    lrwe.getLR(),
                    lrwe.getExplanation(ontology));*/
        }
        return builderObserved.build();
    }

    private List<Double> excludedPhenotypesLikelihoodRatios(TermId diseaseId) {
        ImmutableList.Builder<Double> builderExcluded = new ImmutableList.Builder<>();
        for (TermId negated : this.negatedPhenotypicAbnormalities) {
            LrWithExplanation lrwe = phenotypeLRevaluator.getLikelihoodRatioForExcludedTerm(negated, diseaseId);
            this.currentPhenotypeExplanation.add(lrwe);
            double LR = lrwe.getLR();
            builderExcluded.add(LR);
        }
        return builderExcluded.build();
    }

    public List<String> getErrors(){ return this.errors;}


    /**
     * Perform the evaluation of the current case based only on phenotype evidence
     * @return map with key=disease idea and value=corresponding {@link TestResult}
     */
    private  Map<TermId,TestResult>  phenotypeOnlyEvaluation() {
        ImmutableMap.Builder<TermId,TestResult> mapbuilder = new ImmutableMap.Builder<>();
        for (TermId diseaseId : diseaseMap.keySet()) {
            this.currentPhenotypeExplanation=new ArrayList<>();
            Optional<TestResult> opt =evaluateDiseasePhenotypeOnly(diseaseId);
            if (opt.isPresent())
                mapbuilder.put(diseaseId,opt.get());
        }
        return mapbuilder.build();
    }


    /**
     * This method calculates the likelihood ratio based only on phenotype. It is inteded to be used
     * for analyses where we do not have an exome or genome. Note that we return an optional because
     * with some user settings some differentials will be skipped.
     * @param diseaseId The disease being tested
     * @return The corresponding TestResult.
     */
    private Optional<TestResult> evaluateDiseasePhenotypeOnly(TermId diseaseId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        double pretest = pretestProbabilityMap.get(diseaseId);
        List<Double> observedLR = observedPhenotypesLikelihoodRatios(diseaseId);
        List<Double> excludedLR = excludedPhenotypesLikelihoodRatios(diseaseId);
        TestResult result= new TestResult(observedLR, excludedLR, disease, pretest);
        String phenoExp = getPhenotypeExplanation();
        result.setPhenotypeExplanation(phenoExp);
        return Optional.of(result);
    }

    /**
     * This method is used to calculate the likelihood ratio based on both phenotype and genotype
     * It should be used for analyses where we have an exome or genome. This method is used if
     * the user has indicated that they want to see all differential diagnoses, including those
     * that do not have a known disease gene and those where no pathogenic variant was found
     * in the exome/genome VCF file.
     * @param diseaseId The disease being tested
     * @return The corresponding TestResult.
     */
    private Optional<TestResult> evaluateDiseaseKeepingAllCandidates(TermId diseaseId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        double pretest = pretestProbabilityMap.get(diseaseId);
        List<Double> observedLR = observedPhenotypesLikelihoodRatios(diseaseId);
        List<Double> excludedLR = excludedPhenotypesLikelihoodRatios(diseaseId);
        TestResult result;
        Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
        if ( associatedGenes.isEmpty()) {
            // this is a disease with no known disease gene
            result = new TestResult(observedLR, excludedLR, disease, pretest);
            return Optional.of(result);
        }
        // If we get here, then the disease is associated with one or multiple genes
        // The disease may also be associated with multiple modes of inheritance (this happens rarely)
        List<TermId> inheritancemodes= disease.getModesOfInheritance();
        List<String> genesWithNoIdentifiedVariant = new ArrayList<>(); // we keep track of this for the HTML output
        boolean foundPredictedPathogenicVariant=false;
        Double genotypeLR=null;
        TermId geneId = null;
        // if we get here, then associatedGenes is not empty
        for (TermId entrezGeneId : associatedGenes) {
            // if there is no Gene2Genotype object in the map, then no variant in the gene was found in the VCF
            Gene2Genotype g2g = this.genotypeMap.getOrDefault(entrezGeneId, Gene2Genotype.NO_IDENTIFIED_VARIANT);
            // The following two special cases are if no variant was found or if a ClinVar-pathogenic variant was found.
            if (g2g.equals(Gene2Genotype.NO_IDENTIFIED_VARIANT)) {
                String symbol = this.geneId2symbol.get(entrezGeneId);
                genesWithNoIdentifiedVariant.add(symbol);
            } else {
                if (g2g.hasPathogenicClinvarVar() || g2g.hasPredictedPathogenicVar()) {
                    foundPredictedPathogenicVariant = true;
                }
            }
            Double score = this.genotypeLrEvalutator.evaluateGenotype(g2g,
                    inheritancemodes,
                    entrezGeneId);
            if (genotypeLR == null) { // this is the first iteration
                genotypeLR = score;
                geneId = entrezGeneId;
            } else if (genotypeLR < score) { // if the new genotype LR is better, replace!
                genotypeLR = score;
                geneId = entrezGeneId;
            }
        }
        // when we get here, we have checked for variants in all genes associated with the disease.
        // genotypeLR has the most pathogenic genotype score for all associated genes, or is null if
        // no variants in any associated gene were found.
        //when we get here, genotypeLR is not null
        result = new TestResult(observedLR, excludedLR, disease, genotypeLR, geneId, pretest);
        if (!foundPredictedPathogenicVariant) {
            String expl = String.format("No variants found in disease-associated gene%s: %s",
                    genesWithNoIdentifiedVariant.size() > 1 ? "s" : EMPTY_STRING,
                    String.join("; ", genesWithNoIdentifiedVariant));
            result.setGenotypeExplanation(expl);

        } else {
            // if we get here, then foundPredictedPathogenicVariant is true.
            Gene2Genotype g2g = this.genotypeMap.get(geneId);
            String exp = getGenotypeScoreExplanation(g2g, inheritancemodes,geneId);
            result.setGenotypeExplanation(exp);
        }
        String phenoExp = getPhenotypeExplanation();
        result.setPhenotypeExplanation(phenoExp);
        return Optional.of(result);
    }

    /**
     * Convenience function to create an explanation for the genotype score that we show in the HTML output
     * @param g2g The gene in question
     * @param inheritancemodes Modes of inheritance of diseases associated with this gene
     * @param geneId The NCBI Gene id
     * @return the explanation for the score.
     */
    private String getGenotypeScoreExplanation(Gene2Genotype g2g, List<TermId> inheritancemodes, TermId geneId) {
        return this.genotypeLrEvalutator.explainGenotypeScore(g2g, inheritancemodes, geneId);
    }


    private String getPhenotypeExplanation() {
        ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
        Collections.sort(this.currentPhenotypeExplanation);
        for (LrWithExplanation lrwe:this.currentPhenotypeExplanation) {
            String e = lrwe.getEscapedExplanation(this.ontology);
            builder.add(e);
        }
        List<String> l = builder.build();
        return String.join(": ",l);
    }

    /**
     * Calculate the likelihood ratio for diseaseId. If there is no predicted pathogenic variant in the exome/genome file,
     * then we will return Optional.empty(), which will cause this diseases to be skipped in the differential diagnosis.
     * @param diseaseId an Id for a disease entry, e.g., OMIM:157000.
     * @return A TestResult for diseaseId, or Optional.empty() if no pathogenic variant was found in the associated gene(s).
     */
    private Optional<TestResult> evaluateDisease(TermId diseaseId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        double pretest = pretestProbabilityMap.get(diseaseId);
        List<Double> observedLR = observedPhenotypesLikelihoodRatios(diseaseId);
        List<Double> excludedLR = excludedPhenotypesLikelihoodRatios(diseaseId);
        String phenoExp = getPhenotypeExplanation();
        TestResult result;
        Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
        if (associatedGenes.isEmpty()) {
            // this is a disease with no known disease gene
            if (keepIfNoCandidateVariant) {
                // if keepIfNoCandidateVariant is true then the user wants to
                // keep differentials with no associated gene
                // we create the TestResult based solely on the Phenotype data.
                result = new TestResult(observedLR, excludedLR, disease, pretest);
                result.setPhenotypeExplanation(phenoExp);
                return Optional.of(result);
            } else {
                // we skip this differential because there is no associated gene
                return Optional.empty();
            }
        }
        // If we get here, then the disease is associated with one or multiple genes
        // The disease may also be associated with multiple modes of inheritance (this happens rarely)
        List<TermId> inheritancemodes = disease.getModesOfInheritance();
        List<String> genesWithNoIdentifiedVariant = new ArrayList<>(); // we keep track of this for the HTML output
        boolean foundPredictedPathogenicVariant = false;
        Double genotypeLR = null;
        TermId geneId = null;
        if (associatedGenes.size() > 0) {
            for (TermId entrezGeneId : associatedGenes) {
                // if there is no Gene2Genotype object in the map, then no variant in the gene was found in the VCF
                Gene2Genotype g2g = this.genotypeMap.getOrDefault(entrezGeneId, Gene2Genotype.NO_IDENTIFIED_VARIANT);
                // Set foundPredictedVariant to true if we found a variant in this gene and it was either a
                // known ClinVar-pathogenic variant or we predicted it to be pathogenic.
                if (! g2g.equals(Gene2Genotype.NO_IDENTIFIED_VARIANT) &&
                     (g2g.hasPathogenicClinvarVar() || g2g.hasPredictedPathogenicVar())) {
                        foundPredictedPathogenicVariant = true;
                }
                double score = this.genotypeLrEvalutator.evaluateGenotype(g2g,
                        inheritancemodes,
                        entrezGeneId);
                if (genotypeLR == null) { // this is the first iteration
                    genotypeLR = score;
                    geneId = entrezGeneId;
                } else if (genotypeLR < score) { // if the new genotype LR is better, replace!
                    genotypeLR = score;
                    geneId = entrezGeneId;
                }
            }
        } else {
            // skip this disease since we did not find any disease associated variants.
            return Optional.empty();
        }
        // when we get here, we have checked for variants in all genes associated with the disease.
        // genotypeLR has the most pathogenic genotype score for all associated genes, or is null if
        // no variants in any associated gene were found.
        if (genotypeLR == null) {
            // no variants found, skip this disease
            return Optional.empty();
        }
        if (!foundPredictedPathogenicVariant) {
            return Optional.empty(); // Skip this disease since there was no pathogenic variant.
        } else {
            // if we get here, then foundPredictedPathogenicVariant is true.
            result = new TestResult(observedLR, excludedLR, disease, genotypeLR, geneId, pretest);
            Gene2Genotype g2g = this.genotypeMap.get(geneId);
            if (g2g==null) { // in this case, there was no variant in the gene
                System.err.println("g2g null");
                System.err.println("geneId =\"" + geneId +"\"");
                System.err.println("disease " + disease.getName());
                g2g=Gene2Genotype.NO_IDENTIFIED_VARIANT;
            }
            if (inheritancemodes == null) {
                System.err.println("inheritancemodes null");
            }
            if (geneId == null) {
                System.err.println("geneId null");
            }
            String exp  = getGenotypeScoreExplanation(g2g, inheritancemodes,geneId);
            result.setGenotypeExplanation(exp);
            result.setPhenotypeExplanation(phenoExp);
            return Optional.of(result);
        }
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
//            String idd = diseaseId.getValue();
//            System.out.print(idd);
            Optional<TestResult> optionalTestResult;
            this.currentPhenotypeExplanation=new ArrayList<>();
            if (useGenotypeAnalysis) {
                if (keepIfNoCandidateVariant) {
                    optionalTestResult = evaluateDiseaseKeepingAllCandidates(diseaseId);
                } else {
                    optionalTestResult = evaluateDisease(diseaseId);
                }
            } else {
                optionalTestResult=evaluateDiseasePhenotypeOnly(diseaseId);
            }
            // some differentials will be completely skipped depending on user settings
            // for instance, we might skip differentials if there is no associated gene
            // in this case, evaluateDisease returns an empty Optional and we just skip it here.
            optionalTestResult.ifPresent(testResult -> mapbuilder.put(diseaseId, testResult));
        }
        return mapbuilder.build();
    }


    /** This method evaluates the likelihood ratio for each disease in
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


    /**
     * This function sets the rank of the {@link TestResult} objects.
     * @param resultMap The objects of the resultMap are not not set wrt rank before thie function is called
     * @return same resultMap, but the TestResult objects have their ranks set.
     */
    private Map<TermId,TestResult> evaluateRanks(Map<TermId,TestResult> resultMap) {
        List<TestResult> results = new ArrayList<>(resultMap.values());
       results.sort(Collections.reverseOrder());
        int rank=0;
        for (TestResult res : results) {
            rank++;
            res.setRank(rank);
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
        /** Key: geneId (e.g., NCBI Entrez Gene); value: observed variants/genotypes as {@link org.monarchinitiative.lirical.analysis.Gene2Genotype} object.*/
        private Map<TermId,Gene2Genotype> genotypeMap;
        /** retain candidates even if no candidate variant is found (default: false)*/
        private boolean keepIfNoCandidateVariant=false;
        /** Key: an EntrezGene id; value: corresponding gene symbol. */
        private Map<TermId,String> geneId2symbol;

        public Builder(List<TermId> hpoTerms){ this.hpoTerms=hpoTerms; }

        public Builder ontology(Ontology hont) { this.ontology=hont; return this;}

        public Builder diseaseMap(Map<TermId,HpoDisease> dmap) { this.diseaseMap=dmap; return this;}

        public Builder disease2geneMultimap(Multimap<TermId,TermId> d2gmmap) { this.disease2geneMultimap=d2gmmap; return this;}

        public Builder genotypeMap(Map<TermId,Gene2Genotype> gtmap) { this.genotypeMap=gtmap; return this;}

        public Builder phenotypeLr(PhenotypeLikelihoodRatio phenoLr) { this.phenotypeLR=phenoLr; return this; }

        public Builder genotypeLr(GenotypeLikelihoodRatio glr) { this.genotypeLR=glr; return this; }

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
                throw new LiricalRuntimeException("[ERROR] No HPO terms found. At least one HPO term required to run LIRICAL");
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
