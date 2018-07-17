package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.stream.Collectors;

/**
 * This class organizes information about the result of a test. For instance,  a GDx VCC test for
 * glaucoma may result in a measurement of 48, a value that is known to
 * have a 60% sensitivity and 97% specificity for glaucoma. In general, we need to have the sensitivity and the
 * specificity of a test result in order to perform a likelihood ratio test. The numerical value of the test
 * (in this case, 48) is not important.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @author <a href="mailto:vida.ravanmehr@jax.org">Vida Ravanmehr</a>
 * @version 0.3.4 (2018-06-18)
 */
public class TestResult implements Comparable<TestResult> {
    private static final Logger logger = LogManager.getLogger();
    /**
     * A list of results for the tests performed in a {@link HpoCase} case.
     * To save space, we only record the result of the test, and we assume that the order of the test is the same
     * as indicated in the case object. In the intended use case, there will be one test result for each disease
     * that is tested for the {@link HpoCase} object.
     */
    private final ImmutableList<Double> results;
    /** Result of the lieklhood ratio test for the genotype. */
    private Double genotypeLR=null;
    /** The id of the gene associated with ths disease being tested here. */
    private TermId entrezGeneId=null;
    /** This is the product of the individual test results. */
    private final double compositeLR;
    /** The CURIE of the disease that we are testing (e.g., OMIM:600100).*/
    private final TermId diseaseCurie;
    /** The probability of some result before the first test is done.*/
    private final double pretestProbability;
    /** The overall rank of the the result withint the differential diagnosis. */
    private int rank;

    /**
     * The constructor initializes the variables and calculates {@link #compositeLR}
     *
     * @param reslist list of individual test results
     * @param diseaseId    name of the disease being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(ImmutableList<Double> reslist, TermId diseaseId, double pretest) {
        results = reslist;
        diseaseCurie = diseaseId;
        this.pretestProbability = pretest;
        // the composite LR is the product of the individual LR's
        compositeLR=reslist.stream().reduce(1.0, (a, b) -> a * b);
    }

    /**
     * This constructor should be used if we have a genotype for this gene/disease.
     * @param reslist list of individual test results
     * @param diseaseId name of the disease being tested
     * @param genotypeLr LR result for the genotype
     * @param geneId gene id of the gene being tested
     * @param pretest pretest probability of the disease
     */
    public TestResult(ImmutableList<Double> reslist, TermId diseaseId, Double genotypeLr,TermId geneId,double pretest) {
        results = reslist;
        diseaseCurie = diseaseId;
        this.pretestProbability = pretest;
        this.genotypeLR=genotypeLr;
        this.entrezGeneId=geneId;
        // the composite ratio is equal to the product of the phenotype LR's
        // multiplied by the genotype LR.
        compositeLR=reslist.stream().reduce(1.0, (a, b) -> a * b) * genotypeLr;
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
    public int compareTo(TestResult other) {
        return Double.compare(compositeLR, other.compositeLR);
    }


    @Override
    public String toString() {
        String resultlist = results.stream().map(String::valueOf).collect(Collectors.joining(";"));
        return String.format("%s: %.2f [%s]", diseaseCurie, getCompositeLR(), resultlist);
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
        return diseaseCurie;
    }

    public boolean hasGenotype(){ return genotypeLR!=null;}

    public Double getGenotypeLR() {
        return genotypeLR;
    }

    public TermId getEntrezGeneId() {
        return entrezGeneId;
    }
}
