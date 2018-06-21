package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.BackgroundForegroundTermFrequency;
import org.monarchinitiative.lr2pg.hpo.VcfSimulator;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.poisson.PoissonDistribution;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test on
 * an {@link HpoCase}.
 */
public class CaseEvaluator {
    private static final Logger logger = LogManager.getLogger();
    private final HpoCase hpocase;
    /** key: a disease CURIE, e.g., OMIM:600100; value-corresponding disease object.*/
    private final Map<TermId,HpoDisease> diseaseMap;
    /** Results of the LR calculations. */
    private final Map<TermId,TestResult> disease2resultMap;
    /** Probability of diseases before testing (e.g., prevalence or 1/N).*/
    private final Map<TermId,Double> pretestProbabilityMap;
    /** Object used to calculate phenotype likelihood ratios. */
    private final BackgroundForegroundTermFrequency bftfrequency;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private final List<TestResult> results;
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private Multimap<TermId,TermId> disease2geneMultimap;


    /** Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency sum of pathogenic bin variants. */
    private final Map<TermId,Double> gene2backgroundFrequency;
    /** Map of the observed genotypes in the VCF file. Key is an EntrezGene is, and the value is the average pathogenicity score times the
     * count of all variants in the pathogenic bin.*/
    private final Map<TermId,Double> genotypeMap;


    private static final double DEFAULT_LAMBDA_BACKGROUND=0.1;
    private double diseaseLambda;

    private TermId entrezGeneId;


    /** {@link TermId} for "X-linked recessive inheritance. */
    private static final TermId X_LINKED_RECESSIVE = TermId.constructWithPrefix("HP:0001419");
    /** {@link TermId} for "autosomal recessive inheritance. */
    private static final TermId AUTOSOMAL_RECESSIVE = TermId.constructWithPrefix("HP:0000007");
    /** {@link TermId} for "autosomal dominant inheritance. */
    private static final TermId AUTOSOMAL_DOMINANT = TermId.constructWithPrefix("HP:0000006");






    private CaseEvaluator(List<TermId> hpoTerms,
                          HpoOntology ontology,
                          Map<TermId,HpoDisease> diseaseMap,
                          Multimap<TermId,TermId> disease2geneMultimap,
                          BackgroundForegroundTermFrequency bftfrequency,
                          Map<TermId,Double> genotypeMap,
                          Map<TermId,Double> g2b) {
        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        termIdBuilder.addAll(hpoTerms);
        ImmutableList<TermId> termlist = termIdBuilder.build();
        this.hpocase = (new HpoCase.Builder(termlist)).build();
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.disease2geneMultimap=disease2geneMultimap;
        this.bftfrequency=bftfrequency;
        this.disease2resultMap = new HashMap<>();
        // For now, assume equal pretest probabilities
        this.pretestProbabilityMap =new HashMap<>();
        int n=diseaseMap.size();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.gene2backgroundFrequency=g2b;
        this.genotypeMap=genotypeMap;
        // This where we will store the results
        this.results=new ArrayList<>();

    }







