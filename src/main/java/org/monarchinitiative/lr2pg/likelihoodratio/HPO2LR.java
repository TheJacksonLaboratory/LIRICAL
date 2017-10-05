package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.prototype.Disease;
import org.apache.log4j.Logger;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;


/**
 * Retrieves data needed to perform a likelihood ratio test with HPO Phenotype data.
 * TODO description
 * @author Vida Ravanmehr
 * @version 0.0.2 (09/20/2017)
 */
public class HPO2LR {

    Ontology<HpoTerm, HpoTermRelation> hpoOntology;
    Map<String, Disease> diseaseMap;


    Map<TermId, Integer> hpoTerm2DiseaseCount = null;

    static Logger logger = Logger.getLogger(HPO2LR.class.getName());

    public HPO2LR(Ontology<HpoTerm, HpoTermRelation> ontology, Map<String, Disease> diseaseMp) {
        hpoOntology = ontology;
        diseaseMap = diseaseMp;
        try {
            initializeTerm2DiseaseMap();
        } catch (Exception e) {
            System.err.println(e);
        }
    }


    /**
     * This function counts the number of diseases that are annotated to each HPO term, including
     * implicited (inherited) annotations, and places the result in {@link #hpoTerm2DiseaseCount}.
     * TODO convert into Java8 stream
     */
    /*private void initializeTerm2DiseaseMap() {
        hpoTerm2DiseaseCount = new HashMap<>();
        logger.trace("start creating the map hpoTerm, Disease Count");
        int good=0,bad=0;
        for(Disease disease: diseaseMap.values()){
            System.err.println(String.format("Disease %s", disease.getName()));
            for (TermId termId : disease.getHpoIds()) {
                System.err.println(String.format("Term %s Status %s",
                        termId.getIdWithPrefix(), hpoOntology.getAllTermIds().contains(termId)));
            }
            try {
                Collection<TermId> ids = disease.getHpoIds();
                if (ids==null) {
                    System.err.println("TermIds NULL");
                    System.exit(1);
                } else if (ids.size()==0) {
                    System.err.println("TermIds zero size");
                    System.exit(1);
                }
                ids.remove(null);
                Set<TermId> ancestors = hpoOntology.getAllAncestorTermIds(ids);
                ancestors.remove(null);
                for (TermId hpoid : ancestors) {
                    if (hpoid==null) continue;
                    if (!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                        hpoTerm2DiseaseCount.put(hpoid, 1);
                    } else {
                        hpoTerm2DiseaseCount.put(hpoid, 1 + hpoTerm2DiseaseCount.get(hpoid));
                    }
                }
                good++;
            } catch (Exception e) {
                logger.error("Could not get terms for disease "+ disease.getName());
                Collection<TermId> ids = disease.getHpoIds();



                logger.error(e,e);
                bad++;
                System.exit(1);
            }

        }
        logger.trace("Good disease parses "+good+"' bad="+bad);

        /*for(Disease disease: diseaseMap.values()){
                System.err.println(String.format("Trying to get HPO id %s", disease.getHpoIds()));
            Set<TermId> ancestors = hpoOntology.getAllAncestorTermIds(disease.getHpoIds());
            for(TermId hpoid : ancestors) {
                if(!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                    hpoTerm2DiseaseCount.put(hpoid, 1);
                } else {
                     hpoTerm2DiseaseCount.put(hpoid, 1+hpoTerm2DiseaseCount.get(hpoid));
                }
            }
        }*/

//        Map<TermId, Long> m =diseaseMap.values().
//                stream().
//                map( disease -> hpoOntology.getAllAncestorTermIds(disease.getHpoIds())).
//                collect(Collectors.groupingBy(t -> t, counting()));


    //}

    public void debugPrintDiseaseMap() {
        for (String d: diseaseMap.keySet()) {
            Disease disease = diseaseMap.get(d);
            System.err.println(String.format("Disease: %s: HPO ids: %d",disease.getName(),disease.getHpoIds().size()));
        }


    }


    private void initializeTerm2DiseaseMap() throws Exception {
        hpoTerm2DiseaseCount = new HashMap<>();
        int good=0,bad=0;
        for(Disease disease: diseaseMap.values()){
            System.err.println(String.format("Disease %s", disease.getName()));
            for (TermId termId : disease.getHpoIds()) {
                System.err.println(String.format("Term %s Status %s",
                        termId.getIdWithPrefix(), hpoOntology.getAllTermIds().contains(termId)));
            }

            Collection<TermId> ids = disease.getHpoIds();
            if (ids==null) {
                String msg="TermIds NULL";
                throw new Exception(msg);
            } else if (ids.size()==0) {
                System.err.println("TermIds zero size");
                System.err.println("disease: " + disease.getName());
                debugPrintDiseaseMap();
                //System.exit(17);
                String msg = String.format("Disease %s had zero HpoIds",disease.getName());
                throw new Exception(msg);
            }
            ids.remove(null);
            Set<TermId> ancestors = hpoOntology.getAllAncestorTermIds(ids);
            ancestors.remove(null);
            for (TermId hpoid : ancestors) {
                if (hpoid==null) continue;
                if (!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                    hpoTerm2DiseaseCount.put(hpoid, 1);
                } else {
                    hpoTerm2DiseaseCount.put(hpoid, 1 + hpoTerm2DiseaseCount.get(hpoid));
                }
            }
            good++;


        }


    }


    /**
     * Returns the frequency of an HPO annotation among all diseases of our corpus, i.e., in {@link #diseaseMap}.
     * @param hpoId The HPO Term whose frequency we want to know
     * @return frequency of hpoId among all diseases
     */
    private double getBackgroundFrequency(TermId hpoId) {
        int NumberOfDiseases = diseaseMap.size();
        logger.trace("return number of disease that have the HPO term");
        if (hpoTerm2DiseaseCount.containsKey(hpoId)) { // If the hpoTerm2DiseaseCount contains the HPO term
            return hpoTerm2DiseaseCount.get(hpoId) / NumberOfDiseases; //return number of diseases wit HPO term divided by total number of diseases
        } else {
            return 0;
        }
    }

    /**
     *  If disease has the HPO term, return 0.9; else return 0 (initial approach/simplification)
     *  TODO later calculate the actual frequency if possible
     * @param diseaseID
     * @param hpoId
     * @return The frequency of HPO feature (hpoId) in patients with the given disease
     */
    private double getFrequency(String diseaseID, TermId hpoId) {
        Disease disease1 = diseaseMap.get(diseaseID);
        if (disease1 != null && disease1.getHpoIds().contains(hpoId))
            return 0.9;
        else
            return 0.0;
    }


}