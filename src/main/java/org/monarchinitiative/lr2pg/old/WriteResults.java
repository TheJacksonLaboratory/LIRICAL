package org.monarchinitiative.lr2pg.old;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by ravanv on 10/4/17.
 */

    public class WriteResults {

    Map<String, Double> Disease2LR;
    Map<String, Double> Disease2PretestOdds;
    Map<String, Double> Disease2PosttestOdds;
    Map<String, Double> Disease2PosttestProb;
    public WriteResults(Map<String, Double> DiseaseLR, Map<String, Double> DiseasePretestOdds, Map<String, Double> DiseasePosttestOdds, Map<String, Double> DiseasePosttestProb){
        Disease2LR = DiseaseLR;
        Disease2PretestOdds = DiseasePretestOdds;
        Disease2PosttestOdds = DiseasePosttestOdds;
        Disease2PosttestProb = DiseasePosttestProb;

    }

//Writing results in files
        public void WritingLikelihood(String WriteFileName) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(WriteFileName, false))) {
                writer.write("disease id");
                writer.write("  ");
                writer.write("Likelihood ratio");
                writer.write("  ");
                writer.write("PretestOdds");
                writer.write("  ");
                writer.write("PosttestOdds");
                writer.write("  ");
                writer.write("PosttestProb");
                writer.newLine();
                for (String disease : Disease2LR.keySet()) {
                    writer.write(disease);
                    writer.write("  ");
                    writer.write(String.valueOf(Disease2LR.get(disease)));
                    writer.write("  ");
                    writer.write(String.valueOf(Disease2PretestOdds.get(disease)));
                    writer.write("  ");
                    writer.write(String.valueOf(Disease2PosttestOdds.get(disease)));
                    writer.write("  ");
                    writer.write(String.valueOf(Disease2PosttestProb.get(disease)));
                    writer.newLine();
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }



    }


