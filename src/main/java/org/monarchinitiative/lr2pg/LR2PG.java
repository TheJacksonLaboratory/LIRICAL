package org.monarchinitiative.lr2pg;


import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.Command;

import org.monarchinitiative.lr2pg.io.CommandParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;


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


    /** Pretest Probability */
    private static final double PretestProb = 0.5;

    private String pathToPatientData=null;

    private Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

//    private Map<String,Disease> diseaseMap= new HashMap<>();



    static public void main(String [] args) {
        logger.trace("MAIN");
        long startTime = System.currentTimeMillis();
        CommandParser cmdline= new CommandParser(args);
        Command command = cmdline.getCommand();
        command.execute();
        long stopTime = System.currentTimeMillis();
        System.out.println("Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");


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
