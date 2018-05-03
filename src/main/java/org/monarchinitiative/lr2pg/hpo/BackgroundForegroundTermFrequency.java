package org.monarchinitiative.lr2pg.hpo;



import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.graph.IdLabeledEdge;
import org.monarchinitiative.phenol.graph.algo.BreadthFirstSearch;
import org.monarchinitiative.phenol.ontology.data.*;

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
public class BackgroundForegroundTermFrequency {
    private static final Logger logger = LogManager.getLogger();
    /** The HPO ontology with all of its subontologies. */
    private final HpoOntology ontology;
    /** This map has one entry for each disease in our database. Key--the disease ID, e.g., OMIM:600200.*/
    private final Map<String, HpoDisease> diseaseMap;
    /** Overall, i.e., background frequency of each HPO term. */
    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;

    private final static TermId PHENOTYPIC_ABNORMALITY = ImmutableTermId.constructWithPrefix("HP:0000118");

    private final static double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY = 0.000_005; // 1:20,000


    /**
     * @param onto The HPO ontology object
     * @param diseases List of all diseases for this simulation
     */
    BackgroundForegroundTermFrequency(HpoOntology onto, Map<String, HpoDisease> diseases) {
        this.ontology=onto;
        this.diseaseMap = diseases;
        initializeFrequencyMap();
    }

    /**
     * Calculate and return the likelihood ratio of observing the HPO feature tid in an individual
     * with the disease "diseaseName"
     * @param tid An HPO phenotypic abnormality
     * @param disease the disease
     * @return the likelihood ratio of observing the HPO term in the diseases
     * @throws Lr2pgException If there is an error calculating the LR
     */
    double getLikelihoodRatio(TermId tid, HpoDisease disease) throws Lr2pgException{
        double numerator=getFrequencyOfTermInDisease(disease,tid);
        double denominator=getBackgroundFrequency(tid);
        return numerator/denominator;
    }


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
     * This will return an ordered list of terms emanating from t1 up to the root of the ontology.
     * @param ontology An object representing the Human Phenotype Ontology
     * @param t1 term of interest
     * @return an ordered list of terms emanating from t1 up to the root of the ontology.
     */
    private List<TermId> getPathsToRoot(Ontology<? extends Term, ? extends Relationship> ontology, final TermId t1) {
        final DefaultDirectedGraph<TermId, IdLabeledEdge> graph = ontology.getGraph();
        List<TermId> visitedT1 = new ArrayList<>(); // this will contain all paths from query term to the root
        BreadthFirstSearch<TermId, IdLabeledEdge> bfs = new BreadthFirstSearch<>();
        bfs.startFromForward(
                graph,
                t1,
                (g, termId) -> {
                    visitedT1.add(termId);
                    return true;
                });
        return visitedT1;
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
        // take the average
        int n=0;
        double cumfreq=0.0;
        for (HpoAnnotation hpoTermId : disease.getPhenotypicAbnormalities()) {
            if (isSubclass(ontology,query,hpoTermId.getTermId())) {
                cumfreq+=hpoTermId.getFrequency();
                n++;
            }
        }
        if (n>0) return cumfreq/n;

        //2. If the disease has a subclass of the query term, then
        // everybody with the subclass (e.g., nuclear cataract) also has the
        // parent (e.g., cataract). Hard to say if our query is just inexact or if
        // there is some difference, but not everybody with the disease will have the
        // subterm in question--they could have another one of the subclasses.
        // therefore we need to penalize
        Set<TermId> diseaseTermIds = new HashSet<>();
        for(HpoAnnotation diseaseHpId : disease.getPhenotypicAbnormalities()){
            diseaseTermIds.add(diseaseHpId.getTermId());
        }
        Set<TermId> allAncs = getAncestorTerms(ontology, diseaseTermIds, true);
        for (HpoAnnotation hpoTermId : disease.getPhenotypicAbnormalities()) {
            if (isSubclass(ontology, hpoTermId.getTermId(), query)) {
                List<TermId> pathToRoot = getPathsToRoot(ontology, query);
                for (int i = 0; i < pathToRoot.size(); i++) {
                    TermId td = pathToRoot.get(i);
                    if (allAncs.contains(td)) {
                        // the induced graph of the disease contains an ancestor of the query term
                        if (td.equals(PHENOTYPIC_ABNORMALITY)) break; // no match!
                        //Not sure about the numerator! 1 or td.frequency?
                        double frequency = hpoTermId.getFrequency();
                        return frequency / (1 + Math.log(i));
                    }
                }
            }
        }
        // If we get here, then there is no common ancestor between the query and any of the disease phenotype annotations.
       return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;
    }


    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    double getBackgroundFrequency(TermId termId) {
        if (! hpoTerm2OverallFrequency.containsKey(termId)) {
            logger.fatal(String.format("Map did not contain data for term %s",termId.getIdWithPrefix() ));
            // todo throw error
            System.exit(1);
        }
        return Math.max(DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY,hpoTerm2OverallFrequency.get(termId));
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology.
     */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : getDescendents(ontology, PHENOTYPIC_ABNORMALITY)) {
            mp.put(tid, 0.0D);
        }
        ImmutableMap.Builder<TermId, Double> imb = new ImmutableMap.Builder<>();
        for (HpoDisease dis : this.diseaseMap.values()) {
            for (HpoAnnotation tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid = tidm.getTermId();
                if (!mp.containsKey(tid)) {
                    mp.put(tid, 0.0);
                }
                double delta = tidm.getFrequency();
                // All of the ancestor terms are implicitly annotated to tid
                // therefore, add this to their background frequencies.
                // Note we also include the original term here (third arg: true)
                tid=ontology.getPrimaryTermId(tid);
                Set<TermId> ancs = getAncestorTerms(ontology,tid,true);
                for (TermId at : ancs) {
                    if (!mp.containsKey(at)) mp.put(at, 0.0);
                    double cumulativeFreq = mp.get(at) + delta;
                    mp.put(at, cumulativeFreq);
                }
            }
        }
        // Now we need to normalize by the number of diseases.
        double N = (double) getNumberOfDiseases();
        for (Map.Entry<TermId, Double> me : mp.entrySet()) {
            double f = me.getValue() / N;
            imb.put(me.getKey(), f);
        }
        hpoTerm2OverallFrequency = imb.build();
        //
        logger.trace("Got data on background frequency for " + hpoTerm2OverallFrequency.size() + " terms");
        // ToDo: we need to define some background frequency for the terms used to anotate our corpus
        // We will use a heuristic that will distribute the probabilities of parent terms
        // to the (unannotated) terms beneath them in the tree.
    }
    /** @return the number of diseases we are using for the calculations. */
    int getNumberOfDiseases() {
        return diseaseMap.size();
    }

}
