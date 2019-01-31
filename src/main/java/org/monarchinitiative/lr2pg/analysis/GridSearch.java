package org.monarchinitiative.lr2pg.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.PhenotypeOnlyHpoCaseSimulator;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is a demonstration of the likelihood ratio algorithm that uses simulated cases to assess the performance of the
 * algorithm.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GridSearch  {
    private static final Logger logger = LogManager.getLogger();
    /** key: a disease id such as OMIM:654321; value: coirresponding {@link HpoDisease} object. */
    private final Map<TermId, HpoDisease> diseaseMap;
    /** Reference to HPO ontology object. */
    private final Ontology ontology;
    /** Number of cases to be simulated for any given parameter combination */
    private final int n_cases_to_simulate_per_run;
    /** SHould we exchange the terms with their parents to simulate "imprecise" data entry? */
    private final boolean useImprecision;


    /**
     * Perform a grid search with the indicated number of simulated cases. We will simulate from
     * one to ten HPO terms with from zero to four "random" (noise) terms, and write the results to
     * a file that can be input by R.
     * @param ontology Reference to the HPO ontology
     * @param diseaseMap Map of HPO Disease models
     * @param n_cases Number of cases to simulate
     * @param imprecision if true, use "imprecision" to change HPO terms to a parent term
     */
    public GridSearch(Ontology ontology, Map<TermId, HpoDisease> diseaseMap, int n_cases, boolean imprecision) {
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.n_cases_to_simulate_per_run=n_cases;
        this.useImprecision=imprecision;
    }


    /**
     * Perform a grid search over varying numbers of terms and random terms
     * both with and without moving the terms to parent terms (imprecision).
     * @throws Lr2pgException upon I/O problems with the annotations
     */
    public void gridsearch() throws Lr2pgException {

        int[] termnumber = {1,2,3,4,5,6,7,8,9,10};
        int[] randomtermnumber = {0,1,2,3,4};
        String outfilename=String.format("grid_%d_cases_%s.R",
                n_cases_to_simulate_per_run,
                useImprecision?"imprecise":"precise"
                );
        double[][] Z;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename))) {
            Z = new double[termnumber.length][randomtermnumber.length];
            PhenotypeOnlyHpoCaseSimulator simulator;
            for (int i = 0; i < termnumber.length; i++) {
                for (int j = 0; j < randomtermnumber.length; j++) {
                    simulator = new PhenotypeOnlyHpoCaseSimulator( ontology,diseaseMap,n_cases_to_simulate_per_run, termnumber[i], randomtermnumber[j], useImprecision);
                    simulator.setVerbosity(false); // reduce output!
                    simulator.simulateCases();
                    Z[i][j] = simulator.getProportionAtRank1();
                    System.out.println(String.format("terms: %d; noise terms: %d; percentage at rank 1: %.2f\n",
                            termnumber[i],
                            randomtermnumber[j],
                            100.00 * Z[i][j]));
                }
            }
            // output a file that we will input as an R data frame.
            // see the read-the-docs documentation for how to create a graphic in R with this
            writer.write("library(plot3D)\n");
            writer.write("mat <- matrix(\n");

            List<Double> values=new ArrayList<>();
            for (int j = 0; j < randomtermnumber.length; j++) {
                for (int i = 0; i < termnumber.length; i++) {
                    values.add(Z[i][j]);
                }
            }
            String valuestring=values.stream().map(String::valueOf).collect(Collectors.joining(","));
            writer.write("c(" + valuestring +"),\n");
            writer.write("nrow=5,\nncol=10,\nbyrow=TRUE)\n");
            //writer.write("hist3D(z = mat, scale = FALSE, expand = 0.5, bty = \"g\", phi = 20,\n" +
            //        "      col = \"#0072B2\", border = \"black\", shade = 0.2, ltheta = 99,\n" +
            //       "      space = 0.3, ticktype = \"detailed\", d = 2)");
            writer.write("hist3D(x=seq(0,4),y=seq(1,10),z = mat, scale = FALSE, expand = 3, bty = \"g\", phi = 15, border = \"black\", shade = 0.2,\n"+
                    "ltheta = 50, theta = 40, space = 0.3, ticktype = \"detailed\", d = 50)");
        } catch (IOException e) {
            throw new Lr2pgException("I/O error: " + e.getMessage());
        }

    }
}
