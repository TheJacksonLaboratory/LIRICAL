package org.monarchinitiative.lr2pg.likelihoodratio;


import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

/**
 * This class is designed to calculate the background and foreground frequencies of any HPO term in any disease. The main
 * entry point into this class is the function {@link #getLikelihoodRatio}, which is called by {@link HpoCase} once for
 * each HPO term to which the case is annotation; it calls it once for each disease in our database and calculates the
 * likelihood ratio for each of the diseases.
 * @author <a href="mailto:vida.ravanmehr@jax.org">Vida Ravanmehr</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class PhenotypeLikelihoodRatio {
    private static final Logger logger = LogManager.getLogger();
    /** The HPO ontology with all of its subontologies. */
    private final Ontology ontology;
    /** This map has one entry for each disease in our database. Key--the disease ID, e.g., OMIM:600200.*/
    private final Map<TermId, HpoDisease> diseaseMap;
    /** Overall, i.e., background frequency of each HPO term. */
    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;

    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    private final static double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY = 0.000_005; // 1:20,000


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
     * @param tid An HPO phenotypic abnormality
     * @param diseaseId The CURIE (e.g., OMIM:600300) of the disease
     * @return the likelihood ratio of observing the HPO term in the diseases
     */
    public double getLikelihoodRatio(TermId tid, TermId diseaseId) {
        HpoDisease disease = this.diseaseMap.get(diseaseId);
        double numerator=getFrequencyOfTermInDisease(disease,tid);
        double denominator=getBackgroundFrequency(tid);
        return numerator/denominator;
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
            logger.error("Warning, unusually high bachground freuqency calculated for {} of {} (should never happen)",
                    backgroundFrequency,tid.getValue());
            return 1.0; // should never happen, but protect against divide by zero if there is some error
        }
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
     * @param tid
     * @param disease
     * @param ontology
     * @return
     */
    @Deprecated
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
    @Deprecated
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




    /**
     * Calcultates the frequency of a phenotypic abnormality represented by the TermId tid in the disease.
     * If the disease is not annotated to tid, the method {@link #getFrequencyIfNotAnnotated(TermId, HpoDisease)}
     * is called to provide an estimate.
     * @return the Frequency of tid in the disease */
    double getFrequencyOfTermInDisease(HpoDisease disease, TermId tid) {
        HpoAnnotation hpoTid = disease.getAnnotation(tid);
        if (hpoTid==null) {
            // this disease does not have the Hpo term in question
            return getFrequencyIfNotAnnotated(tid,disease);
        } else {
            return hpoTid.getFrequency();
        }
    }


    /**
     * If we get here, we are trying to find a frequency for a term in a disease but there is not
     * direct match. This function tries several ways of finding a fuzzy match
     * @param query -- the term in the patient being tested for similarity with this disease
     * @param disease the disease for which we want to calculate the frequency
     * @return estimated frequency of the feature given the disease
     */
    private double getFrequencyIfNotAnnotated(TermId query, HpoDisease disease) {
        //Try to find a matching child term.

        // 1. the query term is a superclass of the disease term. Therefore,
        // our query satisfies the criteria for the disease and we can take the
        // frequency of the disease term. Since there may be multiple parents
        // take the maximum frequency (since the parent term will have at least this frequency)
        double cumfreq=0.0;
        boolean isAncestor=false;
        for (HpoAnnotation hpoTermId : disease.getPhenotypicAbnormalities()) {
            if (isSubclass(ontology,hpoTermId.getTermId(),query)) {
                cumfreq=Math.max(cumfreq,hpoTermId.getFrequency());
                isAncestor=true;
            }
        }
        if (isAncestor) return cumfreq;

        //2. If the query term is a subclass of one or more disease terms, then
        // we weight the frequency in the disease--- because not everybody with the disease will have the
        // subterm in question--they could have another one of the subclasses.

        for (HpoAnnotation annot : disease.getPhenotypicAbnormalities()) {
            if (isSubclass(ontology, query, annot.getTermId())){
                double proportionalFrequency = getProportionalFrequencyInAncestors(query,annot.getTermId());
                double queryFrequency = annot.getFrequency();
                double f = proportionalFrequency*queryFrequency;
                return Math.max(f,DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY);
            }
        }
        // If we get here, then there is no common ancestor between the query and any of the disease phenotype annotations.
       return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;
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
            // todo throw error
            System.exit(1);
        }
        return Math.max(DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY,hpoTerm2OverallFrequency.get(termId));
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology. */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : getDescendents(ontology, PHENOTYPIC_ABNORMALITY)) {
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
