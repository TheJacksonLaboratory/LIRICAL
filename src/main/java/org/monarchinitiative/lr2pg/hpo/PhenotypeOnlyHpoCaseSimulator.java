package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.HpoDataIngestor;
import org.monarchinitiative.lr2pg.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * A simulator that simulates cases from the {@link HpoDisease} objects by choosing a subset of terms
 * and adding noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class PhenotypeOnlyHpoCaseSimulator {
    private static final Logger logger = LogManager.getLogger();
    /** An object representing the Human Phenotype Ontology */
    private HpoOntology ontology;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private final Map<TermId,HpoDisease> diseaseMap;
    /** Number of HPO terms to use for each simulated case. */
    private final int n_terms_per_case;
    /** Number of "noise" (unrelated) HPO terms to use for each simulated case. */
    private final int n_noise_terms;
    /** Number of cases to simulate. */
    private final int n_cases_to_simulate;
    /** If true, we exchange each of the non-noise terms with a direct parent except if that would mean going to
     * the root of the phenotype ontology.
     */
    private boolean addTermImprecision = false;
    /** The proportion of cases at rank 1 in the current simulation */
    private double proportionAtRank1=0.0;

    private HpoCase currentCase;


    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.constructWithPrefix("HP:0000118");

   /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}.
     * param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
   @Autowired
    public PhenotypeOnlyHpoCaseSimulator(HpoOntology ontology,
                                         Map<TermId,HpoDisease> diseaseMap,
                                         int cases_to_simulate, int terms_per_case, int noise_terms ) {

        this.n_cases_to_simulate=cases_to_simulate;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
        //HpoDataIngestor ingestor = new HpoDataIngestor(datadir);
        this.ontology=ontology;//ingestor.getOntology();
        this.diseaseMap=diseaseMap;//ingestor.getDiseaseMap();
        this.phenotypeLrEvaluator = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
    }

    @Autowired
    public PhenotypeOnlyHpoCaseSimulator(HpoOntology ontology,
                                         Map<TermId,HpoDisease> diseaseMap, int cases_to_simulate, int terms_per_case, int noise_terms, boolean imprecise ) {
        this(ontology,diseaseMap,cases_to_simulate,terms_per_case,noise_terms);
        this.addTermImprecision=imprecise;
    }


    public double getProportionAtRank1() {
        return proportionAtRank1;
    }

    /** This will run simulations according to the parameters {@link #n_cases_to_simulate},
     * {@link #n_terms_per_case} and {@link #n_noise_terms}.
     * @throws Lr2pgException
     */
    public void simulateCases() throws Lr2pgException {
        int c=0;
        Map<Integer,Integer> ranks=new HashMap<>();
        logger.trace(String.format("Will simulate %d diseases.",diseaseMap.size() ));
        logger.trace("Simulating n={} HPO cases with {} random terms and {} noise terms per case.",n_cases_to_simulate,n_terms_per_case,n_noise_terms);
        for (TermId diseaseCurie : diseaseMap.keySet()) {
            HpoDisease disease = diseaseMap.get(diseaseCurie);
            //logger.trace("Simulating disease "+diseasename);
            if (disease.getNumberOfPhenotypeAnnotations() == 0) {
                logger.trace(String.format("Skipping disease %s [%s] because it has no phenotypic annotations",
                        disease.getName(),
                        disease.getDiseaseDatabaseId()));
                continue;
            }
            int rank = simulateCase(disease);
            ranks.putIfAbsent(rank,0);
            ranks.put(rank, ranks.get(rank) + 1);
            if (++c>n_cases_to_simulate) {
                break; // finished!
            }
            if (c%100==0) {logger.trace("Simulating case " + c); }
        }
        int N=n_cases_to_simulate;
        int rank11_20=0;
        int rank21_30=0;
        int rank31_100=0;
        int rank101_up=0;
        for (int r:ranks.keySet()) {
            if (r==1) {
                proportionAtRank1=ranks.get(r) / (double)N;
            }
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

    /** @return a random parent of term tid. */
    private TermId getRandomParentTerm(TermId tid) {
        Set<TermId> parents = getParentTerms(ontology,tid,false);
        int r = (int)Math.floor(parents.size()*Math.random());
        int i=0;
        return (TermId)parents.toArray()[r];
    }



    public HpoOntology getOntology() {
        return ontology;
    }

    /**
     * This creates a simulated, phenotype-only case based on our annotations for the disease
     * @param disease Disease for which we will simulate the case
     * @return HpoCase object with a randomized selection of phenotypes from the disease
     */
    private List<TermId> getRandomTermsFromDisease(HpoDisease disease) {
        int n_terms=Math.min(disease.getNumberOfPhenotypeAnnotations(),n_terms_per_case);
        int n_random=Math.min(n_terms, n_noise_terms);// do not take more random than real terms.
        logger.trace("Creating simulated case with n_terms="+n_terms + ", n_random="+n_random);
        // the creation of a new ArrayList is needed because disease returns an immutable list.
        List<HpoAnnotation> abnormalities = new ArrayList<>(disease.getPhenotypicAbnormalities());
        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        Collections.shuffle(abnormalities); // randomize order of phenotypes
        // take the first n_random terms of the randomized list
        abnormalities.stream().limit(n_terms).forEach(a-> termIdBuilder.add(a.getTermId()));
        // now add n_random "noise" terms to the list of abnormalities of our case.
        for(int i=0;i<n_random;i++){
            TermId t = getRandomPhenotypeTerm();
            if (addTermImprecision) {
                t = getRandomParentTerm(t);
            }
            termIdBuilder.add(t);
        }
        return termIdBuilder.build();
    }



    /**
     * @param diseaseCurie a term id for a disease id such as OMIM:600100
     * @return the corresponding {@link HpoDisease} object.
     */
    public HpoDisease name2disease(TermId diseaseCurie) {
        return diseaseMap.get(diseaseCurie);
    }


    public HpoCase getCurrentCase() {
        return currentCase;
    }

    public int simulateCase(HpoDisease disease) throws Lr2pgException {
        if (disease == null) {
            // should never happen!
            throw new Lr2pgException("Attempt to create case from Null-value for disease");
        }
        List<TermId> randomizedTerms = getRandomTermsFromDisease(disease);

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(randomizedTerms)
                .ontology(this.ontology)
                .diseaseMap(diseaseMap)
                .phenotypeLr(this.phenotypeLrEvaluator);
        // the following evaluates the case for each disease with equal pretest probabilities.
        /** Object to evaluate the results of differential diagnosis by LR analysis. */
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        HpoCase hpocase = evaluator.evaluate();
        System.err.println(hpocase.toString());
        return hpocase.getRank(disease.getDiseaseDatabaseId());
    }




    public void debugPrint() {
        String.format("Got %d terms and %d diseases",ontology.getAllTermIds().size(),
                diseaseMap.size());
    }




}
