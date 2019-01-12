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


    /**
     *
     */
    public GridSearch(Ontology ontology, Map<TermId, HpoDisease> diseaseMap, int n_cases ) {
        this.ontology=ontology;
        this.diseaseMap=diseaseMap;
        this.n_cases_to_simulate_per_run=n_cases;
    }


    /**
     * Perform a grid search over varying numbers of terms and random terms
     * both with and without moving the terms to parent terms (imprecision).
     * @throws Lr2pgException upon I/O problems with the annotations
     */
    public void gridsearch() throws Lr2pgException {

        int[] termnumber = {1,2,3,4,5,6,7,8,9,10};
        int[] randomtermnumber = {0,1,2,3,4,0,1,2,3,4};
        String outfilename="gridsearch.R";
        double[][] Z;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename))) {
            Z = new double[termnumber.length][randomtermnumber.length];
            PhenotypeOnlyHpoCaseSimulator simulator;
            for (int i = 0; i < termnumber.length; i++) {
                for (int j = 0; j < randomtermnumber.length; j++) {
                    boolean imprec = (j > 4);
                    simulator = new PhenotypeOnlyHpoCaseSimulator( ontology,diseaseMap,n_cases_to_simulate_per_run, termnumber[i], randomtermnumber[j], imprec);
                    simulator.simulateCases();
                    Z[i][j] = simulator.getProportionAtRank1();
                    writer.write(String.format("terms: %d; noise terms: %d; percentage at rank 1: %.2f\n",
                            termnumber[i],
                            randomtermnumber[j],
                            100.0 * Z[i][j]));
                }
            }
            String xstring = String.format("X <- c(%s)\n",
                    Arrays.stream(termnumber).
                            mapToObj(Integer::toString)
                            .collect(Collectors.joining(", ")));
            String ystring = String.format("Y <- c(%s)\n",
                    Arrays.stream(randomtermnumber).
                            mapToObj(Integer::toString)
                            .collect(Collectors.joining(", ")));
            List<String> Zlist = new ArrayList<>();
            for (int i = 0; i < termnumber.length; i++) {
                for (int j = 0; j < randomtermnumber.length; j++) {
                    Zlist.add(String.format("%.2f", 100.0 * Z[i][j]));
                }
            }
            String zstring = String.format("Z <- matrix(c(%s),nrow=%d,ncol=%d)\n",
                    Zlist.stream().collect(Collectors.joining(",")),
                    termnumber.length,
                    randomtermnumber.length);
            writer.write("library(plot3D)\n");
            writer.write(xstring + ystring + zstring);

            String xlab = String.format("c(%s)\n",
                    Arrays.stream(termnumber).
                            mapToObj(i -> String.format("\"%d\"", i))
                            .collect(Collectors.joining(", ")));
            String create3d = String.format(" hist3D(X,Y,Z,xlab=%s, clab=\"%% at rank 1\",zlim=c(0,1))\n", xlab);
            writer.write(create3d);
            //alternatively, do a grouped bar plot

            String colnames = Arrays.stream(termnumber)
                    .mapToObj(i -> String.format("\"%s\"", i))
                    .collect(Collectors.joining(", "));
            String rownames = Arrays.stream(randomtermnumber)
                    .mapToObj(i -> String.format("\"%s\"", i))
                    .collect(Collectors.joining(", "));

            String barplotLegend = String.format("colnames(Z)=c(%s)\nrownames(Z)=c(%s)\n", colnames, rownames);
            String barplot = "barplot(Z, col=colors()[30:32], border=\"white\", font.axis=2, beside=T, " +
                    "legend=rownames(Z), xlab=\"Number of terms\", font.lab=2, cex.lab=2,cex.axis=1.5)\n";
            writer.write(barplotLegend + barplot);

        for (int i=0;i<termnumber.length;i++) {
            for (int j=0;j<randomtermnumber.length;j++) {
                System.err.println(String.format("terms: %d; noise terms: %d; percentage at rank 1: %.2f",
                        termnumber[i],
                        randomtermnumber[j],
                        100.0* Z[i][j]));
            }
        }
        } catch (IOException e) {
            throw new Lr2pgException("I/O error: " + e.getMessage());
        }

    }
}
