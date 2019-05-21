package org.monarchinitiative.lirical.likelihoodratio;


import org.monarchinitiative.phenol.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;


import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getAncestorTerms;
import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.getParentTerms;

/**
 * For some calculations of the phenotype likelihood ratio, we need to traverse the graph induced by the HPO terms to
 * which a disease is annotated. It is cheaper to create this graph once and reuse it for each of the query terms. This
 * class organizes that calculation.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class InducedDiseaseGraph {

    private final HpoDisease disease;
    /** reference to HPO ontology object. */
    private final Ontology ontology;
    private Map<TermId,Double> term2frequencyMap;
    private final static TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    /**
     * An inner class that represents a term together with the minimum path length to any
     * term that directly annotates {@link #disease}.
     */
    static class CandidateMatch {
        public int distance;
        public double frequency;
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


    public InducedDiseaseGraph(HpoDisease hpoDisease, Ontology ontology) {
        this.disease=hpoDisease;
        this.ontology = ontology;
       // ImmutableMap.Builder<TermId,Double> builder = new ImmutableMap.Builder<>();
        term2frequencyMap = new HashMap<>();

        for (HpoAnnotation annot: hpoDisease.getPhenotypicAbnormalities()) {
            double f = annot.getFrequency();
            TermId tid = annot.getTermId();
            Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(ontology,tid,true);
            for (TermId anc : ancs) {
                term2frequencyMap.putIfAbsent(anc,f);
                double oldfreq = term2frequencyMap.get(anc);
                if (f>oldfreq) { term2frequencyMap.put(anc,f); }
            }
        }
    }

    public HpoDisease getDisease() {
        return disease;
    }




    /**
     * Get the terms that annotates disease (or is an ancestor of one of the terms) that are
     * closest to tid in terms of path length. Return the best hits (list if more than one
     * terms has a closest path length
     * @param tid a query term
     * @return The best hit
     */
    public Term2Freq getClosestAncestor(TermId tid) {
        Queue<TermId> queue = new LinkedList<>();
        queue.add(tid);

        while (!queue.isEmpty()) {
            TermId t = queue.remove();
            if (this.term2frequencyMap.containsKey(t)) {
                return new Term2Freq(tid,this.term2frequencyMap.get(t));
            } else {
                Set<TermId> parents = OntologyAlgorithm.getParentTerms(ontology,t,false);
                for (TermId p : parents) {
                    queue.add(p);
                }
            }
        }

       // if we get here, then something wrong has happened, but we did not find any intersection between the query
        // term and the disease. Return a term that represents the root of the Phenotype ontology
        // The frequency of the root is taken to be 1.0

        return new Term2Freq(PHENOTYPIC_ABNORMALITY,1.0);

    }



}
