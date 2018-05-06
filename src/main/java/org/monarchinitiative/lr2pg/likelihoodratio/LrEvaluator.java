package org.monarchinitiative.lr2pg.likelihoodratio;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.BackgroundForegroundTermFrequency;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Likelihood ratio evaluator. This class coordinates the performance of the likelihood ratio test on
 * an {@link HpoCase}.
 */
public class LrEvaluator {

    private final HpoCase hpocase;
    private final List<HpoDisease> diseaselist;
    private final List<Double> pretestProbabilities;
    private final BackgroundForegroundTermFrequency bftfrequency;
    /** Reference to the Human Phenotype Ontology object. */
    private final HpoOntology ontology;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private final List<TestResult> results;


    public LrEvaluator(HpoCase hpcase, List<HpoDisease> diseases, HpoOntology ont,BackgroundForegroundTermFrequency bftfrequency) {
        this.hpocase=hpcase;
        this.diseaselist=diseases;
        this.bftfrequency=bftfrequency;

        // initialize to all equal pretest probabilities.
        int n=diseaselist.size();
        this.pretestProbabilities=new ArrayList<>(n);
        double prob=1.0/(double)n;
        for (int i=0;i<n;i++) { pretestProbabilities.add(i,prob); }
        this.ontology=ont;
        results=new ArrayList<>();
    }


    /** This method evaluates the likilihood ratio for each disease in
     * {@link #diseaselist}. After this, it sorts the results (the best hit is then at index 0, etc).
     */
    public void evaluate()  {
        assert diseaselist.size()==pretestProbabilities.size();
        for (int i=0;i<diseaselist.size();i++) {
            HpoDisease disease = diseaselist.get(i);
            double pretest = pretestProbabilities.get(i);
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.hpocase.getObservedAbnormalities()) {
                double LR = bftfrequency.getLikelihoodRatio(tid,disease);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder.build(),disease.getName(),pretest);
            results.add(result);
        }
        results.sort(Collections.reverseOrder());
    }

    /**
     * This method sorts all of the results in {@link #results}. The best results are the highest, and so
     * we sort in descending order. We return the rank of the item
     * @param disease the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(HpoDisease disease){
        int rank=0;
        for (TestResult r: results){
            rank++;
            if (r.getDiseasename().equals(disease.getName())) {
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