    private Double evaluateGenotype(TermId diseaseId, TermId geneId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        List<TermId> inheritancemodes=disease.getModesOfInheritance();
        double lambda_disease=1.0;
        if (inheritancemodes!=null && inheritancemodes.size()>0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
            }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        if (! this.gene2backgroundFrequency.containsKey(geneId)) {
            return null;
        }
        double x = this.gene2backgroundFrequency.get(geneId);
        PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
        double D = pdDisease.probability(x);
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);
        double B = pdBackground.probability(x);
//        if (geneId.equals(fbn1)) {
//            System.err.println(String.format("disease lambda %f background lambda %f disedase prob %f background prob %f  x=%f",diseaseLambda,lambda_background,
//                    D,B,x));
//        }
        if (B>0 && D>0) {
            return D/B;
        } else {
            return null;
        }
    }




    /** This method evaluates the likilihood ratio for each disease in
     * {@link #diseaseMap}. After this, it sorts the results (the best hit is then at index 0, etc).
     */
    public void evaluate()  {
        assert diseaseMap.size()== pretestProbabilityMap.size();
        for (TermId diseaseId : diseaseMap.keySet()) {
            double pretest = pretestProbabilityMap.get(diseaseId);
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.hpocase.getObservedAbnormalities()) {
                double LR = bftfrequency.getLikelihoodRatio(tid,diseaseId);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder.build(),diseaseId,pretest);
            Collection<TermId> associatedGenes = disease2geneMultimap.get(diseaseId);
            if (associatedGenes != null && associatedGenes.size()>0) {
                Double max=null;
                for (TermId entrezGeneId : associatedGenes) {
                    Double LR = evaluateGenotype(diseaseId,entrezGeneId);
                    if (LR!=null) {
                        if (max==null){max=LR;}
                        else if (LR>max) { max=LR; }
                    }
                }
                if (max!=null) {
                    result.setGeneLikelihoodRatio(max,entrezGeneId);
                }
            }
            disease2resultMap.put(diseaseId,result);
            results.add(result);
        }
        results.sort(Collections.reverseOrder());
    }


    public TestResult getResult(TermId diseaseId) {
        return disease2resultMap.get(diseaseId);
    }


    /**
     * This method sorts all of the results in {@link #results}. The best results are the highest, and so
     * we sort in descending order. We return the rank of the item
     * @param diseaseId CURIE (e.g., OMIM:600100) of the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(TermId diseaseId){
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        int rank=0;
        if (disease==null) {
            logger.error("null disease in getRank");
            return 0;
        }
        for (TestResult r: results){
            rank++;
            if (r==null) {
                logger.error("result at rank " + rank + " null in getRank");
                continue;
            }
            if (r.getDiseaseCurie()==null) {
                logger.error("Result::getDiseaseCurie at rank " + rank + " null in getRank");
                continue;
            }
            if (r.getDiseaseCurie().equals(disease.getDiseaseDatabaseId())) {
                //outputResults();
                outputLR(r,disease, rank);
                return rank;
            }
        }
        return rank;
    }

    /** Output the results for a specific HPO disease. */

        private void outputLR(TestResult r, HpoDisease hpoDisease, int rank) {
            System.err.println("Likelihood ratios for " + hpoDisease.getName() + "[" + hpoDisease.getDiseaseDatabaseId() +
                    "]\tRank="+rank);

            DecimalFormat df = new DecimalFormat("0.000E0");
            System.err.println(String.format("Pretest probability: %s; Composite LR: %.2f; Posttest probability: %s ",
                    niceFormat(r.getPretestProbability()),
                    r.getCompositeLR(),
                    niceFormat(r.getPosttestProbability())));
            for (int i=0;i<r.getNumberOfTests();i++) {
                double ratio = r.getRatio(i);
                TermId tid =hpocase.getObservedAbnormalities().get(i);
                String term = String.format("%s [%s]",ontology.getTermMap().get(tid).getName(),tid.getIdWithPrefix() );
                System.err.println(String.format("%s: ratio=%s",term,niceFormat(ratio)));
            }
            if (r.hasGenotype()) {
                System.err.println(String.format("Genotype LR for %s: %f",r.getEntrezGeneId(),r.getGenotypeLR()));
            }
            System.err.println();
    }


    public void writeSvg(){
//        Lr2Svg l2svg = new Lr2Svg(this.hpocase,result,this.ontology);
//        String fname = String.format("%s.svg",diseaseId.getIdWithPrefix().replace(':','_') );
//        l2svg.writeSvg(fname);
    }


    public HpoDisease id2disease(TermId diseaseCurie) {
         return this.diseaseMap.get(diseaseCurie);
    }

    public String i2diseaseName(TermId diseaseCurie) {
        return this.diseaseMap.get(diseaseCurie).getName();
    }


    private String niceFormat(double d) {
        DecimalFormat df = new DecimalFormat("0.000E0");
        if (d > 1.0) {
            return String.format("%.2f", d);
        } else if (d > 0.005) {
            return String.format("%.4f", d);
        } else {
            return df.format(d);
        }
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
        private BackgroundForegroundTermFrequency bftfrequency;

        private Map<TermId,Double> genotypeMap;

        private Map<TermId,Double> gene2backgroundFrequency;

        public Builder(List<TermId> hpoTerms){ this.hpoTerms=hpoTerms; }

        public Builder ontology(HpoOntology hont) { this.ontology=hont; return this;}

        public Builder diseaseMap(Map<TermId,HpoDisease> dmap) { this.diseaseMap=dmap; return this;}

        public Builder disease2geneMultimap(Multimap<TermId,TermId> d2gmmap) { this.disease2geneMultimap=d2gmmap; return this;}

        public Builder genotypeMap(Map<TermId,Double> gtmap) { this.genotypeMap=gtmap; return this;}

        public Builder gene2backgroundFrequency(Map<TermId,Double> g2b) { this.gene2backgroundFrequency=g2b; return this;}


        public CaseEvaluator build() {
            Objects.requireNonNull(hpoTerms);
            Objects.requireNonNull(ontology);
            Objects.requireNonNull(diseaseMap);
            Objects.requireNonNull(disease2geneMultimap);

            BackgroundForegroundTermFrequency bftfrequency = new BackgroundForegroundTermFrequency(ontology,diseaseMap);

            return new CaseEvaluator(hpoTerms,ontology,diseaseMap,disease2geneMultimap,bftfrequency,genotypeMap,gene2backgroundFrequency);
        }



    }
    

}
