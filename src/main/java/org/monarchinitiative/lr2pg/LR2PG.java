package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.old.Disease;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.old.HPOParser;
import org.monarchinitiative.lr2pg.hpo.HPTerms;
import org.monarchinitiative.lr2pg.old.WriteResults;
import org.monarchinitiative.lr2pg.io.CommandParser;
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
    //private static HPOParser parserHPO=null;
    private static WriteResults writeResults = null;
    private static LR HPLikelihood = null;
    private List<HpoDiseaseAnnotation> annotations=null;
    /** Map of HPO Term Ids and the number of diseases that has the HPO term */
    private Map<TermId, Integer> hpoTerm2DiseaseCount = new HashMap<>();
    /** List of TermIds of the patient */
    private List<TermId> ListOfTermIdsOfHPOTerms = new ArrayList<>();
    /** Sign for calculating LR (Positive='P', Negative = 'N') */
    private static char TestSign = 'P';
    /** Pretest Probability */
    private static final double PretestProb = 0.5;

    private static String fileName = "/Users/ravanv/Documents/IntelliJ_projects/HPO_LRTest/LR2PG/HPOTerms.txt";

    private String pathToPatientData=null;

    private Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

    private Map<String,Disease> diseaseMap= new HashMap<>();

    private static Map<String,Double> Disease2LR = new HashMap<>();
    private static Map<String,Double> Disease2PretestOdds = new HashMap<>();
    private static Map<String, Double> Disease2PosttestOdds = new HashMap<>();
    private static Map<String, Double> Disease2PosttestProb = new HashMap<>();
    /** File Name for the results of the Likelihood ratio, PretestOdds, Posttest Odds and PosttestProb */
    private static String WriteFileNameLR = "Results.txt";

    public LR2PG(String args[]) {
        CommandParser parser= new CommandParser(args);
        String hpopath=parser.getHpoPath();
        String annotpath=parser.getAnnotationPath();
        String pathToPatientData = parser.getPatientAnnotations();
        HPOParser parserHPO = new HPOParser();
        hpoOntology = parserHPO.parseOntology(hpopath);
        parserHPO.parseAnnotation(annotpath);
        this.annotations = parserHPO.getAnnotList();
        parserHPO.initializeTermMap();
        this.diseaseMap = parserHPO.createDiseaseModels();
        try {
            parserHPO.initializeTerm2DiseaseMap(hpoTerm2DiseaseCount,  diseaseMap, hpoOntology);
        } catch (Exception e) {
            System.err.println(e);
        }
        // etc
    }


    public void getPatientHPOTermsFromFile(String filename) {
        if (this.pathToPatientData != null) {
            filename = this.pathToPatientData;
        }

        HPTerms hpTerm = new HPTerms(filename);
        //Reading HPO terms form a file and storing HPO ids in a list
        hpTerm.getHPOIdFile(this.ListOfTermIdsOfHPOTerms);
    }


    public void calculateLikelihoodRatio() {
        //Calculating LR, PretestOdds, Posttest Odds and PosttestProb
        LR HPLikelihood = new LR(diseaseMap, ListOfTermIdsOfHPOTerms, hpoTerm2DiseaseCount, PretestProb,TestSign);
        HPLikelihood.LikelihoodRatios();
        // Writing results of Likelihood ratio, Pretest odds, posttest odds and posttest prob in a file. The results are sorted in a descending order
        HPLikelihood.WritingLikelihood(WriteFileNameLR);
    }



    static public void main(String [] args) {
        LR2PG lr2pg = new LR2PG(args);
        logger.trace("starting");
        logger.trace("reading HPO terms from a file");
        lr2pg.getPatientHPOTermsFromFile(fileName);
        logger.trace("Creating disease map and calculating Likelihood ratio, PretestOdds, Posttest Odds and PosttestProb");
        lr2pg.calculateLikelihoodRatio();

    }



}
