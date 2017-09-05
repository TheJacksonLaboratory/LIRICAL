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
        //Vida
        double SumOfFreq = 0; //Sum of frequencies of an HPO term in diseases//
        int NumberOfDiseases = 7000; //assume number of diseases is 7000//
        double LR = 0; //Likelihood ratio//
        //End
        Optional<Float> freq= hpoa.getFrequency();
        if (freq.isPresent()) {
            System.out.println("do something");
            //Added by Vida
            //Calculate the LR by dividing freq by (1/N * \sum_{i=1}^N frequencies), where N is the number of diseases
            TermId hopID = hpoa.getHpoId();
            int Counter = 0;
            while(Counter < annotList.size()){
                HpoDiseaseAnnotation hpoa_temp = annotList.get(Counter);
                TermId hpoID_temp = hpoa_temp.getHpoId();
                if(hpoID_temp.equals(hopID)) {
                    Optional<Float> freq_temp = hpoa_temp.getFrequency();
                    if(freq_temp.isPresent()) {
                        SumOfFreq += freq_temp.get();
                    }
                }
                ++Counter;
            }

            if (SumOfFreq != 0 ){
                LR = NumberOfDiseases * freq.get() / SumOfFreq;
                System.out.println("The likelihood ratio is " + LR);

            }

        }


        String md =hpoa.getFrequencyModifier();
        if ( md !=null) {
            System.out.println("do something with modifiers");
            //Added by Vida
            SumOfFreq = 0;
            LR = 0;
            double freqModifier = 0;
            if (md.equals( "70%")) {
                 freqModifier = 0.7;
            }
            else if(md.equals("12 of 30")){
                 freqModifier = 12/30;
            }
            //else if(Usually from the subontology Frequency)???

            TermId hpoID = hpoa.getHpoId();
            int Counter = 0;
            double freqModifier_tmp = 0;
            while(Counter < annotList.size()){
                HpoDiseaseAnnotation hpoa_temp = annotList.get(Counter);
                TermId hopID_temp = hpoa_temp.getHpoId();
                if(hopID_temp.equals(hpoID)) {
                    String md_tmp =hpoa.getFrequencyModifier();
                    if(md_tmp != null) {
                        if (md_tmp.equals("70%")) {
                            freqModifier_tmp = 0.7;
                        } else if (md_tmp.equals("12 of 30")) {
                            freqModifier_tmp = 12 / 30;
                        }
                    }
                    SumOfFreq += freqModifier_tmp;
                }
                ++Counter;
            }


            if(SumOfFreq != 0){
               LR = NumberOfDiseases * freqModifier  / SumOfFreq;
               System.out.println("The likelihood ratio is " + LR);
             }

             System.out.println("Counter =" + Counter + "SumOfFreq=" + SumOfFreq);
             //End (Vida)
        }
    }

}
