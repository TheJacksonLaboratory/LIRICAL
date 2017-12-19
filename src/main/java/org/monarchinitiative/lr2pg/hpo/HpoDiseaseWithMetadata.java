package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.graph.data.DirectedGraph;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Pair;

import java.util.*;


/**
 * Model of a disease from the HPO annotations. This is an extension of HpoDisease and will be replaced in ontolib
 *
 * <p>
 * The main purpose here is to separate phenotypic abnormalities from mode of inheritance and other
 * annotations.
 * </p>
 *
 * @author <a href="mailto:manuel.holtgrewe@bihealth.de">Manuel Holtgrewe</a>
 * @author <a href="mailto:sebastian.koehler@charite.de">Sebastian Koehler</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.2.1 (2017-11-16)
 */
public final class HpoDiseaseWithMetadata {
    private static final Logger logger = LogManager.getLogger();
    /** Name of the disease from annotation. */
    private final String name;

    private final String diseaseDatabaseId;

    /** {@link TermId}s with phenotypic abnormalities and their frequencies. */
    private final List<TermIdWithMetadata> phenotypicAbnormalities;

    /** {@link TermId}s with mode of inheritance and their frequencies. */
    private final List<TermId> modesOfInheritance;


    private final List<TermId> negativeAnnotations;

    public String getDiseaseDatabaseId() {
        return diseaseDatabaseId;
    }

    /**
     * Constructor.
     *
     * @param name Name of the disease.
     * @param phenotypicAbnormalities {@link List} of phenotypic abnormalities with their frequencies.
     * @param modesOfInheritance {@link List} of modes of inheritance with their frequencies.
     */
    public HpoDiseaseWithMetadata(String name,
                      String databaseId,
                      List<TermIdWithMetadata> phenotypicAbnormalities,
                      List<TermId> modesOfInheritance,
                      List<TermId> notTerms) {
        this.name = name;
        this.diseaseDatabaseId=databaseId;
        this.phenotypicAbnormalities = ImmutableList.copyOf(phenotypicAbnormalities);
        this.modesOfInheritance = ImmutableList.copyOf(modesOfInheritance);
        this.negativeAnnotations = ImmutableList.copyOf(notTerms);
    }

    /**
     * @return The name of the disease.
     */
    public String getName() {
        return name;
    }

    /**@return the count of the non-negated annotations excluding mode of inheritance. */
    public int getNumberOfPhenotypeAnnotations() { return this.phenotypicAbnormalities.size(); }

    /**
     * @return The list of frequency-annotated phenotypic abnormalities.
     */
    public List<TermIdWithMetadata> getPhenotypicAbnormalities() {
        return phenotypicAbnormalities;
    }

    /**
     * @return The list of frequency-annotated modes of inheritance.
     */
    public List<TermId> getModesOfInheritance() {
        return modesOfInheritance;
    }


    public List<TermId> getNegativeAnnotations() { return this.negativeAnnotations;}

    /**
     * Users can user this function to get the TermIdWithMetadata corresponding to a TermId
     * @param id
     * @return corresponding {@link TermIdWithMetadata} or null if not present.
     */
    public TermIdWithMetadata getTermIdWithMetadata(TermId id) {
        return phenotypicAbnormalities.stream().filter( timd -> timd.getTermId().equals(id)).findAny().orElse(null);
    }


    private int dijkstra2pathlength(TermId source, TermId target,DirectedGraph dag, Set<TermId> candidates) {
        SortedMap<TermId,Integer> vertex2distance= new TreeMap<>();
        for (TermId t : candidates) {
            vertex2distance.put(t,Integer.MAX_VALUE);
        }
        vertex2distance.put(source,0);
        TermId u = source;
        while (! vertex2distance.isEmpty()) {
            u = vertex2distance.firstKey(); // sorted map! This gets the least key (lowest distance)
            int dist_u = vertex2distance.get(u);
            int dist_v = dist_u + 1; // each vertex v has a path length of 1 to u
            vertex2distance.remove(u);
            java.util.Iterator<HpoTermRelation> it = dag.outEdgeIterator(u);
            HpoTermRelation edge = it.next();
            TermId vertex = edge.getDest();
            if (vertex.equals(target)) {
                // we found the path, and the overall length of the path if dist_v
                return dist_v;
            }
            vertex2distance.put(vertex, dist_v);
        }
        // We should never get here, but...
        logger.fatal("Should never happen--we failed to find shortest path although one must exist");
        return Integer.MAX_VALUE;
    }




    public Pair<TermIdWithMetadata, Integer> getMICA(TermId queryTerm, Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology) {
        DirectedGraph dag = phenotypeSubOntology.getGraph();
        // want to get the collection of vertices that is all of the ancestors of our query term and all of the
        // ancestors or the disease terms.
        ImmutableSet.Builder isb = new ImmutableSet.Builder();
        isb.addAll(phenotypeSubOntology.getAncestorTermIds(queryTerm));
        for (TermId ptid : phenotypicAbnormalities) {
            isb.addAll(phenotypeSubOntology.getAncestorTermIds(ptid));
        }
        ImmutableSet<TermId> allAncestors = isb.build();

        for (TermIdWithMetadata ptid : phenotypicAbnormalities) {
            if (phenotypeSubOntology.getAncestorTermIds(ptid).contains(queryTerm)) {
                // the query term is an ancestor of this disease annotation
                // we now want to know the path length that separates the two terms.
                // we know that the path starts at ptid and ends at query term since
                // is_a links point from descendant to ancestor
                // we have found a MICA that is an ancestor of both the query term and the disease terms
                // Use Dijkstra to get path length
                int k = dijkstra2pathlength(ptid, queryTerm, dag, allAncestors);
                return new Pair<>(ptid, k);
            }
        }
        return null;
    }



    /**
     * Decide if a term is directly or indirectly annotated to the query term tid. Note if a disease
     * is annotated to nuclear cataract and the query term is cataract, then the disease is considered to
     * be implicitly annotated to cataract by the true path rule
     * @param tid an Hpo term
     * @return a {@link TermIdWithMetadata} if this disease is directly or indirectly (true path rule) annotated to the tid, otherwise null
     */
    public Pair<TermIdWithMetadata, Integer> getMICAandPathLength(TermId tid, Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology) {
        TermIdWithMetadata timd = phenotypicAbnormalities.stream().filter( tmd -> tmd.getTermId().equals(tid)).findAny().orElse(null);
        if (timd != null)
            return new Pair(timd,1); // relatively fast first try--look for exact match
        else
            return getMICA(tid, phenotypeSubOntology);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TermIdWithMetadata t : phenotypicAbnormalities) {
            sb.append("\t" + t + "\n");
        }

        return "HpoDisease [name=" + name + ", phenotypicAbnormalities=\n" + sb.toString()
                + ", modesOfInheritance=" + modesOfInheritance + "]";
    }

}

