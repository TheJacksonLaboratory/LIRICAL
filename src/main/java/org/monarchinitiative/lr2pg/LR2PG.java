package org.monarchinitiative.lr2pg;



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
    static public void main(String [] args) {
        long startTime = System.currentTimeMillis();
        System.out.println("COMMAND LINE PRIOR TO RUN");
        for(String arg:args) {
            System.out.println(arg);
        }
        System.out.println("##########");
        SpringApplication.run(LR2PG.class, args);
        long stopTime = System.currentTimeMillis();
        System.out.println("LRPG: Elapsed time was " + (stopTime - startTime)*(1.0)/1000 + " seconds.");
    }


}
