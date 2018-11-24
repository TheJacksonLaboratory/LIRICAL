package org.monarchinitiative.lr2pg.likelihoodratio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. For instance,  a GDx VCC test for
 * glaucoma may result in a measurement of 48, a value that is known to
 * have a 60% sensitivity and 97% specificity for glaucoma. In general, we need to have the sensitivity and the
 * specificity of a test result in order to perform a likelihood ratio test. The numerical value of the test
 * (in this case, 48) is not important.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.3.5 (2018-10-28)
 */
public class TestResult implements Comparable<TestResult> {
    private static final Logger logger = LogManager.getLogger();
    /**
     * A list of results for the tests performed in a {@link HpoCase} case.
     * To save space, we only record the result of the test, and we assume that the order of the test is the same
     * as indicated in the case object. In the intended use case, there will be one test result for each disease
     * that is tested for the {@link HpoCase} object.
     */
    private final List<Double> results;
    /** Result of the likelhood ratio test for the genotype. */
    private final Double genotypeLR;
    /** The id of the gene associated with ths disease being tested here. */
    private TermId entrezGeneId=null;
    /** This is the product of the individual test results. */
    private final double compositeLR;
    /** Reference to the the disease that we are testing (e.g., OMIM:600100).*/
    private final HpoDisease hpoDisease;
    /** The probability of some result before the first test is done.*/
    private final double pretestProbability;
    /** The probability of some result before the first test is done.*/
    private final double posttestProbability;
    /** The overall rank of the the result withint the differential diagnosis. */
    private int rank;

    /**
     * The constructor initializes the variables and calculates {@link #compositeLR}
     *
     * @param reslist list of individual test results
     * @param disease    name of the disease being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(List<Double> reslist, HpoDisease disease, double pretest) {
        results = reslist;
        hpoDisease = disease;
        this.pretestProbability = pretest;
        // the composite LR is the product of the individual LR's
        compositeLR=reslist.stream().reduce(1.0, (a, b) -> a * b);
        this.genotypeLR=null;// result without genotype.
        posttestProbability=getPosttestProbability();
    }

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     * @param reslist list of individual test results
     * @param diseaseId name of the disease being tested
     * @param genotypeLr LR result for the genotype
     * @param geneId gene id of the gene being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(List<Double> reslist, HpoDisease diseaseId, Double genotypeLr,TermId geneId,double pretest) {
        results = reslist;
        hpoDisease = diseaseId;
        this.pretestProbability = pretest;
        this.genotypeLR=genotypeLr;
        this.entrezGeneId=geneId;
        // the composite ratio is equal to the product of the phenotype LR's
        // multiplied by the genotype LR.
        compositeLR=reslist.stream().reduce(1.0, (a, b) -> a * b) * genotypeLr;
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
     * @param i index of the test we are interested in
     * @return the likelihood ratio of the i'th test
     */
    public double getRatio(int i) {
        return this.results.get(i);
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
}
