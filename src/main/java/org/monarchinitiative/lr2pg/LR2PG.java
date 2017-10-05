package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.Term;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.prototype.Disease;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HPOParser;
import org.monarchinitiative.lr2pg.hpo.HPTerms;
import org.monarchinitiative.lr2pg.hpo.WriteResults;
import org.monarchinitiative.lr2pg.io.CommandParser;
import org.monarchinitiative.lr2pg.likelihoodratio.HPO2LR;
import org.monarchinitiative.lr2pg.likelihoodratio.LR;

import java.util.*;

public class LR2PG {
    static Logger logger = Logger.getLogger(LR2PG.class.getName());


    private Ontology<HpoTerm, HpoTermRelation> ontology=null;
    /** Map of all of the Phenotypic abnormality terms (i.e., not the inheritance terms). */
    private Map<TermId,HpoTerm> termmap=null;
    Ontology<HpoTerm, HpoTermRelation> inheritance=null;
    /** List of all annotations parsed from phenotype_annotation.tab. */
    private List<HpoDiseaseAnnotation> annotList=null;
    private static HPOParser parserHPO=null;
    private static WriteResults writeResults = null;
    private static LR HPLikelihood = null;
    private static List<HpoDiseaseAnnotation> annotations=null;
    /** Map of HPO Term Ids and the number of diseases that has the HPO term */
    private static Map<TermId, Integer> hpoTerm2DiseaseCount = new HashMap<>();
    /** List of TermIds of the patient */
    private static List<TermId> ListOfTermIdsOfHPOTerms = new ArrayList<>();
    /** Sign for calculating LR (Positive='P', Negative = 'N') */
    private static char TestSign = 'P';
    /** Pretest Probability */
    private static final double PretestProb = 0.5;

    private static Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

    private static Map<String,Disease> diseaseMap= new HashMap<>();

    private static Map<String,Double> Disease2LR = new HashMap<>();
    private static Map<String,Double> Disease2PretestOdds = new HashMap<>();
    private static Map<String, Double> Disease2PosttestOdds = new HashMap<>();
    private static Map<String, Double> Disease2PosttestProb = new HashMap<>();
    /** File Name for the results of the Likelihood ratio */
    private static String WriteFileNameLR = "ResultsLR.txt";
    private static String WriteFileNamePretestOdds = "ResultsPretestOdds.txt";
    private static String WriteFileNamePosttesOdds = "ResultsPosttestOdds.txt";
    private static String WriteFileNamePosttesProb = "ResultsPosttestProb.txt";

    static public void main(String [] args) {
        CommandParser parser= new CommandParser(args);
        String hpopath=parser.getHpoPath();
        String annotpath=parser.getAnnotationPath();
        logger.trace("starting");
        parserHPO = new HPOParser();
        hpoOntology = parserHPO.parseOntology(hpopath);
        parserHPO.parseAnnotation(annotpath);
        annotations = parserHPO.getAnnotList();
        parserHPO.initializeTermMap();
        diseaseMap = parserHPO.createDiseaseModels();

        try {
            parserHPO.initializeTerm2DiseaseMap(hpoTerm2DiseaseCount,  diseaseMap, hpoOntology);
        } catch (Exception e) {
            System.err.println(e);
        }

        logger.trace("reading HPO terms from a file");
        HPTerms hpTerm = new HPTerms();
        //Reading HPO terms form a file and storing HPO ids in a list
        hpTerm.getHPOIdFile(ListOfTermIdsOfHPOTerms);

        //Calculating LR, PretestOdds, Posttest Odds and PosttestProb
        LR HPLikelihood = new LR(diseaseMap, ListOfTermIdsOfHPOTerms, hpoTerm2DiseaseCount,  Disease2LR,  Disease2PretestOdds, Disease2PosttestOdds, Disease2PosttestProb, PretestProb,TestSign);
        HPLikelihood.LikelihoodRatios();

       // Writing results of Likelihood ratio, Pretest odds, posttest odds and posttest prob in a file. The results are sorted in a descending order
        HPLikelihood.WritingLikelihood(WriteFileNameLR);
        HPLikelihood.WritingPretestOdds(WriteFileNamePretestOdds);
        HPLikelihood.WritingPosttestOdds(WriteFileNamePosttesOdds);
        HPLikelihood.WritingPosttestProb(WriteFileNamePosttesProb);


/*        WriteResults WriteResults = new WriteResults(Disease2LR, Disease2PretestOdds, Disease2PosttestOdds, Disease2PosttestProb);
        //Writing results of Likelihood ratio in a file
        WriteResults.WritingLikelihood(WriteFileNameLR);

        //Writing results of PretestOdds in a file
        WriteResults.WritingPretestOdds(WriteFileNamePretestOdds);

        //Writing results of PosttestOdds in a file
        WriteResults.WritingPosttestOdds(WriteFileNamePosttesOdds);

        //Writing results of PosttestProb in a file
        WriteResults.WritingPosttestProb(WriteFileNamePosttesProb);*/

    }



 //   public LR2PG(String hpo, String annotation) {
       // parseHPOData(hpo,annotation);
    //}


  /*  private void createDiseaseModels() {
        diseaseMap = new HashMap<>();
        logger.trace("createDiseaseModels");
        for (HpoDiseaseAnnotation annot: annotList) {
            String database=annot.getDb(); /* e.g., OMIM, ORPHA, DECIPHER */
           // String diseaseName=annot.getDbName(); /* e.g., Marfan syndrome */
           // String diseaseId = annot.getDbObjectId(); /* e.g., OMIM:100543 */
           // TermId hpoId  = annot.getHpoId();
            /* Filter database to just get OMIM */
           /* Disease disease=null;
            if (diseaseMap.containsKey(diseaseId)) {
                disease=diseaseMap.get(diseaseId);
            } else {
                disease = new Disease(database,diseaseName,diseaseId); //String database, String name,String id
                diseaseMap.put(diseaseId,disease);
            }
            if ( this.termmap.containsKey(hpoId)) { // restrict to clinical terms, i.e., not inheritance.
                disease.addHpo(hpoId);
            } else {
                logger.trace("Not adding term "+ hpoId.getId());
            }
        }

    }*/



    //private void setUpHpo2Lr() {
        //HPO2LR h2l = new HPO2LR( this.ontology,this.diseaseMap);
   // }


    /*private void parseHPOData(String hpo, String annotation) {
        HPOParser parser = new HPOParser();
       logger.trace("About to parse OBO file");
      // this.ontology = parser.parseOntology(hpo);
       logger.trace("About to parse annot file");
        parser.parseAnnotation(annotation);
        logger.trace("number of non obsolete terms: " + ontology.getNonObsoleteTermIds().size());
        this.inheritance=parser.getInheritanceSubontology();
        this.termmap = parser.extractStrictPhenotypeTermMap();
    }*/


    /*private void debugPrintOntology() {
        logger.trace(this.ontology.getTerms().size() + " terms found in HPO");
        TermId rootID=ontology.getRootTermId();
        Collection<HpoTerm> termlist=ontology.getTerms();
        Map<TermId,HpoTerm> termmap=new HashMap<>();
        for (HpoTerm term:termlist) {
            termmap.put(term.getId(),term);
        }
        Term root = termmap.get(rootID);
        logger.trace("Root: " + root.toString());
    }*/


}
