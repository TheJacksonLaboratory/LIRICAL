package org.monarchinitiative.lr2pg.hpo;



import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.likelihoodratio.LrEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.*;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.xml.sax.ext.Locator2;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;

/**
 * A simulator that simulates cases from the {@link HpoDisease} objects by choosing a subset of terms
 * and adding noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class HpoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** An object representing the Human Phenotype Ontology */
    private HpoOntology ontology =null;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private BackgroundForegroundTermFrequency bftfrequency;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** The file name of the HPO ontology file. */
    private static final String HP_OBO="hp.obo";
    /** The file name of the HPO annotation file. */
    private static final String HP_PHENOTYPE_ANNOTATION="phenotype.hpoa";
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private static Map<String,HpoDisease> diseaseMap;
    /** Object to evaluate the results of differential diagnosis by LR analysis. */
    private LrEvaluator evaluator;
    /** Number of HPO terms to use for each simulated case. */
    private final int n_terms_per_case;
    /** Number of "noise" (unrelated) HPO terms to use for each simulated case. */
    private final int n_noise_terms;
    /** Number of cases to simulate. */
    private final int n_cases_to_simulate;


    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = ImmutableTermId.constructWithPrefix("HP:0000118");

   /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}.
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public HpoCaseSimulator(String datadir, int cases_to_simulate, int terms_per_case, int noise_terms ) {

        this.n_cases_to_simulate=cases_to_simulate;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
        inputHpoOntologyAndAnnotations(datadir);
        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
    }


    public void simulateCases() throws Lr2pgException {
        int c=0;
        Map<Integer,Integer> ranks=new HashMap<>();
        logger.trace(String.format("Will simulate %d diseases.",diseaseMap.size() ));
        logger.trace("Simulating n={} HPO cases with {} random terms and {} noise terms per case.",n_cases_to_simulate,n_terms_per_case,n_noise_terms);
        for (String diseasename : diseaseMap.keySet()) {
            HpoDisease disease = diseaseMap.get(diseasename);
            //logger.trace("Simulating disease "+diseasename);
            if (disease.getNumberOfPhenotypeAnnotations() == 0) {
                logger.trace(String.format("Skipping disease %s [%s] because it has no phenotypic annotations",
                        disease.getName(),
                        disease.getDiseaseDatabaseId()));
                continue;
            }
            int rank = simulateCase(disease);
            if (!ranks.containsKey(rank)) {
                ranks.put(rank, 0);
            }
            ranks.put(rank, ranks.get(rank) + 1);
            if (++c>n_cases_to_simulate)break;
            if (c%100==0) {logger.trace("Simulating case " + c); }
        }
        int N=n_cases_to_simulate;
        int rank11_20=0;
        int rank21_30=0;
        int rank31_100=0;
        int rank101_up=0;
        for (int r:ranks.keySet()) {
            if (r<11) {
                System.out.println(String.format("Rank=%d: count:%d (%.1f%%)", r, ranks.get(r), 100.0 * ranks.get(r) / N));
            } else if (r<21) {
                rank11_20+=ranks.get(r);
            } else if (r<31) {
                rank21_30+=ranks.get(r);
            } else if (r<101) {
                rank31_100+=ranks.get(r);
            } else {
                rank101_up+=ranks.get(r);
            }
        }
        System.out.println(String.format("Rank=11-20: count:%d (%.1f%%)", rank11_20, (double) 100* rank11_20 / N));
        System.out.println(String.format("Rank=21-30: count:%d (%.1f%%)", rank21_30, (double) 100 * rank21_30 / N));
        System.out.println(String.format("Rank=31-100: count:%d (%.1f%%)", rank31_100, (double) 100 * rank31_100 / N));
        System.out.println(String.format("Rank=101-...: count:%d (%.1f%%)", rank101_up, (double) 100 * rank101_up / N));
    }


    /**
     * This is a term that was observed in the simulated patient (note that it should not be a HpoTermId, which
     * contains metadata about the term in a disease entity, such as overall frequency. Instead, we are simulating an
     * individual patient and this is a definite observation.
     * @return a random term from the phenotype subontology.
     */
    private TermId getRandomPhenotypeTerm() {
        int n=phenotypeterms.size();
        int r = (int)Math.floor(n*Math.random());
        return phenotypeterms.get(r);
    }


    private HpoCase createSimulatedCase(HpoDisease disease) throws Lr2pgException {
        if (disease==null) {
            throw new Lr2pgException("Attempt to create case from Null-value for disease");
        }
        int n_terms=Math.min(disease.getNumberOfPhenotypeAnnotations(),n_terms_per_case);
        int n_random=Math.min(n_terms, n_noise_terms);// do not take more random than real terms.
        logger.trace("Create simulated case with n_terms="+n_terms + ", n_random="+n_random);
        // the creation of a new ArrayList is needed because disease returns an immutable list.
        List<HpoAnnotation> abnormalities = new ArrayList<>(disease.getPhenotypicAbnormalities());
        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        Collections.shuffle(abnormalities); // randomize order of phenotypes
        // take the first n_random terms of the randomized list
        abnormalities.stream().limit(n_terms).forEach(a-> termIdBuilder.add(a.getTermId()));
        // now add n_random "noise" terms to the list of abnormalities of our case.
        for(int i=0;i<n_random;i++){
            TermId t = getRandomPhenotypeTerm();
            termIdBuilder.add(t);
        }
        ImmutableList<TermId> termlist = termIdBuilder.build();
        return new HpoCase.Builder(termlist).build();
    }


    public TestResult getResults(HpoDisease disease) throws Lr2pgException {
        if (this.evaluator==null) {
            int rank = simulateCase(disease);
            System.err.println(String.format("Rank for %s was %d",disease.getName(),rank));
        }
        return evaluator.getResult(disease);
    }

    public HpoDisease name2disease(String diseasename) {
        return diseaseMap.get(diseasename);
    }


    public int simulateCase(HpoDisease disease) throws Lr2pgException {
        HpoCase hpocase2 = createSimulatedCase(disease);
        List<HpoDisease> diseaselist = new ArrayList<>(diseaseMap.values());
        // the following evaluates the case for each disease with equal pretest probabilities.
        this.evaluator = new LrEvaluator(hpocase2, diseaselist,ontology,bftfrequency);
        evaluator.evaluate();
        return evaluator.getRank(disease);
    }




    public void debugPrint() {
        logger.trace(String.format("Got %d terms and %d diseases",ontology.getAllTermIds().size(),
                diseaseMap.size()));
    }


    private void inputHpoOntologyAndAnnotations(String datadir)  {
        String hpopath=String.format("%s%s%s",datadir, File.separator,HP_OBO);
        String annotationpath=String.format("%s%s%s",datadir,File.separator,HP_PHENOTYPE_ANNOTATION);
        HpoOboParser parser = new HpoOboParser(new File(hpopath));
        try {
            this.ontology = parser.parse();
        } catch (IOException ioe) {
            System.err.println("Could not parse hp.obo file: " + ioe.getMessage());
            throw new RuntimeException("Could not parse hp.obo file: " + ioe.getMessage());
        }
        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationpath,ontology);
        try {
            diseaseMap = annotationParser.parse();
            if (! annotationParser.validParse()) {
                logger.error("Warning -- parse problems encountered with the annotation file at {}.", annotationpath);
                for (String error: annotationParser.getErrors()) {
                    logger.error(error);
                }
            }
        } catch (PhenolException pe) {
            throw new RuntimeException("Could not parse annotation file: "+pe.getMessage());
        }
        bftfrequency=new BackgroundForegroundTermFrequency(ontology,diseaseMap);
    }
}
