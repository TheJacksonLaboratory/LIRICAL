package org.monarchinitiative.lr2pg.likelihoodratio;


import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. The class is intended to be used together
 * with the class {@link HpoCase}, which contains lists of observed and excluded HPO terms. For each
 * disease in the database, the likelihood ratios of these phenotypes is calculated, and the result
 * for each disease is stored in an object of this class. The order of the individual phenotypes in
 * {@link HpoCase} is the same as the order of the corresponding test rests in this class: {@link #results}
 * for the observed phenotypes and {@link #excludedResults} for the phenotypes that were excluded in
 * the patient. This object can include the result of a likelihood ratio for a genotype test. However,
 * not every disease is associated with a known disease gene. Therefore, if no genotype is available,
 * {@link #genotypeLR} and {@link #entrezGeneId} are null.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.4.5 (2019-10-28)
 */
public class TestResult implements Comparable<TestResult> {
    private static final Logger logger = LoggerFactory.getLogger(TestResult.class);
    private static final String EMPTY_STRING="";
    /**A list of results for the tests performed on observed phenotypes for {@link #hpoDisease}.*/
    private final List<Double> results;
    /** A list of test results for phenotypes that were excluded.*/
    private final List<Double> excludedResults;
    /** Result of the likelhood ratio test for the genotype. */
    private final Double genotypeLR;
    /** The id of the gene associated with ths disease being tested here. */
    private final TermId entrezGeneId;
    /** This is the product of the individual test results. */
    private final double compositeLR;
    /** Reference to the the disease that we are testing (e.g., OMIM:600100).*/
    private final HpoDisease hpoDisease;
    /** The probability of some result before the first test is done.*/
    private final double pretestProbability;
    /** The probability of some result after testing.*/
    private final double posttestProbability;
    /** The overall rank of the the result withint the differential diagnosis. */
    private int rank;
    /** An optional explanation of the result, intended for display */
    private String explanation=EMPTY_STRING;

    /**
     * The constructor initializes the variables and calculates {@link #compositeLR}
     *
     * @param reslist list of individual test results for observed phenotypes
     * @param excllist list of individual test results for excluded phenotypes
     * @param diseaseId name of the disease being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(List<Double> reslist, List<Double> excllist, HpoDisease diseaseId, double pretest) {
        this.results = reslist;
        this.excludedResults=excllist;
        this.hpoDisease = diseaseId;
        this.pretestProbability = pretest;
        // the composite LR is the product of the individual LR's
        double observed=reslist.stream().reduce(1.0, (a, b) -> a * b);
        if (excludedResults.size()>0) {
            double excluded = excludedResults.stream().reduce(1.0, (a, b) -> a * b);
            this.compositeLR=observed*excluded;
        } else {
            this.compositeLR=observed;
        }

        this.genotypeLR=null;// result without genotype.
        this.entrezGeneId=null;
        this.posttestProbability=getPosttestProbability();
    }

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     * @param reslist list of individual test results for observed phenotypes
     * @param excllist list of individual test results for excluded phenotypes
     * @param diseaseId name of the disease being tested
     * @param genotypeLr LR result for the genotype
     * @param geneId gene id of the gene being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(List<Double> reslist, List<Double> excllist,HpoDisease diseaseId, Double genotypeLr,TermId geneId,double pretest) {
        this.results = reslist;
        this.excludedResults=excllist;
        this.hpoDisease = diseaseId;
        this.pretestProbability = pretest;
        this.genotypeLR=genotypeLr;
        this.entrezGeneId=geneId;
        // the composite ratio is equal to the product of the phenotype LR's
        // multiplied by the genotype LR.
        double observed=reslist.stream().reduce(1.0, (a, b) -> a * b);
        if (excludedResults.size()>0) {
            double excluded = excludedResults.stream().reduce(1.0, (a, b) -> a * b);
            this.compositeLR=observed*excluded*genotypeLr;
        } else {
            this.compositeLR=observed*genotypeLr;
        }
        posttestProbability=getPosttestProbability();
    }



    /** @return the composite likelihood ratio (product of the LRs of the individual tests).*/
    public double getCompositeLR() {
        return genotypeLR!=null ? genotypeLR*compositeLR : compositeLR;
    }

    /** @return the total count of tests performed (including one for the genotype if done).*/
    public int getNumberOfTests() {
        return results.size();
    }

    /** @return the pretest odds.*/
    public double pretestodds() {
        return pretestProbability / (1.0 - pretestProbability);
    }

    /** @return the post-test odds. */
    public double posttestodds() {
        double pretestodds = pretestodds();
        return pretestodds * getCompositeLR();
    }


    public double getPretestProbability() {
        return pretestProbability;
    }

    public double getPosttestProbability() {
        double po = posttestodds();
        return po / (1 + po);
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    /**
     * Compare two TestResult objects based on their {@link #compositeLR} value.
     * @param other the "other" TestResult being compared.
     * @return comparison result
     */
    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") TestResult other) {
        return Double.compare(posttestProbability, other.posttestProbability);
    }


    @Override
    public String toString() {
        String resultlist = results.stream().map(String::valueOf).collect(Collectors.joining(";"));
        String genoResult = hasGenotype() ? String.format("genotype LR: %.4f",this.genotypeLR) : "no genotype LR";
        return String.format("%s: %.2f [%s] %s", hpoDisease, getCompositeLR(), resultlist, genoResult);
    }

    /**
     * @param i index of the test we are interested in for an observed phenotype
     * @return the likelihood ratio of the i'th test
     */
    public double getObservedPhenotypeRatio(int i) {
        return this.results.get(i);
    }

    /**
     * @param i index of the test we are interested in for an excluded phenotype
     * @return the likelihood ratio of the i'th test
     */
    public double getExcludedPhenotypeRatio(int i) {
        return this.excludedResults.get(i);
    }

    /** @return name of the disease being tested. */
    public TermId getDiseaseCurie() {
        return hpoDisease.getDiseaseDatabaseId();
    }
    /** @return the name of the disease, e.g., Marfan syndrome. */
    public String getDiseaseName() { return hpoDisease.getName();}
    /**@return true if a genotype likelihood ratio was assigned to this test result. */
    public boolean hasGenotype(){ return genotypeLR!=null;}

    public Double getGenotypeLR() {
        return genotypeLR;
    }

    public TermId getEntrezGeneId() {
        return entrezGeneId;
    }

    public void appendToExplanation(String text) { this.explanation = this.explanation + text; }
    public String getExplanation() { return this.explanation; }
    public boolean hasExplanation() { return ! this.explanation.isEmpty();}
}
