package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.command.Command;
import org.monarchinitiative.lr2pg.hpo.HpoDiseaseWithMetadata;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;
import org.apache.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HPTerms;
import org.monarchinitiative.lr2pg.old.WriteResults;
import org.monarchinitiative.lr2pg.io.CommandParser;

import java.io.IOException;
import java.util.*;

/**
 * This is the central class that coordinates the phenotype/genotype likelihood ratio test.
 * The path through the program is as follows.
 * <ul>
 *     <li>Parse the hp.obo file (see {@link HpoOntologyParser}</li>
 * </ul>
 * @author Vida Ravanmehr
 * @author Peter Robinson
 * @version 0.2.1 (2017-11-15)
 */
public class LR2PG {
    static Logger logger = Logger.getLogger(LR2PG.class.getName());
    /** HPO phenotypic abnormality subontology. */
    private Ontology<HpoTerm, HpoTermRelation> phenotypeSubontology =null;
    /** HPO inheritance subontology. */
    Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology =null;
    /** Key, a disease ID like 100324 for OMIM:100324; value-corresponding {@link HpoDiseaseWithMetadata} object .*/
    private Map<String, HpoDiseaseWithMetadata> diseaseMap=null;

    /** List of all annotations parsed from phenotype_annotation.tab. */
//    private List<HpoDiseaseAnnotation> annotList=null;

    private static WriteResults writeResults = null;
//    private static LR HPLikelihood = null;
//    private List<HpoDiseaseAnnotation> annotations=null;

    /** Map of HPO Term Ids and the number of diseases that has the HPO term */
    private Map<TermId, Integer> hpoTerm2DiseaseCount = new HashMap<>();
    /** List of TermIds of the patient */
    private List<TermId> ListOfTermIdsOfHPOTerms = new ArrayList<>();
    /** Sign for calculating LR (Positive='P', Negative = 'N') */
    private static char TestSign = 'P';
    /** Pretest Probability */
    private static final double PretestProb = 0.5;

//    private static String fileName = "/Users/ravanv/Documents/IntelliJ_projects/HPO_LRTest/LR2PG/HPOTerms.txt";

    private String pathToPatientData=null;

    private Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

//    private Map<String,Disease> diseaseMap= new HashMap<>();

//    private static Map<String,Double> Disease2LR = new HashMap<>();
//    private static Map<String,Double> Disease2PretestOdds = new HashMap<>();
//    private static Map<String, Double> Disease2PosttestOdds = new HashMap<>();
//    private static Map<String, Double> Disease2PosttestProb = new HashMap<>();
    /** File Name for the results of the Likelihood ratio, PretestOdds, Posttest Odds and PosttestProb */
    private static String WriteFileNameLR = "Results.txt";

    static public void main(String [] args) {
        LR2PG lr2pg = new LR2PG(args);
        logger.trace("reading HPO terms from a file");
//        lr2pg.getPatientHPOTermsFromFile(fileName);
//        logger.trace("Creating disease map and calculating Likelihood ratio, PretestOdds, Posttest Odds and PosttestProb");
//        lr2pg.calculateLikelihoodRatio();

    }

    public LR2PG(String args[]) {
        CommandParser cmdline= new CommandParser(args);
        if (true) {
            Command command = cmdline.getCommand();
            command.execute();
            return;
        }
        String annotationPath = cmdline.getAnnotationPath();
        String pathToPatientData = cmdline.getPatientAnnotations();
        HpoOntologyParser parserHPO = new HpoOntologyParser(cmdline.getHpoPath());
        try {
            parserHPO.parseOntology();
            this.phenotypeSubontology=parserHPO.getPhenotypeSubontology();
            this.inheritanceSubontology=parserHPO.getInheritanceSubontology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        HpoAnnotation2DiseaseParser annotationParser =
                new HpoAnnotation2DiseaseParser(annotationPath,
                    this.phenotypeSubontology,
                        this.inheritanceSubontology);
        this.diseaseMap = annotationParser.getDiseaseMap();
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
//        LR HPLikelihood = new LR(diseaseMap, ListOfTermIdsOfHPOTerms, hpoTerm2DiseaseCount, PretestProb,TestSign);
//        HPLikelihood.LikelihoodRatios();
//        // Writing results of Likelihood ratio, Pretest odds, posttest odds and posttest prob in a file. The results are sorted in a descending order
//        HPLikelihood.WritingLikelihood(WriteFileNameLR);
    }







}
