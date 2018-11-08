package org.monarchinitiative.lr2pg;



import org.monarchinitiative.lr2pg.cmd.Lr2PgCommand;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.CommandLine;


/**
 * This is the central class that coordinates the phenotype/Genotype2LR likelihood ratio test.
 * @author Peter Robinson
 * @version 0.5.1 (2018-11-05)
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
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }

}
