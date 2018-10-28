package org.monarchinitiative.lr2pg;



import org.monarchinitiative.lr2pg.cmd.Lr2PgCommand;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.CommandLine;



/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Vida Ravanmehr
 * @author Peter Robinson
 * @version 0.3.1 (2018-03-11)
 */


public class LR2PG {
    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();
        CommandLine clp = new CommandLine(args);
        Lr2PgCommand command = clp.getCommand();
        try {
            command.run();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }


      /*  System.out.println("COMMAND LINE PRIOR TO RUN");
        for(String arg:args) {
            System.out.println(arg);
        }
        System.out.println("##########");
        SpringApplication.run(LR2PG.class, args);
        */
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }


}
