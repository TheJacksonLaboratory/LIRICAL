package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.Term;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HPOParser;
import org.monarchinitiative.lr2pg.io.CommandParser;

import java.util.*;

public class LR2PG {
    static Logger logger = Logger.getLogger(LR2PG.class.getName());
    private Ontology<HpoTerm, HpoTermRelation> ontology=null;
    /** List of all annotations parsed from phenotype_annotation.tab  */
    private List<HpoDiseaseAnnotation> annotList=null;


    static public void main(String [] args) {
        CommandParser parser= new CommandParser(args);
        String hpopath=parser.getHpoPath();
        String annotpath=parser.getAnnotationPath();
        logger.trace("starting");
        LR2PG lr2pg = new LR2PG(hpopath,annotpath);
        lr2pg.parseHPOData(hpopath,annotpath);
        lr2pg.debugPrintOntology();
        lr2pg.debugPrintAssociations();


    }


    public LR2PG(String hpo, String annotation) {
        parseHPOData(hpo,annotation);
    }


    private void parseHPOData(String hpo, String annotation) {
        HPOParser parser = new HPOParser();
       logger.trace("About to parse OBO file");
        this.ontology = parser.parseOntology(hpo);
        logger.trace("About to parse annot file");
        this.annotList = parser.parseAnnotation(annotation);
        logger.trace("number of non obsolete terms: " + ontology.getNonObsoleteTermIds().size());
    }


    private void debugPrintOntology() {
        logger.trace(this.ontology.getTerms().size() + " terms found in HPO");
        TermId rootID=ontology.getRootTermId();
        Collection<HpoTerm> termlist=ontology.getTerms();
        Map<TermId,HpoTerm> termmap=new HashMap<>();
        for (HpoTerm term:termlist) {
            termmap.put(term.getId(),term);
        }
        Term root = termmap.get(rootID);
        logger.trace("Root: " + root.toString());


    }

    private void debugPrintAssociations() {
        logger.trace(annotList.size() + " annotations");
        HpoDiseaseAnnotation hpoa = annotList.get(0);
        logger.trace(hpoa);
        Optional<Float> freq= hpoa.getFrequency();
        if (freq.isPresent()) {
            System.out.println("do something");
        }
        String md =hpoa.getFrequencyModifier();
        if ( md !=null) {
            System.out.println("do something with modifiers");
        }

    }


}
