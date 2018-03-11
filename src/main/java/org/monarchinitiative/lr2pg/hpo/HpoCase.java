package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.formats.hpo.HpoTerm;
import org.monarchinitiative.phenol.formats.hpo.TermIdWithMetadata;


import java.util.*;


/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.1 (2017-11-24)
 */
public class HpoCase {
    private static final Logger logger = LogManager.getLogger();
    /** The {@link Disease2TermFrequency} has data on each disease. Within this class, we iterate over each disease
     * in order to get the overall likelihood ratio for the diagnosis*/
    private Disease2TermFrequency disease2TermFrequencyMap=null;

    private String disease=null;
    /** List of Hpo terms for our case. TODO add negative annotations. */
    private List<TermIdWithMetadata> observedAbnormalities;

    private final HpoOntology hpoOntology;
    /** a set of test results -- the evaluation of each HPO term for the disease. */
    private List<TestResult> results;



    public HpoCase(HpoOntology ontol,
                   Disease2TermFrequency diseaseFreqMap,
                   String diseaseNane,
                   List<TermIdWithMetadata> observedAbn) {
        this.disease2TermFrequencyMap=diseaseFreqMap;
        this.hpoOntology=ontol;
        this.disease=diseaseNane;
        this.observedAbnormalities=observedAbn;
        results=new ArrayList<>();
    }

    /**
     * Calculate the likelihood ratio for the diagnosis of each disease in {@link #disease2TermFrequencyMap}
     * given the observed phenotypic abnormalities in {@link #observedAbnormalities}. Place the results into
     * {@link #results}.
     */
    public void calculateLikelihoodRatios() throws Lr2pgException {
        Iterator<String> it = disease2TermFrequencyMap.getDiseaseNameIterator();
        while (it.hasNext()) {
            String diseasename = it.next();
            ImmutableList.Builder builder = new ImmutableList.Builder();
            for (TermIdWithMetadata tid : this.observedAbnormalities) {
                double LR = disease2TermFrequencyMap.getLikelihoodRatio(tid,diseasename);
                builder.add(LR);
            }
            TestResult result = new TestResult(builder.build(),diseasename);
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
       Collections.sort(results,Collections.reverseOrder());

        int rank=0;
        for (TestResult r: results){
            rank++;
            if (r.getDiseasename().equals(diseasename)) { return rank; }
        }
        return rank;

    }

    public void outputResults() {
        Collections.sort(results,Collections.reverseOrder());
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



}
