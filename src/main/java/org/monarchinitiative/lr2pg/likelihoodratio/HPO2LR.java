package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.prototype.Disease;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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


    private void initializeTerm2DiseaseMap() {
        hpoTerm2DiseaseCount = new HashMap<>();
        // todo
        int CounterHPOTerms = 0; //Counter for counting HPO terms of a disease
        int NumberOfHpoDiseases = 0;//Number of diseases with the HPO term (HPOID)
        TermId HPOID = null;
        Disease disease = null;
        logger.trace("start creating the map hpoTerm, Disease Count");
        for(String Disease_ID: diseaseMap.keySet()){ //search among all diseases
            disease = diseaseMap.get(Disease_ID);
            for(CounterHPOTerms = 0; CounterHPOTerms < disease.getHpoIds().size(); ++CounterHPOTerms) {//Search among all HPO terms of a disease
                HPOID = disease.getHpoIds().get(CounterHPOTerms); // Get an HPOID
                if(!hpoTerm2DiseaseCount.containsKey(HPOID)) { // If the HPOID does not exist in the hpoTem2DiseaseCount, which means that it is the first time that tha the HPO term appears in a disease
                    hpoTerm2DiseaseCount.put(HPOID, 1);
                }
                    else{ //increase the number of diseases that has the HPO term by one
                     NumberOfHpoDiseases =  hpoTerm2DiseaseCount.get(HPOID);
                     hpoTerm2DiseaseCount.put(HPOID, ++NumberOfHpoDiseases);
                }
            }
        }
       // System.out.print(hpoTerm2DiseaseCount);
    }


    public double getBackgroundFrequency(TermId hpoId) {
        int NumberOfDiseases = 7000;// fix the number of diseases or it should be equal to the size of diseaseMap???//
        logger.trace("return number of disease that have the HPO term");
        if (hpoTerm2DiseaseCount.containsKey(hpoId)) { // If the hpoTerm2DiseaseCount contains the HPO term
            return hpoTerm2DiseaseCount.get(hpoId) / NumberOfDiseases; //return number of diseases wit HPO term divided by total number of diseases
        } else {
            return 0;
        }
    }


    public double getFrequency(String diseaseID, TermId hpoId) {
        Disease disease1 = null;
        disease1 = diseaseMap.get(diseaseID);
        if (disease1 != null && disease1.getHpoIds().contains(hpoId)) // If disease has the HPO term, return 0.9; else return 0
            return 0.9;
        else
            return 0.0;
    }
}