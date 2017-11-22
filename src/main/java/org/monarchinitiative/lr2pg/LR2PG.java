package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.Command;
import org.monarchinitiative.lr2pg.hpo.HpoDiseaseWithMetadata;
import org.monarchinitiative.lr2pg.io.CommandParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the central class that coordinates the phenotype/genotype likelihood ratio test.
 * The path through the program is as follows.
 * <ul>
 *     <li>Parse the hp.obo file (see {@link HpoOntologyParser}</li>
 * </ul>
 * @author Vida Ravanmehr
 * @author Peter Robinson
 * @version 0.3.1 (2017-11-25)
 */
public class LR2PG {
    private static final Logger logger = LogManager.getLogger();
    /** HPO phenotypic abnormality subontology. */
    private Ontology<HpoTerm, HpoTermRelation> phenotypeSubontology =null;
    /** HPO inheritance subontology. */
    Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology =null;
    /** Key, a disease ID like 100324 for OMIM:100324; value-corresponding {@link HpoDiseaseWithMetadata} object .*/
    private Map<String, HpoDiseaseWithMetadata> diseaseMap=null;

    /** List of all annotations parsed from phenotype_annotation.tab. */
//    private List<HpoDiseaseAnnotation> annotList=null;


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
        logger.trace("MAIN");
        CommandParser cmdline= new CommandParser(args);
        Command command = cmdline.getCommand();
        command.execute();


//        lr2pg.getPatientHPOTermsFromFile(fileName);
//        logger.trace("Creating disease map and calculating Likelihood ratio, PretestOdds, Posttest Odds and PosttestProb");
//        lr2pg.calculateLikelihoodRatio();

    }






    public void calculateLikelihoodRatio() {
        //Calculating LR, PretestOdds, Posttest Odds and PosttestProb
//        LR HPLikelihood = new LR(diseaseMap, ListOfTermIdsOfHPOTerms, hpoTerm2DiseaseCount, PretestProb,TestSign);
//        HPLikelihood.LikelihoodRatios();
//        // Writing results of Likelihood ratio, Pretest odds, posttest odds and posttest prob in a file. The results are sorted in a descending order
//        HPLikelihood.WritingLikelihood(WriteFileNameLR);
    }







}
