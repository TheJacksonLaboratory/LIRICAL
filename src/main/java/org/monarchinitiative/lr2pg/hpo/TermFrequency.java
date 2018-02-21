package org.monarchinitiative.lr2pg.hpo;



import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.github.phenomics.ontolib.ontology.algo.OntologyAlgorithm.*;

/**
 * Created by ravanv on 2/20/18.
 */

public class TermFrequency {
    private static final Logger logger = LogManager.getLogger();
    /**
     * Path to the {@code hp.obo} file.
     */
    private String hpoOboFilePath;
    /**
     * Path to the {@code phenotype_annotation.tab} file.
     */
    private String hpoPhenotypeAnnotationPath;
    /**
     * The subontology of the HPO with all the phenotypic abnormality terms.
     */
    private Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology = null;
    /**
     * The subontology of the HPO with all the inheritance terms.
     */
    private Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology = null;
    /**
     * This map has one entry for each disease in our database.
     */
    private Map<String, HpoDiseaseWithMetadata> diseaseMap;

    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;

    private String  IDENTICAL = "Identical";

    private String SUPERCLASS = "Superclass";

    private String SUBCLASS = "Subclass";

    private String SIBLINGS = "Sibling";

    private String RELATED = "Related";

    public TermFrequency(Ontology<HpoTerm, HpoTermRelation> pheno,
                         Ontology<HpoTerm, HpoTermRelation> inheri,
                         Map<String, HpoDiseaseWithMetadata> diseases) {
        phenotypeSubOntology = pheno;
        inheritanceSubontology = inheri;
        this.diseaseMap = diseases;
        initializeFrequencyMap();
    }

    /**
     * Initialize the {@link #hpoTerm2OverallFrequency} object that has the background frequencies of each of the
     * HPO terms in the ontology.
     */
    private void initializeFrequencyMap() {
        Map<TermId, Double> mp = new HashMap<>();
        for (TermId tid : this.phenotypeSubOntology.getTermMap().keySet()) {
            mp.put(tid, 0.0D);
        }
        ImmutableMap.Builder<TermId, Double> imb = new ImmutableMap.Builder<>();
        for (HpoDiseaseWithMetadata dis : this.diseaseMap.values()) {
            for (TermIdWithMetadata tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid = tidm.getTermId();
                // tid = phenotypeSubOntology.getTermMap().get(tid).getId(); //replace with current id if needed
                if (!mp.containsKey(tid)) {
                    mp.put(tid, 0.0);
                }
                double delta = tidm.getFrequency().upperBound();
                Set<TermId> ancs = this.phenotypeSubOntology.getAncestorTermIds(tid);
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
        logger.trace("Got data on background frequency for " + hpoTerm2OverallFrequency.size() + " terms");
    }
    private int getNumberOfDiseases() {
        return diseaseMap.size();
    }

    private final static double DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY = 0.000_005; // 1:20,000


    /*
    We need a function that gets the best matching term for a query term TID so that we can calculate frequency of an arbitrary term in an arbitrary disease.
    Priority list

    1. TID is identical with a term in the disease annotations
    2. TID is a (direct) parent term
    3. TID is a (direct) child term
    4. TID is a sibling of a term in the disease annotations
    5. TID is related to a term in the disease annotations
    6. TID is unrelated to any term in the disease annotations
     */
    private double getFrequencyTerm(TermId query, String diseaseName){
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);

      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if ( tid.equals(query) )  return getAdjustedFrequency(tid,diseaseName,IDENTICAL);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if ( isSubclass( phenotypeSubOntology,  tid, query) )  return getAdjustedFrequency(tid,diseaseName,SUPERCLASS);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (isSubclass(phenotypeSubOntology,query,tid))  return getAdjustedFrequency(tid,diseaseName,SUBCLASS);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (termsAreSiblings(phenotypeSubOntology,query,tid))  return getAdjustedFrequency(tid,diseaseName,SIBLINGS);
      }

      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (termsAreRelated(phenotypeSubOntology,query,tid))  return getAdjustedFrequency(tid,diseaseName,RELATED);
      }
      return   DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

  }

   private double getAdjustedFrequency(TermId tid, String diseaseName, String relation){
      HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
      TermIdWithMetadata timd = disease.getTermIdWithMetadata(tid);

      switch (relation) {
          case "Identical":
              return timd.getFrequency().upperBound();
          case "Superclass":
              return getFrequencySuperclassTerm(tid, diseaseName);
          case "Subclass":
              return getFrequencySubclassTerm(tid, diseaseName);
          case "Sibling":
              return getSiblingsTermsFrequency(tid, diseaseName);

          case "Related":
              return getRelatedTermsFrequency(timd,diseaseName);
          default:
              return   DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

      }

  }

    /**
     * If the query term is a (direct)subclass of a term of disease, we divide the frequency of term by the number of children.
     * @param tid: TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getFrequencySubclassTerm(TermId tid, String diseaseName) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
        TermIdWithMetadata timd = disease.getTermIdWithMetadata(tid);
        int level = 2;

        int NumberOfChildren = getChildTerms(phenotypeSubOntology,timd).size();
        return timd.getFrequency().upperBound()/(NumberOfChildren * Math.log(level));


    }

    /**
     * If the query term is a (direct)superclass of a term of disease, then we need to add frequency terms of all children (maybe with some changes)
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getFrequencySuperclassTerm(TermId tid, String diseaseName) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);

        double prob =0;
        Set<TermId> children = getChildTerms(phenotypeSubOntology, tid);
        for(TermId id:children) {
            prob += getFrequencyTerm(id, diseaseName);
        }
        return prob;


    }

    /**
     * If two terms are siblings, we first find their parent, then we divide the frequency of the parent by the number of children that it has.
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getSiblingsTermsFrequency(TermId tid, String diseaseName) {
        HpoDiseaseWithMetadata disease = diseaseMap.get(diseaseName);
        Set<TermId> parents =  getParentTerms(phenotypeSubOntology,tid);
        if(parents.size() == 1){
            //Access to child needs to be checked!
            int NumberOfChildren = getChildTerms(phenotypeSubOntology,parents).size();
            parents.iterator().next();
            return getAdjustedFrequency(parents.iterator().next(),diseaseName,SUPERCLASS)/NumberOfChildren;
        }
        //what if the term has more than 1 parent? TODO find a good estimate when there is more than 1 parent
        else{
            return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;
        }

    }

    /**
     * Need to find a good estimate??
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getRelatedTermsFrequency(TermId tid, String diseaseName) {

        return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

    }


}
