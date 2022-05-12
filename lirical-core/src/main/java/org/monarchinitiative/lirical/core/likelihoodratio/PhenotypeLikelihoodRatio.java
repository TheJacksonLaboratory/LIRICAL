package org.monarchinitiative.lirical.core.likelihoodratio;


import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class is designed to calculate the background and foreground frequencies of any HPO term in any disease
 * (This is calculated by {@link #initializeFrequencyMap()} and stored in {@link #hpoTerm2OverallFrequency}).
 * The main entry point into this class is the function {@link #lrForObservedTerm}, which is called by
 * {@link LiricalAnalysisRunner} once for each HPO term
 * to which the case is annotation; it calls it once for each disease in our
 * database and calculates the likelihood ratio for each HPO term in the query for each of the diseases.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
// TODO - should be an interface
public class PhenotypeLikelihoodRatio {
    private static final Logger logger = LoggerFactory.getLogger(PhenotypeLikelihoodRatio.class);
    // TODO - the constants below should be parameters?
    /**
     * The default probability for an HPO term annotating a disease if we cannot find in the dataset.
     */
    private static final double DEFAULT_BACKGROUND_FREQUENCY =1.0/10000;
    /** The default likelihood ratio for a query term that is explicitly excluded in a disease.*/
    private static final double EXCLUDED_IN_DISEASE_BUT_PRESENT_IN_QUERY_PROBABILITY = 1.0/1000;
    /** The default likelihood ratio for an excluded query term that is explicitly excluded in a disease.*/
    private static final double EXCLUDED_IN_DISEASE_AND_EXCLUDED_IN_QUERY_PROBABILITY = 1000.0;
    private static final double FALSE_NEGATIVE_OBSERVATION_OF_PHENOTYPE_PROB = 0.01;
    /** The default frequency of a term in a disease if the explicit frequency is not available. */
    public static final float DEFAULT_TERM_FREQUENCY = 1.f; // TODO - is this the right thing to do?
    /** The HPO ontology with all of its subontologies. */
    private final Ontology ontology;
    /** This map has one entry for each disease in our database. Key--the disease ID, e.g., OMIM:600200.*/
    private final Map<TermId, HpoDisease> diseaseMap;
    private final LrWithExplanationFactory explanationFactory;
    /** Overall, i.e., background frequency of each HPO term. */
    private Map<TermId, Double> hpoTerm2OverallFrequency = null;
    /**
     * This is the probability of a finding if the disease is not annotated to it and there
     * is no common ancestor except the root. There are many possible causes of findings called
     * to be "false-positive". The annotations of the disease can be incomplete. A finding such
     * as renal insufficiency can manifest with proteinuria etc, but the disease annotation might
     * be renal insufficiency and the patient might just be reported to have proteinuria. Currently,
     * our software is not smart enough to make possible connections such as this. Finally, it
     * may be truly false positive because there is a secondary etiology. We assume arbitrary
     * small probability (one in ten thousand)
     */
    private final double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY=0.01;

    /**
     * @param ontology The HPO ontology object
     * @param diseases List of all diseases for this simulation
     */
    public PhenotypeLikelihoodRatio(Ontology ontology, HpoDiseases diseases) {
        this.ontology = ontology;
        this.diseaseMap = diseases.diseaseById();
        this.explanationFactory = new LrWithExplanationFactory(ontology); // TODO - DI?
        initializeFrequencyMap();
    }

    /**
     * Calculate and return the likelihood ratio of observing the HPO feature queryTid in an individual
     * with the disease idg (note that the InducedDiseaseGraph contains information about the annotations
     * of the disease that we use to calculate the likelihood ratios).
     * @param queryTid An HPO phenotypic abnormality
     * @param idg The {@link InducedDiseaseGraph} of the disease
     * @return A {@link LrWithExplanation} object with an explanation and the likelihood ratio of observing the HPO term in the disease corresponding to idg
     */
    public LrWithExplanation lrForObservedTerm(TermId queryTid, InducedDiseaseGraph idg) {
        HpoDisease disease = idg.getDisease();
        Set<TermId> queryAncestors = OntologyAlgorithm.getAncestorTerms(ontology,queryTid,true);
        if (disease.absentAnnotationsStream().anyMatch(a -> queryAncestors.contains(a.id()))) {
            // i.e., the query term is explicitly EXCLUDED in the disease definition
            return explanationFactory.create(queryTid,
                    LrMatchType.QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE,
                    EXCLUDED_IN_DISEASE_BUT_PRESENT_IN_QUERY_PROBABILITY);
        }

        if (disease.isDirectlyAnnotatedTo(queryTid)) {
            // The optional must be present since the disease is directly annotated to the query term.
            //noinspection OptionalGetWithoutIsPresent
            HpoDiseaseAnnotation annotation = disease.getAnnotation(queryTid).get();
            double numerator = annotation.frequency();
            double denominator = getBackgroundFrequency(queryTid);
            double lr = numerator / denominator;
            return explanationFactory.create(queryTid, LrMatchType.EXACT_MATCH, lr);
        } else {
            // there are multiple possibilities
            // 1. the query term is a superclass of at least one disease term. Therefore,
            // our query satisfies the criteria for the disease and we can take the
            // frequency of the disease term. Since there may be multiple parents
            // take the maximum frequency (since the parent term will have at least this frequency)
            double maximumFrequencyOfDescendantTerm = 0.0;
            boolean isAncestor=false;
            TermId diseaseMatchingTerm=null;
            for (HpoDiseaseAnnotation annotation : disease.annotations()) {
                double frequency = annotation.frequency();
                // is query an ancestor of a term that annotates the disease?
                if (OntologyAlgorithm.isSubclass(ontology,annotation.id(),queryTid)) {
                    maximumFrequencyOfDescendantTerm=Math.max(maximumFrequencyOfDescendantTerm,frequency);
                    diseaseMatchingTerm=annotation.id();
                    isAncestor=true;
                }
            }
            if (isAncestor) {
                double denominator = getBackgroundFrequency(queryTid);
                double lr = maximumFrequencyOfDescendantTerm/denominator;
                return explanationFactory.create(queryTid, diseaseMatchingTerm,
                        LrMatchType.DISEASE_TERM_SUBCLASS_OF_QUERY,
                        lr);
            }
            // if we get here, then the query term was not a superclass of a disease term

            //2. If the query term is a subclass of one or more disease terms, then
            // we weight the frequency in the disease--- because not everybody with the disease will have the
            // subterm in question--they could have another one of the subclasses.
            //  Note that isSubclass is implemented like this:
            //public static boolean isSubclass(
            //    Ontology ontology, TermId source, TermId dest) {
            //    return ontology.getAncestorTermIds(source).contains(dest);
            //  }

            boolean hasNonRootCommonAncestor = false;
            double maxF = 0f;
            TermId bestMatchTermId = null;
            double denominatorForNonRootCommandAnc = getBackgroundFrequency(queryTid);
            for (HpoDiseaseAnnotation annotation : disease.annotations()) {
                if (OntologyAlgorithm.isSubclass(ontology, queryTid, annotation.id())){
                    double proportionalFrequency = getProportionInChildren(queryTid,annotation.id());
                    double queryFrequency = annotation.frequency();
                    double f = proportionalFrequency*queryFrequency;
                    if (f > maxF) {
                        bestMatchTermId = annotation.id();
                        maxF = f;
                        hasNonRootCommonAncestor = true;
                    }
                }
            }
            if (hasNonRootCommonAncestor) {
                double lr = Math.max(maxF,noCommonOrganProbability(queryTid))/denominatorForNonRootCommandAnc;
                return explanationFactory.create(queryTid, bestMatchTermId,
                        LrMatchType.QUERY_TERM_SUBCLASS_OF_DISEASE_TERM,
                        lr);
            }
            // If we get here, queryId is not directly annotated in the disease, and it is not a child
            // of a disease term, nor is a disease term a subclass of queryTid. The next bit of code
            // checks whether they have a common ancestor that is more specfic that Phenotypic_Abnormality
            Term2Freq t2f = idg.getClosestAncestor(queryTid, ontology);
            if (t2f.nonRootCommonAncestor()) {
                double numerator = t2f.frequency();
                double denominator = getBackgroundFrequency(t2f.termId());
                double lr = Math.max(DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY,numerator/denominator);
                return explanationFactory.create(queryTid, t2f.termId(), LrMatchType.NON_ROOT_COMMON_ANCESTOR, lr);
            }
            // If we get here, then the only common ancestor is PHENOTYPIC_ABNORMALITY
            // therefore, return a heuristic penalty score
            return explanationFactory.create(queryTid, LrMatchType.NO_MATCH_BELOW_ROOT, DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY);
        }
    }

    /**
     * Calculate and return the likelihood ratio of an EXCLUDED HPO feature tid in an individual
     * with the disease "diseaseId"
     * @param queryTid An HPO phenotypic abnormality
     * @param idg An {@link InducedDiseaseGraph} created for the disease
     * @return the likelihood ratio of an EXCLUDED HPO term in the diseases
     */
    public LrWithExplanation lrForExcludedTerm(TermId queryTid, InducedDiseaseGraph idg) {
        HpoDisease disease = idg.getDisease();
        // check if term excluded in query is also excluded in disease
        if (idg.isExactExcludedMatch(queryTid)) {
            return explanationFactory.create(queryTid,
                    LrMatchType.EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE,
                    EXCLUDED_IN_DISEASE_AND_EXCLUDED_IN_QUERY_PROBABILITY);
        }
        double backgroundFrequency=getBackgroundFrequency(queryTid);
        // probability a feature is present but not recorded or not noticed.
        if (backgroundFrequency>0.99) {
            logger.error("Warning, unusually high background frequency calculated for {} of {} (should never happen)",
                    backgroundFrequency,queryTid.getValue());
            // should never happen, but protect against divide by zero if there is some error
            return explanationFactory.create(queryTid, LrMatchType.UNUSUAL_BACKGROUND_FREQUENCY, 1.);
        }
        // The phenotype was excluded in the proband and also the disease
        // is not annotated to the term. This should result in a slight improvement of the LR score.
        if (!disease.isAnnotatedTo(queryTid, ontology)) {
            double lr = 1.0/(1.0-backgroundFrequency); // this is the negative LR if the disease does not have the term
            return explanationFactory.create(queryTid, LrMatchType.EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE, lr);
        }
        double frequency=getFrequencyOfTermInDiseaseWithAnnotationPropagation(queryTid,disease,ontology);
        // If the disease actually does have the abnormality in question, but the abnormality was ruled out in
        // the patient, we model this as the 1-F, where F is the frequency of the term in question.
        // We model the frequency of a term "by chance" as one half of its frequency across the entire corpus
        // of diseases.
        // if the frequency of a feature in a disease is 100%, then the expected frequency of its exclusion
        // in that disease is 0%. However, a disease can occur by chance, and we calculate this using the constant
        // FALSE_NEGATIVE_OBSERVATION_OF_PHENOTYPE_PROB
        double excludedFrequency=Math.max(FALSE_NEGATIVE_OBSERVATION_OF_PHENOTYPE_PROB, 1-frequency);
        // now calculate and return the likelihood ratio
        double lr = excludedFrequency/(1.0-backgroundFrequency);
        return explanationFactory.create(queryTid, LrMatchType.EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE, lr);
    }

    /**
     * Get the frequency of a term in the disease. This includes if any disease term is an ancestor of the
     * query term -- we take the maximum of any ancestor term.
     * @param query Term ID of an HPO term whose frequency we want to know
     * @param disease The disease in which we want to know the frequency of tid
     * @param ontology Reference to the HPO ontology
     * @return frequency of the term in the disease (including annotation propagation)
     */
    private static double getFrequencyOfTermInDiseaseWithAnnotationPropagation(TermId query, HpoDisease disease, Ontology ontology) {
        double maxFrequency = 0.0;
        for (HpoDiseaseAnnotation annotation : disease.annotations()) {
            Set<TermId> ancestors = ontology.getAncestorTermIds(annotation.id(),true);
            if (ancestors.contains(query))
                maxFrequency = Math.max(maxFrequency, disease.getFrequencyOfTermInDisease(annotation.id()).map(Ratio::frequency).orElse(DEFAULT_TERM_FREQUENCY));
        }
        return maxFrequency;
    }

    /** The intuition is that a patient has been observed to have a phenotype to which the disease
     * is not annotated. We will model this as being more likely if the phenotype is common amongst
     * the entire corpus of diseases. If the feature is maximally rare, i.e., 1/diseases.size(), then
     * we will estimate this frequency as being 1:500. If the feature is very common (at least 10%),
     * then we will estimate it as being 1:10.
     * @param tid TermId of a term for which the disease has no annotations (nothing in common except root)
     * @return Estimate probability of this ("false-positive") finding
     */
    private double noCommonOrganProbability(TermId tid) {
        double f = this.hpoTerm2OverallFrequency.getOrDefault(tid, DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY);
        final double MIN_PROB = 0.002; // lowest prob of 1:500
        final double MAX_PROB = 0.10; // highest prob of 1:10
        final double MAX_MINUS_MIN = MAX_PROB - MIN_PROB;
        final double DENOMINATOR = MAX_PROB - DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;
        final double FACTOR = MAX_MINUS_MIN/DENOMINATOR;
        // falsePositivePenalty represents the range of penalty for a completely false positive finding -- from 0.1 to 1:500
        // the more common the finding, the closer the penalty is to 0.1; the rarer it is, the closer we
        // get to 1:500
        double falsePositivePenalty = MIN_PROB + (f - DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY) * FACTOR;
        // We multiply the overall feature frequency in our cohort by the penalty factor
        // this will give us a likelihood ratio that varies from 0.1 to 0.002
        return falsePositivePenalty * f;
    }

    /**
     * Get the overall proportion of the frequency that is made up by the query term, given that
     * query term is a descendant of the diseaseTerm (which should be checked before this method is called).
     * @param queryTid A term used in the query (i.e., an annotation of the HpoCase proband)
     * @param diseaseTid A term that is annotated to the disease we are investigating
     * @return the proportion of the frequency of diseaseTerm that is attributable to query
     */
    private double getProportionInChildren(TermId queryTid, TermId diseaseTid) {
        if (queryTid.getId().equals(diseaseTid.getId())) {
            return 1.0;
        }
        Set<TermId> directChildren= OntologyAlgorithm.getChildTerms(ontology,diseaseTid,false);
        if (directChildren.isEmpty()) {
            return 0.0;
        }     

        for (TermId tid : directChildren) {
            if (queryTid.equals(tid)) {
                return 1.0/(double)directChildren.size();
            }
        }
        // if we get here, there was no match
        return 0d;
    }
    
        

    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    double getBackgroundFrequency(TermId termId) {
        Double backgroundFrequency = hpoTerm2OverallFrequency.get(termId);
        if (backgroundFrequency == null) {
            logger.error("Map did not contain data for term {}",termId.getValue() );
            logger.error("hpoTerm2OverallFrequency has total of {} entries", hpoTerm2OverallFrequency.size());
            // Should never happen!
            return DEFAULT_BACKGROUND_FREQUENCY;
        }

        return Math.max(backgroundFrequency, DEFAULT_BACKGROUND_FREQUENCY);
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology. */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : ontology.getNonObsoleteTermIds()) {
            mp.put(tid, 0.0D);
        }
        Map<TermId, Double> mapbuilder = new HashMap<>();
        for (HpoDisease dis : this.diseaseMap.values()) {
            // We construct a map in order to get the maximum frequencies for any
            // given ancestor term, also in order to avoid double counting.
            Map<TermId, Double> updateMap=new HashMap<>();

            for (HpoDiseaseAnnotation annotation : dis.annotations()) {
                TermId tid = annotation.id();
                double termFrequency = annotation.frequency();
                TermId primaryTermId = ontology.getPrimaryTermId(tid);
                if (primaryTermId == null) {
                    logger.warn("Primary term ID for {} was not found!", tid.getValue());
                    continue;
                }
                // All of the ancestor terms are implicitly annotated to tid
                // therefore, add this to their background frequencies.
                // Note we also include the original term here (third arg: true)
                Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(ontology,primaryTermId,true);
                for (TermId at : ancs) {
                    updateMap.putIfAbsent(at,termFrequency);
                    // put the maximum frequency for this term given it is
                    // an ancestor of one or more of the HPO terms that annotate
                    // the disease.
                    if (termFrequency > updateMap.get(at)) {
                        updateMap.put(at,termFrequency);
                    }
                }
            }
            for (TermId tid : updateMap.keySet()) {
                double delta = updateMap.get(tid);
                mp.putIfAbsent(tid,0.0);
                double cumulative = delta + mp.get(tid);
                mp.put(tid,cumulative);
            }
        }
        // Now we need to normalize by the number of diseases.
        double N = getNumberOfDiseases();
        for (Map.Entry<TermId, Double> me : mp.entrySet()) {
            double f = me.getValue() / N;
            mapbuilder.put(me.getKey(), f);
        }
        hpoTerm2OverallFrequency = Map.copyOf(mapbuilder);
        logger.trace("Got data on background frequency for " + hpoTerm2OverallFrequency.size() + " terms");
    }

    /** @return the number of diseases we are using for the calculations. */
    private int getNumberOfDiseases() {
        return diseaseMap.size();
    }

}
