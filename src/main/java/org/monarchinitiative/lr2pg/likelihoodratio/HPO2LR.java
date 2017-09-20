package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.prototype.Disease;
import org.apache.log4j.Logger;

import java.util.*;
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
        initializeTerm2DiseaseMap();
    }


    /**
     * This function counts the number of diseases that are annotated to each HPO term, including
     * implicited (inherited) annotations, and places the result in {@link #hpoTerm2DiseaseCount}.
     * TODO convert into Java8 stream
     */
    private void initializeTerm2DiseaseMap() {
        hpoTerm2DiseaseCount = new HashMap<>();
        logger.trace("start creating the map hpoTerm, Disease Count");
        for(Disease disease: diseaseMap.values()){
            Set<TermId> ancestors = hpoOntology.getAllAncestorTermIds(disease.getHpoIds());
            for(TermId hpoid : ancestors) {
                if(!hpoTerm2DiseaseCount.containsKey(hpoid)) {
                    hpoTerm2DiseaseCount.put(hpoid, 1);
                } else {
                     hpoTerm2DiseaseCount.put(hpoid, 1+hpoTerm2DiseaseCount.get(hpoid));
                }
            }
        }

//        Map<TermId, Long> m =diseaseMap.values().
//                stream().
//                map( disease -> hpoOntology.getAllAncestorTermIds(disease.getHpoIds())).
//                collect(Collectors.groupingBy(t -> t, counting()));


    }


    /**
     * Returns the frequency of an HPO annotation among all diseases of our corpus, i.e., in {@link #diseaseMap}.
     * @param hpoId The HPO Term whose frequency we want to know
     * @return frequency of hpoId among all diseases
     */
    public double getBackgroundFrequency(TermId hpoId) {
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
    public double getFrequency(String diseaseID, TermId hpoId) {
        Disease disease1 = diseaseMap.get(diseaseID);
        if (disease1 != null && disease1.getHpoIds().contains(hpoId))
            return 0.9;
        else
            return 0.0;
    }
}