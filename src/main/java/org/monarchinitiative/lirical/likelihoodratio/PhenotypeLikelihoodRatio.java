package org.monarchinitiative.lirical.likelihoodratio;


import com.google.common.collect.ImmutableMap;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

/**
 * This class is designed to calculate the background and foreground frequencies of any HPO term in any disease. The main
 * entry point into this class is the function {@link #getLikelihoodRatio}, which is called by {@link HpoCase} once for
 * each HPO term to which the case is annotation; it calls it once for each disease in our database and calculates the
 * likelihood ratio for each of the diseases.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class PhenotypeLikelihoodRatio {
    private static final Logger logger = LoggerFactory.getLogger(PhenotypeLikelihoodRatio.class);
    /** The HPO ontology with all of its subontologies. */
    private final Ontology ontology;
    /** This map has one entry for each disease in our database. Key--the disease ID, e.g., OMIM:600200.*/
    private final Map<TermId, HpoDisease> diseaseMap;
    /** Overall, i.e., background frequency of each HPO term. */
    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;

//    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");
//    private final static TermId CLINICAL_COURSE = TermId.of("HP:0031797");
//    private final static TermId PAST_MEDICAL_HISTORY = TermId.of("HP:0032443");
    /**
     * This is the probability of a finding if it the disease is not annotated to it and there
     * is no common ancestor except the root. There are many possible causes of findings called
     * to be "false-positive". The annotations of the disease can be incomplete. A finding such
     * as renal insufficiency can manifest with proteinuria etc, but the disease annotation might
     * be renal insufficiency and the patient might just be reported to have proteinuria. Currently,
     * our software is not smart enough to make possibile connections such as this. Finally, it
     * may be truly false positive because there is a secondary etiology.
     */
    private final double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY=0.01;
    /**
     * The default probability for features that we cannot find in the dataset.
     */
    private final double DEFAULT_BACKGROUND_PROBQABILITY=1.0/10000;



    /**
     * @param onto The HPO ontology object
     * @param diseases List of all diseases for this simulation
     */
    public PhenotypeLikelihoodRatio(Ontology onto, Map<TermId, HpoDisease> diseases) {
        this.ontology=onto;
        this.diseaseMap = diseases;
        initializeFrequencyMap();
    }

    /**
     * Calculate and return the likelihood ratio of observing the HPO feature tid in an individual
     * with the disease "diseaseName"
     * @param queryTid An HPO phenotypic abnormality
     * @param idg The {@link InducedDiseaseGraph} of the disease
     * @return the likelihood ratio of observing the HPO term in the diseases
     */
    public LrWithExplanation getLikelihoodRatio(TermId queryTid, InducedDiseaseGraph idg) {
        HpoDisease disease = idg.getDisease();
        if (disease.isDirectlyAnnotatedTo(queryTid)) {
            HpoAnnotation hpoTid = disease.getAnnotation(queryTid);
            double numerator = hpoTid.getFrequency();
            double denominator = getBackgroundFrequency(queryTid);
            double lr = numerator / denominator;
            return LrWithExplanation.exactMatch(queryTid,lr);
        } else {
            // there are multiple possibilities
            // 1. the query term is a superclass of the disease term. Therefore,
            // our query satisfies the criteria for the disease and we can take the
            // frequency of the disease term. Since there may be multiple parents
            // take the maximum frequency (since the parent term will have at least this frequency)
            double cumfreq=0.0;
            boolean isAncestor=false;
            TermId diseaseMatchingTerm=null;
            for (HpoAnnotation hpoTermId : disease.getPhenotypicAbnormalities()) {
                // is query an ancestor of a term that annotates the disease?
                if (isSubclass(ontology,hpoTermId.getTermId(),queryTid)) {
                    cumfreq=Math.max(cumfreq,hpoTermId.getFrequency());
                    diseaseMatchingTerm=hpoTermId.getTermId();
                    isAncestor=true;
                }
            }
            if (isAncestor) {
                double denominator = getBackgroundFrequency(queryTid);
                double lr = cumfreq/denominator;
                return LrWithExplanation.diseaseTermSubTermOfQuery(queryTid,diseaseMatchingTerm,lr);
            }

            //2. If the query term is a subclass of one or more disease terms, then
            // we weight the frequency in the disease--- because not everybody with the disease will have the
            // subterm in question--they could have another one of the subclasses.

            for (HpoAnnotation annot : disease.getPhenotypicAbnormalities()) {
                if (isSubclass(ontology, queryTid, annot.getTermId())){
                    double proportionalFrequency = getProportionalFrequencyInAncestors(queryTid,annot.getTermId());
                    double queryFrequency = annot.getFrequency();
                    double f = proportionalFrequency*queryFrequency;
                    double denominator = getBackgroundFrequency(queryTid);
                    double lr = Math.max(f,noCommonOrganProbability(queryTid))/denominator;
                    return LrWithExplanation.queryTermSubTermOfDisease(queryTid,annot.getTermId(),lr);
                }
            }
            // If we get here, queryId is not directly annotated in the disease, and it is not a subclass
            // of a disease term, nor is a disease term a subclass of queryTid. The next bit of code
            // checks whether they have a common ancestor that is more specfic that Phenotypic_Abnormality
            Term2Freq t2f = idg.getClosestAncestor(queryTid);
            if (t2f.nonRootCommonAncestor()) {
                double numerator = t2f.frequency;
                double denominator = getBackgroundFrequency(t2f.tid);
                double lr = Math.max(DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY,numerator/denominator);
                return LrWithExplanation.nonRootCommonAncestor(queryTid,t2f.tid,lr);
            }
            // If we get here, then the only common ancestor is PHENOTYPIC_ABNORMALITY
            // therefore, return a heuristic penalty score
            return LrWithExplanation.noMatch(queryTid,DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY);
        }
    }

    /**
     * Calculate and return the likelihood ratio of an EXCLUDED HPO feature tid in an individual
     * with the disease "diseaseId"
     * @param tid An HPO phenotypic abnormality
     * @param diseaseId The CURIE (e.g., OMIM:600300) of the disease
     * @return the likelihood ratio of an EXCLUDED HPO term in the diseases
     */
    public double getLikelihoodRatioForExcludedTerm(TermId tid, TermId diseaseId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        double backgroundFrequency=getBackgroundFrequency(tid);
        // probability a feature is present but not recorded or not noticed.
        final double FALSE_NEGATIVE_OBSERVATION_OF_PHENOTYPE_PROB=0.01;
        if (backgroundFrequency>0.99) {
            logger.error("Warning, unusually high background frequency calculated for {} of {} (should never happen)",
                    backgroundFrequency,tid.getValue());
            return 1.0; // should never happen, but protect against divide by zero if there is some error
        }
        // The phenotype was excluded in the proband and also the disease
        // is not annotated to the term. This should result in a slight improvement of the LR score.
        if (! isIndirectlyAnnotatedTo(tid,disease,ontology)) {
            return 1.0/(1.0-backgroundFrequency); // this is the negative LR if the disease does not have the term
        }
        double frequency=getFrequencyOfTermInDiseaseWithAnnotationPropagation(tid,disease,ontology);
        // If the disease actually does have the abnormality in question, but the abnormality was ruled out in
        // the patient, we model this as the 1-F, where F is the frequency of the term in question.
        // We model the frequency of a term "by chance" as one half of its frequency across the entire corpus
        // of diseases.
        // if the frequency of a feature in a disease is 100%, then the expected frequency of its exclusion
        // in that disease is 0%. However, a disease can occur by chance, and we calculate this as
        //backgroundFrequency/0.5
        double excludedFrequency=Math.max(FALSE_NEGATIVE_OBSERVATION_OF_PHENOTYPE_PROB, 1-frequency);
        // now calculate and return the likelihood ratio
        return excludedFrequency/(1.0-backgroundFrequency);
    }

    /** NOTE: Use the version in phenol-1.3.3 once it becomes available!
     *
     * @param tid TermId of an HPO term
     * @param disease the disease being studied
     * @param ontology reference to HPO Ontology
     * @return true if the disease has a direct (explicit) or indirect (implicit) annotation to tid
     */
    private boolean isIndirectlyAnnotatedTo(TermId tid, HpoDisease disease, Ontology ontology) {
        List<TermId> direct = disease.getPhenotypicAbnormalityTermIdList();
        Set<TermId> ancs = ontology.getAllAncestorTermIds(direct,true);
        return ancs.contains(tid);
    }

    /**
     * Switch to phenol 1.3.3 implementation once released
     * Get the frequency of a term in the disease. This includes if any disease term is an ancestor of the
     * query term -- we take the maximum of any ancestor term.
     * @param tid Term ID of an HPO term whose frequency we want to know
     * @param disease The disease in which we want to know the frequency of tid
     * @param ontology Reference to the HPO ontology
     * @return frequency of the term in the disease (including annotation propagation)
     */
    private double getFrequencyOfTermInDiseaseWithAnnotationPropagation(TermId tid, HpoDisease disease, Ontology ontology) {
        double freq=0.0;
        for (TermId diseaseTermId :  disease.getPhenotypicAbnormalityTermIdList() ) {
            Set<TermId> ancs = ontology.getAncestorTermIds(diseaseTermId,true);
            if (ancs.contains(tid)) {
                double f = disease.getFrequencyOfTermInDisease(diseaseTermId);
                freq = Math.max(f,freq);
            }
        }
        return freq;
    }


    static class CandidateMatch {
        public int distance;
        public TermId termId;

        public CandidateMatch(TermId tid) {
            this.termId=tid;
            distance=0;
        }

        public CandidateMatch(TermId tid, int level) {
            this.termId=tid;
            this.distance = level;
        }

        public int getDistance() { return distance; }
        public TermId getTermId() { return termId; }



    }

    /**
     * Get the terms that annotates disease (or is an ancestor of one of the terms) that are
     * closest to tid in terms of path length. Return the best hits (list if more than one
     * terms has a closest path length
     * @param tid a query term
     * @param disease the disease being analyzed
     * @return A list of Ancestor terms of both the tid and one or more terms of the disease.
     */
    public List<TermId> getClosestAncestor(TermId tid,HpoDisease disease) {
        List<TermId> directAnnotations = disease.getPhenotypicAbnormalityTermIdList();
        Set<TermId> directAnnotSet = new HashSet<>(directAnnotations);
        Set<TermId> ancestors = getAncestorTerms(this.ontology,directAnnotSet,false);
        List<CandidateMatch> matches = new ArrayList<>();
        Stack<CandidateMatch> stack = new Stack<>();
        stack.push(new CandidateMatch(tid));
        int mindistance = Integer.MAX_VALUE;
        while (!stack.empty()) {
            CandidateMatch cmatch = stack.pop();
            if (ancestors.contains(cmatch.termId) && cmatch.distance <= mindistance) {
                matches.add(cmatch);
                if (cmatch.distance<mindistance)
                    mindistance=cmatch.distance;
            } else {
                Set<TermId> parents = getParentTerms(this.ontology,cmatch.termId,false);
                int level = cmatch.distance;
                for (TermId t: parents) {
                    CandidateMatch cm = new CandidateMatch(t,level+1);
                    stack.push(cm);
                }
            }
        }
        final int d = mindistance;
        return matches.stream().filter(cm-> cm.getDistance() == d).
                map(CandidateMatch::getTermId).
                collect(Collectors.toList());
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
        // We multiple the overall feature frequency in our cohort by the penalty factor
        // this will give us a likelihood ratio that varies from 0.1 to 0.002
        return falsePositivePenalty*f;
    }

    /**
     * Get the overall proportion of the frequency that is made up by the query term, given that
     * query term is a descendant of the diseaseTerm (which should be checked before this method is called).
     * @param query A term used in the query (i.e., an annotation of the HpoCase proband)
     * @param diseaseTerm A term that is annotated to the disease we are investigating
     * @return the proportion of the frequency of diseaseTerm that is attributable to query
     */
    private double getProportionalFrequencyInAncestors(TermId query, TermId diseaseTerm) {
        if (query.getId().equals(diseaseTerm.getId())) {
            return 1.0;
        }
        Set<TermId> directChildren= getChildTerms(ontology,diseaseTerm,false);
        if (directChildren.isEmpty()) {
            return 0.0;
        }     
        double f=0.0;
        for (TermId tid : directChildren) {
            f += getProportionalFrequencyInAncestors(query,tid);
        }
        return f/(double)directChildren.size();
    }
    
        

    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    double getBackgroundFrequency(TermId termId) {
        if (! hpoTerm2OverallFrequency.containsKey(termId)) {
            logger.error(String.format("Map did not contain data for term %s",termId.getValue() ));
            logger.error(String.format("hpoTerm2OverallFrequency has total of %d entries",hpoTerm2OverallFrequency.size()));
            // Should never happen!
            return DEFAULT_BACKGROUND_PROBQABILITY;

        }
        return Math.max(DEFAULT_BACKGROUND_PROBQABILITY,hpoTerm2OverallFrequency.get(termId));
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology. */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : ontology.getNonObsoleteTermIds()) {
            mp.put(tid, 0.0D);
        }
        ImmutableMap.Builder<TermId, Double> mapbuilder = new ImmutableMap.Builder<>();
        for (HpoDisease dis : this.diseaseMap.values()) {
            // We construct a map in order to get the maximum frequencies for any
            // given ancestor term, also in order to avoid double counting.
            Map<TermId, Double> updateMap=new HashMap<>();

            for (HpoAnnotation tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid = tidm.getTermId();
                double termFrequency = tidm.getFrequency();
                // All of the ancestor terms are implicitly annotated to tid
                // therefore, add this to their background frequencies.
                // Note we also include the original term here (third arg: true)
                tid=ontology.getPrimaryTermId(tid);
                Set<TermId> ancs = getAncestorTerms(ontology,tid,true);
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
        double N = (double) getNumberOfDiseases();
        for (Map.Entry<TermId, Double> me : mp.entrySet()) {
            double f = me.getValue() / N;
            mapbuilder.put(me.getKey(), f);
        }
        hpoTerm2OverallFrequency = mapbuilder.build();
        logger.trace("Got data on background frequency for " + hpoTerm2OverallFrequency.size() + " terms");
    }

    /** @return the number of diseases we are using for the calculations. */
    int getNumberOfDiseases() {
        return diseaseMap.size();
    }

}
