package org.monarchinitiative.lirical.simulation;


import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.CaseEvaluator;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getDescendents;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * A simulator that simulates cases from the {@link HpoDisease} objects by choosing a subset of terms
 * and adding noise terms.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class PhenotypeOnlyHpoCaseSimulator {
    private static final Logger logger = LoggerFactory.getLogger(PhenotypeOnlyHpoCaseSimulator.class);
    /** An object representing the Human Phenotype Ontology */
    private Ontology ontology;
    /** An object that calculates the foreground frequency of an HPO term in a disease as well as the background frequency */
    private final PhenotypeLikelihoodRatio phenotypeLrEvaluator;
    /** A list of all HPO term ids in the Phenotypic abnormality subontology. */
    private final ImmutableList<TermId> phenotypeterms;
    /** Key: diseaseID, e.g., OMIM:600321; value: Corresponding HPO disease object. */
    private final Map<TermId, HpoDisease> diseaseMap;
    /** Number of HPO terms to use for each simulated case. */
    private final int n_terms_per_case;
    /** Number of "noise" (unrelated) HPO terms to use for each simulated case. */
    private final int n_noise_terms;
    /** Number of cases to simulate. */
    private final int n_cases_to_simulate;
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

    /**
     * The constructor initializes {@link #ontology} and {@link #diseaseMap} and {@link #phenotypeterms}. This
     * constructor sets "imprecision" to false.
     * @param ontology reference to HPO Ontology object
     * @param diseaseMap Map containing (usuallu) all diseases in the corpus
     * @param cases_to_simulate Number of individual simulations to perform
     * @param terms_per_case Number of HPO terms per case
     * @param noise_terms Number of "noise" (random, unrelated) terms to add per case
     */
    public PhenotypeOnlyHpoCaseSimulator(Ontology ontology,
                                         Map<TermId,HpoDisease> diseaseMap,
                                         int cases_to_simulate,
                                         int terms_per_case,
                                         int noise_terms ) {
        this.n_cases_to_simulate=cases_to_simulate;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.phenotypeLrEvaluator = new PhenotypeLikelihoodRatio(ontology,diseaseMap);
        Set<TermId> descendents=getDescendents(ontology,PHENOTYPIC_ABNORMALITY);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (TermId t: descendents) {
            builder.add(t);
        }
        this.phenotypeterms=builder.build();
        this.termIndices=diseaseMap.keySet().toArray(new TermId[0]);
    }

    /**
     * @param ontology reference to HPO Ontology object
     * @param diseaseMap Map containing (usuallu) all diseases in the corpus
     * @param cases_to_simulate Number of individual simulations to perform
     * @param terms_per_case Number of HPO terms per case
     * @param noise_terms Number of "noise" (random, unrelated) terms to add per case
     * @param imprecise Whether or not to use imprecision
     */
    public PhenotypeOnlyHpoCaseSimulator(Ontology ontology,
                                         Map<TermId,HpoDisease> diseaseMap,
                                         int cases_to_simulate,
                                         int terms_per_case,
                                         int noise_terms,
                                         boolean imprecise ) {
        this(ontology,diseaseMap,cases_to_simulate,terms_per_case,noise_terms);
        this.addTermImprecision=imprecise;
    }


    public void setVerbosity(boolean v) { this.verbose=v;}

    /** @return the proportion of all simulated cases at rank 1.*/
    public double getProportionAtRank1() {
        return proportionAtRank1;
    }

    private TermId getNextRandomDisease(Random r) {
        int i = r.nextInt(diseaseMap.size());
        TermId tid = termIndices[i];
        HpoDisease disease = diseaseMap.get(tid);
        while (disease.getPhenotypicAbnormalities().size() < this.n_terms_per_case) {
            i = r.nextInt(diseaseMap.size());
            tid = termIndices[i];
            disease = diseaseMap.get(tid);
        }
        return tid;
    }



    /** This will run simulations according to the parameters {@link #n_cases_to_simulate},
     * {@link #n_terms_per_case} and {@link #n_noise_terms}.
     * @throws LiricalException if there is an issue running the simulation
     */
    public void simulateCases() throws LiricalException {
        int c=0;
        Map<Integer,Integer> ranks=new HashMap<>();
        List<TermId> notRanked = new ArrayList<>();
        logger.trace(String.format("Simulating n=%d HPO cases with %d random terms and %d noise terms per case.",n_cases_to_simulate,n_terms_per_case,n_noise_terms));
        int size = diseaseMap.size();

        //int[] randomIndices=IntStream.generate(() -> new Random().nextInt(diseaseMap.size())).limit(n_cases_to_simulate).toArray();

        Random r = new Random();

        for (int i=0;i<n_cases_to_simulate;++i) {
            TermId diseaseToSimulate = getNextRandomDisease(r);//termIndices[randomIndices[i]];
            HpoDisease disease = diseaseMap.get(diseaseToSimulate);
            //logger.trace("Simulating disease "+diseasename);
            if (disease.getNumberOfPhenotypeAnnotations() <this.n_terms_per_case) {
                logger.trace(String.format("Skipping disease %s [%s] because it has no phenotypic annotations",
                        disease.getName(),
                        disease.getDiseaseDatabaseId()));
                continue;
            }
            Optional<Integer> optionalRank = simulateCase(disease);
            if (optionalRank.isPresent()) {
                int rank = optionalRank.get();
                if (verbose) {
                    System.err.println(String.format("%s: rank=%d", disease.getName(), rank));
                }
                ranks.putIfAbsent(rank,0);
                ranks.put(rank, ranks.get(rank) + 1);
            } else {
                notRanked.add(diseaseToSimulate);
            }


        }
        if (ranks.containsKey(1)) {
            proportionAtRank1 = ranks.get(1) / (double) n_cases_to_simulate;
        } else {
            proportionAtRank1 = 0.0;
        }
        if (verbose) {
            dump2shell(ranks);
            System.out.println("Could not rank " + notRanked.size() + " diseases");
        }


    }


    private void dump2shell(Map<Integer,Integer> ranks) {
        int N=n_cases_to_simulate;
        int rank11_20=0;
        int rank21_30=0;
        int rank31_100=0;
        int rank101_up=0;
        System.out.println();
        System.out.println();
        System.out.println(String.format("Simulation of %d cases with %d HPO terms, %d noise terms. Imprecision: %s",
                n_cases_to_simulate,n_terms_per_case,n_noise_terms,addTermImprecision));
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


    /** @return a non-root random parent of term tid. It could be empty. */
    private Optional<TermId> getNonRootRandomParentTerm(TermId tid) {
        Set<TermId> parents = new HashSet<>(getParentTerms(ontology.subOntology(PHENOTYPIC_ABNORMALITY),tid,false));
        parents.remove(PHENOTYPIC_ABNORMALITY);
        if (parents.isEmpty()) { //no parents could be found
            return Optional.empty();
        }
        int r = (int)Math.floor(parents.size()*Math.random());
        return Optional.of((TermId) parents.toArray()[r]);
    }


    public Ontology getOntology() {
        return ontology;
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



    private Optional<Integer> simulateCase(HpoDisease disease) throws LiricalException {
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
        HpoCase hpocase = evaluator.evaluate();
        if (verbose)
            System.err.println(hpocase.toString());
        return hpocase.getRank(disease.getDiseaseDatabaseId());
    }


}
