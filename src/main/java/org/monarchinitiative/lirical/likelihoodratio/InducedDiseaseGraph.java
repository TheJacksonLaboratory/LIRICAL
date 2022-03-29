package org.monarchinitiative.lirical.likelihoodratio;


import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoSubOntologyRootTermIds;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * For some calculations of the phenotype likelihood ratio, we need to traverse the graph induced by the HPO terms to
 * which a disease is annotated. It is cheaper to create this graph once and reuse it for each of the query terms. This
 * class organizes that calculation. Note that this class is only used if there are no direct matches, so there is
 * no need to store the directly annotated diseases here.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class InducedDiseaseGraph {

    private final HpoDisease disease;
    private final Map<TermId, Double> term2frequencyMap;

    /**
     * If a disease is negative for say Abnormal serum creatinine kinase level
     * and the parent term Elevated serum creatinine kinase, was excluded in
     * the patient, then we can say that the patient must be negative for all of the
     * children of Abnormal serum creatinine kinase. We can therefore tak the induced
     * ancestor graph of Abnormal serum creatinine kinase level (which includes
     * Abnormal serum creatinine kinase), and if any of the patient negated terms are
     * in this graph, then they are excluded both in the patient and in the disease.
     */
    private final Set<TermId> inducedNegativeGraph;

    /**
     * An inner class that represents a term together with the minimum path length to any
     * term that directly annotates {@link #disease}.
     */
    record CandidateMatch(TermId termId, int distance) {
    }

    public static InducedDiseaseGraph create(HpoDisease disease, Ontology ontology) {
        Map<TermId, Double> termFrequencies = new HashMap<>(disease.getNumberOfPhenotypeAnnotations());

        for (HpoAnnotation annot: disease.getPhenotypicAbnormalities()) {
            double f = annot.getFrequency();
            CandidateMatch cmatch = new CandidateMatch(annot.id(), 0); // distance is zero
            Stack<CandidateMatch> stack = new Stack<>();
            stack.push(cmatch);
            while (!stack.empty()) {
                CandidateMatch cm = stack.pop();
                Set<TermId> parents = OntologyAlgorithm.getParentTerms(ontology, cm.termId,false);
                for (TermId parentTermId : parents) {
                    if (parentTermId.equals(HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY)) {
                        continue;
                    }
                    int distance = cm.distance + 1;
                    double adjustedFrequency = f / Math.pow(10.0, distance);
                    // Store the adjustedFrequency if no frequency is associated with termId.
                    // Otherwise, choose the greater frequency.
                    termFrequencies.compute(parentTermId, (termId, freq) -> (freq == null)
                            ? adjustedFrequency
                            : Math.max(freq, adjustedFrequency));
                    stack.push(new CandidateMatch(parentTermId, distance));
                }
            }
        }
        Set<TermId> negativeInducedGraph = OntologyAlgorithm.getAncestorTerms(ontology, Set.copyOf(disease.getNegativeAnnotations()), true);

        return new InducedDiseaseGraph(disease, termFrequencies, negativeInducedGraph);
    }

    /**
     * Create the induced graph of the HPO terms used to annotate the disease. We weight the frequency downwards
     * according to the number of links (path length). That is, if the path length from a direct annotation to
     * an ancestor is k, then we multiple the frequency of the annotation by (1/k).
     * @param hpoDisease The disease we are currently investigating.
     * @param term2frequencyMap
     * @param ancestorTerms
     */
    public InducedDiseaseGraph(HpoDisease hpoDisease,
                               Map<TermId, Double> term2frequencyMap,
                               Set<TermId> ancestorTerms) {
        this.disease = hpoDisease;
        this.term2frequencyMap = term2frequencyMap;
        this.inducedNegativeGraph = ancestorTerms;
    }

    /**
     * See comments about {@link #inducedNegativeGraph}.
     * @param tid A term that was negated in a patient
     * @return true if the term is also negated in the disease.
     */
    public boolean isExactExcludedMatch(TermId tid) { return this.inducedNegativeGraph.contains(tid); }

    public HpoDisease getDisease() {
        return disease;
    }


    /**
     * Get the terms that annotate the disease (or is an ancestor of one of the terms) that are
     * closest to tid in terms of path length. Return the best hits (list if more than one
     * terms has the closest path length
     * @param tid a query term
     * @param ontology HPO
     * @return The best hit
     */
    Term2Freq getClosestAncestor(TermId tid, Ontology ontology) {
        Queue<TermId> queue = new LinkedList<>();
        queue.add(tid);

        while (!queue.isEmpty()) {
            TermId t = queue.remove();
            if (term2frequencyMap.containsKey(t)) {
                return new Term2Freq(t,term2frequencyMap.get(t));
            } else {
                Set<TermId> parents = OntologyAlgorithm.getParentTerms(ontology, t,false);
                queue.addAll(parents);
            }
        }

       // if we get here, then something wrong has happened, but we did not find any intersection between the query
        // term and the disease. Return a term that represents the root of the Phenotype ontology
        // The frequency of the root is taken to be 1.0

        return new Term2Freq(HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY, 1.0);
    }



}
