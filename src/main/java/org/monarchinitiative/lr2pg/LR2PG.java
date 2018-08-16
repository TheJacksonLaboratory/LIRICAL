package org.monarchinitiative.lr2pg;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.command.Command;

import org.monarchinitiative.lr2pg.io.CommandParser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Vida Ravanmehr
 * @author Peter Robinson
 * @version 0.3.1 (2018-03-11)
 */

@SpringBootApplication
public class LR2PG {
    private static final Logger logger = LogManager.getLogger();
    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();
       /* CommandParser cmdline= new CommandParser(args);
        Command command = cmdline.getCommand();
        command.execute();*/


        SpringApplication.run(LR2PG.class, args);

        long stopTime = System.currentTimeMillis();
        System.out.println("Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");

    }


}
