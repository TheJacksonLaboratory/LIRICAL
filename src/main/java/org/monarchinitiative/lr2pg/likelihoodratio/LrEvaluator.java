package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.BackgroundForegroundTermFrequency;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.model.Model;
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
public class LrEvaluator {
    private static final Logger logger = LogManager.getLogger();
    private final HpoCase hpocase;
    /** key: a disease CURIE, e.g., OMIM:600100; value-corresponding disease object.*/
    private final Map<TermId,HpoDisease> diseaseMap;
    /** Results of the LR calculations. */
    private final Map<HpoDisease,TestResult> disease2resultMap;
    private final Map<TermId,Double> pretestProbabilityMap;
    private final BackgroundForegroundTermFrequency bftfrequency;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private final List<TestResult> results;
    /** Entrez gene Curie, e.g., NCBIGene:2200; value--corresponding background frequency sum of pathogenic bin variants. */
    private Map<TermId,Double> genotypeMap;
    /* key: a gene CURIE such as NCBIGene:123; value: a collection of disease CURIEs such as OMIM:600123; */
    private Multimap<TermId,TermId> disease2geneMultimap;

    private double diseaseLambda;

    private TermId entrezGeneId;

    private Map<TermId,Double> gene2backgroundFrequency;


    /** {@link TermId} for "X-linked recessive inheritance. */
    private static final TermId X_LINKED_RECESSIVE = TermId.constructWithPrefix("HP:0001419");
    /** {@link TermId} for "autosomal recessive inheritance. */
    private static final TermId AUTOSOMAL_RECESSIVE = TermId.constructWithPrefix("HP:0000007");
    /** {@link TermId} for "autosomal dominant inheritance. */
    private static final TermId AUTOSOMAL_DOMINANT = TermId.constructWithPrefix("HP:0000006");


    public LrEvaluator(HpoCase hpcase, Map<TermId,HpoDisease> diseaseMap, HpoOntology ont,BackgroundForegroundTermFrequency bftfrequency) {
        this.hpocase=hpcase;
        this.diseaseMap=diseaseMap;
        this.disease2resultMap = new HashMap<>();
        this.bftfrequency=bftfrequency;

        // initialize to all equal pretest probabilities.
        int n=diseaseMap.size();
        this.pretestProbabilityMap =new HashMap<>();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
         pretestProbabilityMap.put(tid,prob);
        }
        this.ontology=ont;
        results=new ArrayList<>();
    }


    public LrEvaluator(Model model) {
        this.hpocase=model.getCurrentCase();
        this.diseaseMap=model.getDiseaseMap();
        this.disease2resultMap=new HashMap<>();
        this.bftfrequency=model.getBftfrequency();
        // initialize to all equal pretest probabilities.
        int n=diseaseMap.size();
        this.pretestProbabilityMap =new HashMap<>();
        double prob=1.0/(double)n;
        for (TermId tid : diseaseMap.keySet()) {
            pretestProbabilityMap.put(tid,prob);
        }
        this.ontology=model.getOntology();
        results=new ArrayList<>();
        this.genotypeMap=model.getGene2BackgroundFreq();
        this.disease2geneMultimap = model.getDisease2geneMultimap();
        this.diseaseLambda = model.getVariantCount() * model.getMeanVariantPathogenicity();
        this.entrezGeneId=model.getEntrezGeneId();
        this.gene2backgroundFrequency=model.getBackgroundFrequency();
    }

    private static final double DEFAULT_LAMBDA_BACKGROUND=0.1;

    TermId fbn1 = TermId.constructWithPrefix("NCBIGene:2200");

    private Double evaluateGenotype(HpoDisease disease, TermId geneId) {
        List<TermId> inheritancemodes=disease.getModesOfInheritance();
        double lambda_disease=1.0;
        if (inheritancemodes!=null && inheritancemodes.size()>0) {
            TermId tid = inheritancemodes.get(0);
            if (tid.equals(AUTOSOMAL_RECESSIVE) || tid.equals(X_LINKED_RECESSIVE)) {
                lambda_disease=2.0;
            }
        }
        double lambda_background = this.gene2backgroundFrequency.getOrDefault(geneId, DEFAULT_LAMBDA_BACKGROUND);
        if (! this.genotypeMap.containsKey(geneId)) {
            return null;
        }
        double x = this.genotypeMap.get(geneId);
        PoissonDistribution pdDisease = new PoissonDistribution(lambda_disease);
        double D = pdDisease.probability(x);
        PoissonDistribution pdBackground = new PoissonDistribution(lambda_background);

        double B = pdBackground.probability(x);
        if (geneId.equals(fbn1)) {
            System.err.println(String.format("disease lambda %f background lambda %f disedase prob %f background prob %f  x=%f",diseaseLambda,lambda_background,
                    D,B,x));
        }
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
        for (HpoDisease disease : diseaseMap.values()) {
            double pretest = pretestProbabilityMap.get(disease.getDiseaseDatabaseId());
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.hpocase.getObservedAbnormalities()) {
                double LR = bftfrequency.getLikelihoodRatio(tid,disease);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder.build(),disease.getDiseaseDatabaseId(),pretest);
            Collection<TermId> associatedGenes = disease2geneMultimap.get(disease.getDiseaseDatabaseId());
            if (associatedGenes != null && associatedGenes.size()>0) {
                Double max=null;
                for (TermId entrezGeneId : associatedGenes) {
                    Double LR = evaluateGenotype(disease,entrezGeneId);
                    if (LR!=null) {
                        if (max==null){max=LR;}
                        else if (LR>max) { max=LR; }
                    }
                }
                if (max!=null) {
                    result.setGeneLikelihoodRatio(max,entrezGeneId);
                }
            }
            disease2resultMap.put(disease,result);
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
     * @param disease the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(HpoDisease disease){
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
    

}
