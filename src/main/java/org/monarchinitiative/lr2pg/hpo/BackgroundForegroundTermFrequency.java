package org.monarchinitiative.lr2pg.hpo;



import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.formats.hpo.HpoTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import static org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm.*;

/**
 * This class is designed to calculate the background and foreground frequencies of any HPO term in any disease.
 * @author <a href="mailto:vida.ravanmehr@jax.org">Vida Ravanmehr</a>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class BackgroundForegroundTermFrequency {
    private static final Logger logger = LogManager.getLogger();
    /** Path to the {@code hp.obo} file.*/
    private String hpoOboFilePath;
    /** Path to the {@code phenotype.hpoa} version 2 phenotype annotation file.*/
    private String hpoPhenotypeAnnotationPath;
    /** The HPO ontology with all of its subontologies. */
    private final HpoOntology ontology;
    /** This map has one entry for each disease in our database. */
    private final Map<String, HpoDisease> diseaseMap;
    /** Overall, i.e., background frequency of each HPO term. */
    private ImmutableMap<TermId, Double> hpoTerm2OverallFrequency = null;

    private final static TermId PHENOTYPIC_ABNORMALITY = ImmutableTermId.constructWithPrefix("HP:0000118");

    private String  IDENTICAL = "Identical";

    private String SUPERCLASS = "Superclass";

    private String SUBCLASS = "Subclass";

    private String SIBLINGS = "Sibling";

    private String RELATED = "Related";

    public BackgroundForegroundTermFrequency(HpoOntology onto,
                                             Map<String, HpoDisease> diseases) {
        this.ontology=onto;
        this.diseaseMap = diseases;
        initializeFrequencyMap();
    }


    /** @return iterator over the diseases in the database.*/
    public Iterator<HpoDisease> getDiseaseIterator() {
        return this.diseaseMap.values().iterator();
    }


    public double getLikelihoodRatio(TermId tid, String diseaseName) {

        HpoDisease disease = diseaseMap.get(diseaseName);
        if (disease==null) {
//            logger.fatal("Could not find disease %s in diseaseMap. Terminating...",diseaseName);
            System.exit(1);
        }
        double numerator=getFrequencyOfTermInDisease(diseaseName,tid);
        double denominator=getBackgroundFrequency(tid);
        return numerator/denominator;

    }

    public double getFrequencyOfTermInDisease(String diseaseName, TermId term) {
        HpoDisease disease = diseaseMap.get(diseaseName);
        if (disease==null) {
//            logger.fatal("Could not find disease %s in diseaseMap. Terminating...",diseaseName);
            System.exit(1);
        }
        HpoTermId timd = disease.getTermIdWithMetadata(term);
        if (timd==null) {
            // this disease does not have the Hpo term in question
            return getFrequencyIfNotAnnotated(term,diseaseName);
        } else {
            return timd.getFrequency();
        }
    }

    private double getFrequencyIfNotAnnotated(TermId tid, String diseaseName) {
        if (ontology.getTermMap().get(tid)==null) {
//            logger.error("Could not get term for "+tid.getIdWithPrefix());
//            logger.error("phenotypeSubOntology size "+ontology.getTermMap().size());
            //System.exit(1);
            return 0.001;
        }
        tid = ontology.getPrimaryTermId(tid);// make sure we have current tid
        int level = 0;
        double prob = 0;
        boolean foundannotation=false;
        HpoDisease disease = diseaseMap.get(diseaseName);
        Set<TermId> currentlevel=new HashSet<>();
        currentlevel.add(tid);
        while (! currentlevel.isEmpty()) {
            level++;
            Set<TermId> parents =  getParentTerms(ontology,currentlevel);
            for (TermId id : parents) {
                if (ontology.isRootTerm(id)) { // to do replace with Abnormal Phenotype root term id.
                    break;
                }
                HpoTermId timd = disease.getTermIdWithMetadata(id);
                if (timd != null) {
                    prob += timd.getFrequency() / (1 + Math.log(level)); //penalty for imprecision,
                    foundannotation=true;
                }
            }
            if (foundannotation) {
                return prob;
            } else {
                currentlevel=parents;
            }
        }
        // if we get here, then we are at the root.
        // this means that the disease has no annotations in the same section (e.g., cardiology) of the HPO
        // Therefore, the current term is not at all typical for the disease and is probably just a false positive
        // (there is a smaller chance that our annotations are incomplete)
        return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

    }


    /**
     * This function estimates the probability of a test finding (the HP term is present) given that the
     * disease is not present -- we call this the background frequency.
     * @return the estimate background frequency (note: bf \in [0,1])
     */
    double getBackgroundFrequency(TermId termId) {
        if (termId instanceof HpoTermId) {
            termId= ((HpoTermId) termId).getTermId();
        }
        if (! hpoTerm2OverallFrequency.containsKey(termId)) {
//            logger.fatal(String.format("Map did not contain data for term %s",termId.getIdWithPrefix() ));
            // todo throw error
            System.exit(1);
        }
        return hpoTerm2OverallFrequency.get(termId);
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
            for (HpoTermId tidm : dis.getPhenotypicAbnormalities()) {
                TermId tid = tidm.getTermId();
                if (!mp.containsKey(tid)) {
                    mp.put(tid, 0.0);
                }
                double delta = tidm.getFrequency();
                // All of the ancestor terms are implicitly annotated to tid
                // therefore, get all of the strict ancestors and add this to their background frequencies.
                Set<TermId> ancs = getAncestorTerms(ontology,tid,false);
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
    int getNumberOfDiseases() {
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
        HpoDisease disease = diseaseMap.get(diseaseName);

      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if ( tid.equals(query) )  return getAdjustedFrequency(tid,query,diseaseName,IDENTICAL);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if ( isSubclass( ontology,  tid, query) )  return getAdjustedFrequency(tid,query,diseaseName,SUPERCLASS);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (isSubclass(ontology,query,tid))  return getAdjustedFrequency(tid,query,diseaseName,SUBCLASS);
      }
      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (termsAreSiblings(ontology,query,tid))  return getAdjustedFrequency(tid,query,diseaseName,SIBLINGS);
      }

      for (TermId tid : disease.getPhenotypicAbnormalities()) {
          if (termsAreRelated(ontology,query,tid))  return getAdjustedFrequency(tid,query,diseaseName,RELATED);
      }
      return   DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

  }

   private double getAdjustedFrequency(TermId tid, TermId queryTerm, String diseaseName, String relation){
       HpoDisease disease = diseaseMap.get(diseaseName);
       HpoTermId timd = disease.getTermIdWithMetadata(tid);

      switch (relation) {
          case "Identical":
              return timd.getFrequency();
          case "Superclass":
              return getFrequencySuperclassTerm(tid, queryTerm, diseaseName);
          case "Subclass":
              return getFrequencySubclassTerm(tid, queryTerm, diseaseName);
          case "Sibling":
              return getSiblingsTermsFrequency(tid, queryTerm,diseaseName);
          case "Related":
              return getRelatedTermsFrequency(tid, queryTerm, diseaseName);
          default:
              return   DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;
      }
  }

    /**
     * If the query term is a superclass of a term of disease, then the frequency of the term will be the maximum of the frequency of its children.
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getFrequencySuperclassTerm(TermId tid, TermId query, String diseaseName) {
        double prob =0;
        //Needds to be completed.
        return prob;
    }


    /**
     * If the query term is a subclass of a term of disease, we divide the frequency of term by 1+log(level).
     * @param tid: TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getFrequencySubclassTerm(TermId tid, TermId query, String diseaseName) {
        HpoDisease disease = diseaseMap.get(diseaseName);
        HpoTermId timd = disease.getTermIdWithMetadata(tid);
        double prob = 0;
        //Needs to be completed.
        return prob;
    }




    /**
     * If two terms are siblings, we divide the frequency of the term by (1+log(level)).
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getSiblingsTermsFrequency(TermId tid, TermId queryTerm, String diseaseName) {
        //Needs to be completed.
        double prob = 0;
        return prob;

    }

    /**
     * If two terms are related, we divide the frequency of the disease term Id by 1+log(level)
     *
     * @param tid:TermId
     * @param diseaseName:Disease
     * @return frequency
     */
    private double getRelatedTermsFrequency(TermId tid, TermId query, String diseaseName) {
        int level = 0;
        Set<TermId> currentlevel=new HashSet<>();
        currentlevel.add(tid);
        while (! currentlevel.isEmpty()) {
            level++;
            Set<TermId> parents =  getParentTerms(ontology,currentlevel);
            for (TermId id : parents) {
                if (id.equals(PHENOTYPIC_ABNORMALITY)) {
                    break;
                }
                Set<TermId>children = getChildTerms(ontology,id);
                if (children.contains(tid)){
                   return (  getSiblingsTermsFrequency( tid, query, diseaseName) /(1 + Math.log(level)));
                }
            }
            currentlevel=parents;
        }

        return DEFAULT_FALSE_POSITIVE_NO_COMMON_ORGAN_PROBABILITY;

    }


}
