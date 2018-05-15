package org.monarchinitiative.lr2pg.command;

import com.sun.xml.internal.xsom.impl.scd.Iterators;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.hpo.HpoCaseSimulator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a demonstration of the likelihood ratio algorithm that uses simulated cases to assess the performance of the
 * algorithm.
 */
public class SimulateCasesCommand implements Command {
    private static final Logger logger = LogManager.getLogger();
    /** Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}. */
    private final String dataDirectoryPath;
    private final int n_cases_to_simulate;
    private final int n_terms_per_case;
    private final int n_noise_terms;

    private boolean gridSearch=false;

    /**
     * @param datadir Path to a directory containing {@code hp.obo} and {@code phenotype.hpoa}.
     */
    public SimulateCasesCommand(String datadir, int cases_to_simulate, int terms_per_case, int noise_terms ) {
        dataDirectoryPath=datadir;
        this.n_cases_to_simulate=cases_to_simulate;
        this.n_terms_per_case=terms_per_case;
        this.n_noise_terms=noise_terms;
    }

    public SimulateCasesCommand(String datadir, int cases_to_simulate, int terms_per_case, int noise_terms, boolean grid ) {
        this(datadir,cases_to_simulate,terms_per_case,noise_terms);
        this.gridSearch=grid;
    }

    public void execute() {
        logger.trace("Executing HpoCase simulation");
        if (gridSearch) {
            try {
                gridsearch();
                return;
            } catch (Lr2pgException|IOException lre) {
                lre.printStackTrace();
            }
        }
        HpoCaseSimulator simulator = new HpoCaseSimulator(this.dataDirectoryPath,n_cases_to_simulate, n_terms_per_case, n_noise_terms);
        simulator.debugPrint();
        try {
            simulator.simulateCases();
        } catch (Lr2pgException e) {
            e.printStackTrace();
        }
    }


    private void gridsearch() throws Lr2pgException, IOException {


        int[] termnumber = {1,2,3,4,5,6,7,8,9,10};
        int[] randomtermnumber = {0,1,2,3,4,0,1,2,3,4};
//        int[] termnumber = {1,2,3};
//        int[] randomtermnumber = {0,1,2};
        String outfilename="gridsearch.R";
        BufferedWriter writer = new BufferedWriter(new FileWriter(outfilename));
        double[][] Z = new double[termnumber.length][randomtermnumber.length];
        int n_cases=100;
        HpoCaseSimulator simulator;
        for (int i=0;i<termnumber.length;i++) {
            for (int j=0;j<randomtermnumber.length;j++) {
                boolean imprec;
                if (j>4) {
                    imprec=true;
                } else {
                    imprec=false;
                }
                simulator = new HpoCaseSimulator(this.dataDirectoryPath,n_cases, termnumber[i], randomtermnumber[j],imprec);
                simulator.simulateCases();
                Z[i][j] = simulator.getProportionAtRank1();
                writer.write(String.format("terms: %d; noise terms: %d; percentage at rank 1: %.2f\n",
                        termnumber[i],
                        randomtermnumber[j],
                        100.0* Z[i][j]));
            }
        }
        String xstring=String.format("X <- c(%s)\n",
                Arrays.stream(termnumber).
                        mapToObj(Integer::toString)
                        .collect(Collectors.joining(", ")));
        String ystring=String.format("Y <- c(%s)\n",
                Arrays.stream(randomtermnumber).
                        mapToObj(Integer::toString)
                        .collect(Collectors.joining(", ")));
        List<String> Zlist = new ArrayList<>();
        for (int i = 0; i < termnumber.length; i++) {
            for (int j=0;j<randomtermnumber.length;j++) {
                Zlist.add(String.format("%.2f",100.0*Z[i][j]));
            }
        }
        String zstring=String.format("Z <- matrix(c(%s),nrow=%d,ncol=%d)\n",
                Zlist.stream().collect(Collectors.joining(",")),
                termnumber.length,
                randomtermnumber.length);
        writer.write("library(plot3D)\n");
        writer.write(xstring + ystring +  zstring);

        String xlab = String.format("c(%s)\n",
                Arrays.stream(termnumber).
                        mapToObj(i -> String.format("\"%d\"",i))
                        .collect(Collectors.joining(", ")));
        String create3d = String.format(" hist3D(X,Y,Z,xlab=%s, clab=\"%% at rank 1\",zlim=c(0,1))\n",xlab);
        writer.write(create3d);
        //alternatively, do a grouped bar plot

        String colnames= Arrays.stream(termnumber).mapToObj(i->String.format("\"%s\"",i)).collect(Collectors.joining(", "));
        String rownames= Arrays.stream(randomtermnumber).mapToObj(i->String.format("\"%s\"",i)).collect(Collectors.joining(", "));

        String barplotLegend = String.format("colnames(Z)=c(%s)\nrownames(Z)=c(%s)\n",colnames,rownames);
        String barplot = String.format("barplot(Z, col=colors()[30:32], border=\"white\", font.axis=2, beside=T, " +
                "legend=rownames(Z), xlab=\"Number of terms\", font.lab=2, cex.lab=2,cex.axis=1.5)\n");
        writer.write(barplotLegend+barplot);


        writer.close();
        for (int i=0;i<termnumber.length;i++) {
            for (int j=0;j<randomtermnumber.length;j++) {
                System.err.println(String.format("terms: %d; noise terms: %d; percentage at rank 1: %.2f",
                        termnumber[i],
                        randomtermnumber[j],
                        100.0* Z[i][j]));
            }
        }

    }
}
