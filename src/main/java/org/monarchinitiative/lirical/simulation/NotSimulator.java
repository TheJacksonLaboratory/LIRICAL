package org.monarchinitiative.lirical.simulation;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * This classes simulates cases with and without "NOT" annotations and examines the effect of NOT annotations
 * on differential diagnosis
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class NotSimulator {
    private static final Logger logger = LoggerFactory.getLogger(NotSimulator.class);
    /** An object representing the Human Phenotype Ontology */
    private Ontology ontology;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private final Map<TermId, HpoDisease> diseaseMap;
    /** If true, we exchange each of the non-noise terms with a direct parent except if that would mean going to
     * the root of the phenotype ontology.*/
    private boolean addTermImprecision = false;
    /** The proportion of cases at rank 1 in the current simulation */
    private double proportionAtRank1=0.0;
    /** This array will hold the TermIds from the disease map in order -- this will allow us to
     * get random indices for the simulations. */
    private TermId[] termIndices;
    /** If true, show lots of results in STDOUT while we are calculating. */
    private boolean verbose=true;
    /** Root term id in the phenotypic abnormality subontology. */
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    /** Number of HPO terms to use for each simulated case. */
    private int n_terms_per_case;
    /** Number of "noise" (unrelated) HPO terms to use for each simulated case. */
    private int n_noise_terms;
    /** Number of cases to simulate. */
    private int n_cases_to_simulate;
    private List<NotAnnotationDifferential> caselist;


    /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}. This
     * constructor sets "imprecision" to false.
     * @param ontology reference to HPO Ontology object
     * @param diseaseMap Map containing (usuallu) all diseases in the corpus
     */
    public NotSimulator(Ontology ontology,
                        Map<TermId,HpoDisease> diseaseMap) {

        this.ontology = ontology;
        this.diseaseMap = diseaseMap;
        this.phenotypeLrEvaluator = new PhenotypeLikelihoodRatio(ontology, diseaseMap);
        Set<TermId> descendents = getDescendents(ontology, PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
        this.termIndices=diseaseMap.keySet().toArray(new TermId[0]);
        initializeCaseList();
    }

    /**
     * Represents a pair of diseases where one disease has a certain HPO term and the
     * other disease is explicitly annotated NOT to have that HPO term.
     */
    public static class NotAnnotationDifferential {
        /** disease id., e.g., OMIM:600123, of a disease that is annotated to {@link #hpoId} .*/
        public final TermId diseaseIdWithTermAnnotated;
        /** disease id., e.g., OMIM:600125, of a disease for which {@link #hpoId} is excluded.*/
        public final TermId diseaseIdWithTermExcluded;
        /** Term id of an HPO term. */
        public final TermId hpoId;

        public NotAnnotationDifferential(TermId disease1, TermId disease2, TermId hpo) {
            this.diseaseIdWithTermAnnotated = disease1;
            this.diseaseIdWithTermExcluded  = disease2;
            this.hpoId = hpo;
        }
    }


    private void initializeCaseList() {
        caselist = new ArrayList<>();
        // Marfan vs Loes sDietz Syndrome 4.
        TermId marfan = TermId.of("OMIM:154700");
        TermId lds4 = TermId.of("OMIM:614816");
        TermId ectopiaLentis = TermId.of("HP:0001083");
        NotAnnotationDifferential nad1 = new NotAnnotationDifferential(marfan,lds4,ectopiaLentis);
        caselist.add(nad1);
    }


    private void runSimulation(NotAnnotationDifferential nad) {
        TermId diseaseIdWithTermAnnotated = nad.diseaseIdWithTermAnnotated;
        TermId diseaseIdWithTermExcluded = nad.diseaseIdWithTermExcluded;
        TermId hpo = nad.hpoId;
        HpoDisease diseaseWithTermAnnotated = this.diseaseMap.get(diseaseIdWithTermAnnotated);
        HpoDisease diseaseWithTermExcluded = this.diseaseMap.get(diseaseIdWithTermExcluded);
        try {
            HpoCase hcase = simulateCase(diseaseWithTermExcluded);
            HpoCase hcaseX = simulateCaseWithExcludedTerm(diseaseWithTermExcluded,hpo);
            double denominator1 = hcase.getResult(diseaseIdWithTermAnnotated).getPosttestProbability();
            double numerator1 = hcase.getResult(diseaseIdWithTermExcluded).getPosttestProbability();
            double ratio1 = numerator1 - denominator1;
            double denominator2 = hcaseX.getResult(diseaseIdWithTermAnnotated).getPosttestProbability();
            double numerator2 = hcaseX.getResult(diseaseIdWithTermExcluded).getPosttestProbability();
            double ratio2 = numerator2 - denominator2;
            // we expect that diseaseWithTermExcluded will get the better score.
            // there should be an improvement with using the negated term
            System.out.println(String.format("ratio2 = %.1f  ratio1 = %.1f", ratio2, ratio1));

        } catch(LiricalException e) {
            e.printStackTrace();
        }

    }



    public void runSimulations(int n) {
        this.n_cases_to_simulate = n;
        n_terms_per_case = 5;
        n_noise_terms = 3;
        addTermImprecision = true;
        for (NotAnnotationDifferential nad : this.caselist) {
            runSimulation(nad);
        }
    }

    /** @return a non-root random parent of term tid. It could be empty. */
    private Optional<TermId> getNonRootRandomParentTerm(TermId tid) {
        Set<TermId> parents = new HashSet<>(getParentTerms(ontology.subOntology(PHENOTYPIC_ABNORMALITY),tid,false));
        if (parents.contains(PHENOTYPIC_ABNORMALITY)){
            parents.remove(PHENOTYPIC_ABNORMALITY);
        }
        if (parents.isEmpty()) { //no parents could be found
            return Optional.empty();
        }
        int r = (int)Math.floor(parents.size()*Math.random());
        return Optional.of((TermId) parents.toArray()[r]);
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

    /**
     * This creates a simulated, phenotype-only case based on our annotations for the disease
     * @param disease Disease for which we will simulate the case
     * @return HpoCase object with a randomized selection of phenotypes from the disease
     */
    private List<TermId> getRandomTermsFromDisease(HpoDisease disease) {
        //We already checked to make sure disease have at least n_terms_per_case, so the following line is unnecessary and confusing to read--Aaron
        //int n_terms = Math.min(disease.getNumberOfPhenotypeAnnotations(), n_terms_per_case);
        //int n_random=Math.min(n_terms, n_noise_terms);
        logger.trace("Creating simulated case with n_terms="+n_terms_per_case + ", n_random=" + n_noise_terms);
        // the creation of a new ArrayList is needed because disease returns an immutable list.
        List<HpoAnnotation> abnormalities = new ArrayList<>(disease.getPhenotypicAbnormalities());
        ImmutableList.Builder<TermId> termIdBuilder = new ImmutableList.Builder<>();
        Collections.shuffle(abnormalities); // randomize order of phenotypes
        // take the first n_random terms of the randomized list
        if (addTermImprecision) {
            abnormalities.stream().limit(n_terms_per_case).forEach( a -> {
                Optional<TermId> randomParent = getNonRootRandomParentTerm(a.getTermId());
                if (randomParent.isPresent()) {
                    termIdBuilder.add(randomParent.get());
                } else { //cannot find non-root parent
                    termIdBuilder.add(a.getTermId());
                }
            });
        } else {
            abnormalities.stream().limit(n_terms_per_case).forEach(a-> termIdBuilder.add(a.getTermId()));
        }
        // now add n_random "noise" terms to the list of abnormalities of our case.
        for(int i=0;i<n_noise_terms;i++){
            TermId t = getRandomPhenotypeTerm();
            termIdBuilder.add(t);
        }
        return termIdBuilder.build();
    }

    private HpoCase simulateCase(HpoDisease disease) throws LiricalException {
        if (disease == null) {
            // should never happen!
            throw new LiricalException("Attempt to create case from Null-value for disease");
        }
        List<TermId> randomizedTerms = getRandomTermsFromDisease(disease);

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(randomizedTerms)
                .ontology(this.ontology)
                .diseaseMap(diseaseMap)
                .phenotypeLr(this.phenotypeLrEvaluator);
        // the following evaluates the case for each disease with equal pretest probabilities.
        // Object to evaluate the results of differential diagnosis by LR analysis.
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        return evaluator.evaluate();

    }

    private HpoCase simulateCaseWithExcludedTerm(HpoDisease disease,TermId excluded) throws LiricalException {
        if (disease == null) {
            // should never happen!
            throw new LiricalException("Attempt to create case from Null-value for disease");
        }
        List<TermId> randomizedTerms = getRandomTermsFromDisease(disease);
        List<TermId> excludedterms = new ArrayList<>();
        excludedterms.add(excluded);

        CaseEvaluator.Builder caseBuilder = new CaseEvaluator.Builder(randomizedTerms)
                .ontology(this.ontology)
                .diseaseMap(diseaseMap)
                .negated(excludedterms)
                .phenotypeLr(this.phenotypeLrEvaluator);
        // the following evaluates the case for each disease with equal pretest probabilities.
        // Object to evaluate the results of differential diagnosis by LR analysis.
        CaseEvaluator evaluator = caseBuilder.buildPhenotypeOnlyEvaluator();
        return evaluator.evaluate();

    }




}
