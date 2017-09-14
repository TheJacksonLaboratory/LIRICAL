package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.prototype.Disease;

import java.util.HashMap;
import java.util.Map;

public class HPO2LR {

    Ontology<HpoTerm, HpoTermRelation> hpoOntology;
    Map<String, Disease> diseaseMap;

    Map<TermId,Integer> hpoTerm2DiseaseCount=null;



    public HPO2LR(Ontology<HpoTerm, HpoTermRelation> ontology, Map<String, Disease> diseaseMp) {
        hpoOntology=ontology;
        diseaseMap=diseaseMp;
        initializeTerm2DiseaseMap();
    }



    private void initializeTerm2DiseaseMap() {
        hpoTerm2DiseaseCount=new HashMap<>();
        // todo
    }


    public double getBackgroundFrequency(TermId hpoId) {
        // return total number of diseases with HPO term divided by total number of diseases

        return 0;
    }



    public double getFrequency(String diseaseID, TermId hpoId) {
        // if disease has term return 0.90
        // else return 0

        return 0.0;
    }
}
