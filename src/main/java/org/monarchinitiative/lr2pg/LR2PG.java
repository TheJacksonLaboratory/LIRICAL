package org.monarchinitiative.lr2pg;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.Command;

import org.monarchinitiative.lr2pg.io.CommandParser;

/**
 * This is the central class that coordinates the phenotype/genotype likelihood ratio test.
 * @author Vida Ravanmehr
 * @author Peter Robinson
 * @version 0.3.1 (2018-03-11)
 */
public class LR2PG {
    private static final Logger logger = LogManager.getLogger();
    static public void main(String [] args) {
        logger.trace("MAIN");
        long startTime = System.currentTimeMillis();
        CommandParser cmdline= new CommandParser(args);
        Command command = cmdline.getCommand();
        command.execute();
        long stopTime = System.currentTimeMillis();
        System.out.println("Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");

    }


}
