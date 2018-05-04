package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.*;


/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2018-04-04)
 */
public class HpoCaseOld {
    private static final Logger logger = LogManager.getLogger();
    /** The {@link BackgroundForegroundTermFrequency} has data on each disease. Within this class, we iterate over each disease
     * in order to get the overall likelihood ratio for the diagnosis*/
    private final BackgroundForegroundTermFrequency bftFrequency;
    /** For some simulations, we know what the correct disease diagnosis is; if so, it is recorded here. */
    private final String disease;
    /** Reference to the Human Phenotype Ontology object. */
    private HpoOntology ontology =null;
    /** List of Hpo terms for our case. TODO add negative annotations. */
    private final List<TermId> observedAbnormalities;
    /** Map of all diseases we are using for differential diagnosis./ Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private final Map<String,HpoDisease> diseaseMap;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private final List<TestResult> results;



    public HpoCaseOld(BackgroundForegroundTermFrequency diseaseFreqMap,
                      String diseaseNane,
                      List<TermId> observedAbn,
                      Map<String,HpoDisease> disMap,
                      HpoOntology ont) {
        this.bftFrequency =diseaseFreqMap;
        this.disease=diseaseNane;
        this.observedAbnormalities=observedAbn;
        this.diseaseMap=disMap;
        this.results=new ArrayList<>();
        this.ontology=ont;
    }

    /**
     * Calculate the likelihood ratio for the diagnosis of each disease in {@link #bftFrequency}
     * given the observed phenotypic abnormalities in {@link #observedAbnormalities}. Place the results into
     * {@link #results}.
     */
    public void calculateLikelihoodRatios() throws Lr2pgException {
        for (String diseaseName: diseaseMap.keySet()) {
            HpoDisease disease = diseaseMap.get(diseaseName);
            ImmutableList.Builder<Double> builder = new ImmutableList.Builder<>();
            for (TermId tid : this.observedAbnormalities) {
                double LR = bftFrequency.getLikelihoodRatio(tid,disease);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder.build(),diseaseName);
            results.add(result);
        }
    }
    /** @return the total number of tests performed. */
    public int getTotalResultCount() {
        return results.size();
    }

    /**
     * This method sorts all of the results in {@link #results}. The best results are the highest, and so
     * we sort in descending order. We return the rank of the item
     * @param diseasename name of the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(String diseasename){
        results.sort(Collections.reverseOrder());

        int rank=0;
        for (TestResult r: results){
            rank++;
            if (r.getDiseasename().equals(diseasename)) {
                //outputResults();
                outputLR(r,diseasename, rank);
                return rank;
            }
        }
        return rank;
    }


    private void outputLR(TestResult r, String diseasename, int rank) {
        System.err.println("Likelihood ratios for " + diseasename + "\tRank="+rank);
        for (int i=0;i<r.getNumberOfTests();i++) {
            double ratio = r.getRatio(i);
            TermId tid =observedAbnormalities.get(i);
            String term = String.format("%s [%s]",ontology.getTermMap().get(tid).getName(),tid.getIdWithPrefix() );
            System.err.println(term + ": ratio="+ratio);
        }
        System.err.println();
    }

    public void outputResults() {
        results.sort(Collections.reverseOrder());
       // Collections.sort(results,);
        int rank=0;
        for (TestResult r: results){
            rank++;
            System.out.println(rank + ") "+ r.toString());
        }
    }


    /** @return Number of annotations (i.e., HPO Terms) that were observed in this case. */
    public int getNumberOfAnnotations() {
        return observedAbnormalities.size();
    }


    public void debugPrint() {
        System.out.println("Observed abnormalities:");
        for (TermId tid : observedAbnormalities) {
            System.out.println("\t" + tid.getIdWithPrefix());
        }
    }



}
