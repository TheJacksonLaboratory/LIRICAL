package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Represents a single case and the HPO terms assigned to the case (patient), as well as the results of the
 * likelihood ratio analysis.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2018-04-04)
 */
public final class HpoCase {
    private static final Logger logger = LogManager.getLogger();
    /** List of Hpo terms for our case. */
    private final List<TermId> observedAbnormalities;
    /** List of excluded Hpo terms for our case. */
    private final List<TermId> excludedAbnormalities;
    /** One of Male, Female, Unknown. See {@link Sex}. */
    private final Sex sex;
    /** Age of the proband, if known. */
    private final Age age;

    private final Map<TermId,TestResult> disease2resultMap;

    private HpoCase(List<TermId> observedAbn,  List<TermId> excludedAbn, Map<TermId,TestResult> d2rmap, Sex sex, Age age) {
        this.observedAbnormalities=observedAbn;
        this.excludedAbnormalities=excludedAbn;
        this.disease2resultMap=d2rmap;
        this.sex=sex;
        this.age=age;
    }


    /** @return A list of the HPO terms representing the phenotypic abnormalities in the person being evaluated.*/
    public List<TermId> getObservedAbnormalities() { return observedAbnormalities;  }
    /** @return A list of the HPO terms representing abnormalities that were excluded in the person being evaluated.*/
    public List<TermId> getExcludedAbnormalities() {  return excludedAbnormalities;  }
    /** @return the sex of the person being evaluated. */
    public Sex getSex() { return sex;  }
    /** The {@link Age} of the person being evaluated.*/
    public Age getAge() { return age; }
    /** @return Sort List of {@link TestResult} objects for each diseases in the differential diagnosis. */
    public List<TestResult> getResults() {
        List<TestResult> trlist = new ArrayList<>(this.disease2resultMap.values());
        trlist.sort(Collections.reverseOrder());
        return trlist;
    }
    /** * @return total number of positive and negative phenotype observations for this case.*/
    public int getNumberOfObservations() {
        return observedAbnormalities.size() + excludedAbnormalities.size();
    }

    public TestResult getResult(TermId diseaseId) {
        return this.disease2resultMap.get(diseaseId);
    }

    /**
     * @param diseaseId CURIE (e.g., OMIM:600100) of the disease whose rank we want to know
     * @return the rank of the disease within all of the test results
     */
    public int getRank(TermId diseaseId){
        TestResult result = this.disease2resultMap.get(diseaseId);
        return result==null?Integer.MAX_VALUE : result.getRank();
    }

    /** Output the results for a specific HPO disease.
     * This is ugly and just for development. */
    public void outputLrToShell(TermId diseaseId, HpoOntology ontology,Map<TermId, Gene2Genotype> g2gmap) {
        int rank = getRank(diseaseId);

        TestResult r = getResult(diseaseId);
        String diseaseName=r.getDiseaseName();
        int idx = diseaseName.indexOf(';');
        if (idx>0)
            diseaseName=diseaseName.substring(0,idx);
        System.err.println(String.format("%s[%s]: rank=%d",diseaseName,diseaseId.getIdWithPrefix(), rank));
        DecimalFormat df = new DecimalFormat("0.000E0");
        System.err.println(String.format("Pretest probability: %s; Composite LR: %.2f; Posttest probability: %s ",
                niceFormat(r.getPretestProbability()),
                r.getCompositeLR(),
                niceFormat(r.getPosttestProbability())));
        for (int i = 0; i < r.getNumberOfTests(); i++) {
            double ratio = r.getRatio(i);
            TermId tid = getObservedAbnormalities().get(i);
            String term = String.format("%s [%s]", ontology.getTermMap().get(tid).getName(), tid.getIdWithPrefix());
            System.err.println(String.format("%s: ratio=%s", term, niceFormat(ratio)));
        }
        if (r.hasGenotype()) {
            Gene2Genotype g2g= g2gmap.get(r.getEntrezGeneId());
            if (g2g==null) {
                // TODO check this--why do we have a genotype result if there is no genotype?
                // is this for diseases with a gene but we found no variant?
                System.err.println(String.format("Genotype LR for %s: %f",r.getEntrezGeneId().getIdWithPrefix(), r.getGenotypeLR()));
                System.err.println("No variants found in VCF");
                return;
            }
            System.err.println(String.format("Genotype LR for %s[%s]: %f",g2g.getSymbol(), r.getEntrezGeneId().getIdWithPrefix(), r.getGenotypeLR()));
            System.err.println(g2g);
        } else {
            System.err.println("No genotype used to calculated");
        }
        System.err.println();
    }

    /**
     * Ootputs the top n results to the shell
     * @param n number of top results to output.
     */
    public void outputTopResults(int n, HpoOntology ontology, Map<TermId, Gene2Genotype> g2gmap) {
        List<TestResult> resultlist = new ArrayList<>(this.disease2resultMap.values());
        resultlist.sort(Collections.reverseOrder());
        int i=0;
        while (i<n && i<resultlist.size()) {
            TestResult tres = resultlist.get(i);
            TermId diseaseId = tres.getDiseaseCurie();
            outputLrToShell(diseaseId,ontology,g2gmap);
            i++;
        }
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

    @Override
    public String toString() {
        String observed=this.observedAbnormalities.
                stream().
                map(TermId::getIdWithPrefix).
                collect(Collectors.joining("; "));
        String excluded=this.excludedAbnormalities.stream().
                map(TermId::getIdWithPrefix).
                collect(Collectors.joining("; "));
        int n_results=this.getResults().size();
        return "HPO Case\n" + "observed: " + observed +"\nexcluded: " + excluded +"\nTests: n="+n_results;

    }


    /** Convenience class to construct an {@link HpoCase} object. */
    public static class Builder {
        /** List of Hpo terms for our case. */
        private final List<TermId> observedAbnormalities;
        /** List of excluded Hpo terms for our case. */
        private List<TermId> excludedAbnormalities;
        /** List of results . */
        private Map<TermId,TestResult> testResultMap;
        /** One of Male, Female, Unknown. See {@link Sex}. */
        private Sex sex;
        /** Age of the proband, if known. */
        private Age age;

        public Builder(List<TermId> abnormalPhenotypes) {
            this.observedAbnormalities = ImmutableList.copyOf(abnormalPhenotypes);
            excludedAbnormalities=ImmutableList.of(); // default empty list
            sex=Sex.UNKNOWN;
            age=Age.ageNotKnown();
        }

        public Builder excluded(List<TermId> excludedPhenotypes) {
            this.excludedAbnormalities = ImmutableList.copyOf(excludedPhenotypes);
            return this;
        }

        public Builder sex(Sex s) {
            this.sex=s;
            return this;
        }

        public Builder age(Age a) {
            this.age=a;
            return this;
        }

        public Builder results(Map<TermId,TestResult> trlist) {
            this.testResultMap =trlist;
            return this;
        }

        public HpoCase build() {
            Objects.requireNonNull(testResultMap);
            return new HpoCase(observedAbnormalities,excludedAbnormalities, testResultMap,sex,age);
        }
    }

}
