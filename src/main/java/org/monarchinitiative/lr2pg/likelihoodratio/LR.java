package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.ontology.data.TermId;
import org.monarchinitiative.lr2pg.old.MapUtil;
import org.monarchinitiative.lr2pg.old.Disease;
import org.monarchinitiative.lr2pg.old.HPOParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ravanv on 10/4/17.
 */
public class LR {

     HPOParser parserHPO=new HPOParser();
     Map<String, Disease> diseaseMap;
     List<TermId> listOfTermIdsOfHPOTerms;
     Map<TermId, Integer> hpoTerm2DiseaseCount;
     Map<String, Double> Disease2LR;
     Map<String, Double> Disease2PretestOdds;
     Map<String, Double> Disease2PosttestOdds;
     Map<String, Double> Disease2PosttestProb;
     Double PretestProb;
     char testSign;
    // String WriteFileNameLR;

    public LR (Map<String,Disease>diseaseMp, List<TermId>ListOfTermIds, Map<TermId,Integer>hpoTerm2DisCount,    Double PreProb, char Sign ){
        diseaseMap = diseaseMp;
        listOfTermIdsOfHPOTerms = ListOfTermIds;
        hpoTerm2DiseaseCount = hpoTerm2DisCount;
        Disease2LR = new HashMap<>();
        Disease2PretestOdds = new HashMap<>();
        Disease2PosttestOdds = new HashMap<>();
        Disease2PosttestProb = new HashMap<>();
        PretestProb = PreProb;
        testSign = Sign;
       // WriteFileNameLR = FileName;


    }
    // calculating LRs, Pretest Odds, Posttest Odds and Posttest Prob
    public void LikelihoodRatios( ){

        for (String disease : diseaseMap.keySet()) {
            List<HPOTestResult> results = new ArrayList<>();
            for (TermId id : listOfTermIdsOfHPOTerms) {
                if(hpoTerm2DiseaseCount.containsKey(id)) {
                   HPOTestResult result = new HPOTestResult(parserHPO.getFrequency(disease, id, diseaseMap), 1-parserHPO.getBackgroundFrequency(id,diseaseMap, hpoTerm2DiseaseCount));
                   results.add(result);
                }
            }
            HPOLRTest hpolrtest = new HPOLRTest(results, PretestProb, testSign);
            Disease2LR.put(disease,hpolrtest.getCompositeLikelihoodRatio());
            Disease2PretestOdds.put(disease,hpolrtest.getPretestOdds());
            Disease2PosttestOdds.put(disease,hpolrtest.getPosttestOdds());
            Disease2PosttestProb.put(disease,hpolrtest.getPosttestProbability());

        }
        //Sorting the LR, Pretest Odds, Posttest Odds and Posttest Prob lists
        Disease2LR = MapUtil.sortByValue(Disease2LR);
        Disease2PretestOdds = MapUtil.sortByValue(Disease2PretestOdds);
        Disease2PosttestOdds = MapUtil.sortByValue(Disease2PosttestOdds);
        Disease2PosttestProb = MapUtil.sortByValue(Disease2PosttestProb);
    }

    public void WritingLikelihood(String WriteFileNameLR) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WriteFileNameLR, false))) {
            writer.write("disease_id");
            writer.write("  ");
            writer.write("Likelihood_Ratio");
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

   /* public void WritingPretestOdds(String WriteFileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WriteFileName, false))) {
            writer.write("disease id");
            writer.write("  ");
            writer.write("PretestOdds");
            writer.newLine();
            for (String disease : Disease2PretestOdds.keySet()) {
                writer.write(disease);
                writer.write("  ");
                writer.write(String.valueOf(Disease2PretestOdds.get(disease)));
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void WritingPosttestOdds(String WriteFileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WriteFileName, false))) {
            writer.write("disease id");
            writer.write("  ");
            writer.write("PosttestOdds");
            writer.newLine();
            for (String disease : Disease2PosttestOdds.keySet()) {
                writer.write(disease);
                writer.write("  ");
                writer.write(String.valueOf(Disease2PosttestOdds.get(disease)));
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public void WritingPosttestProb( String WriteFileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WriteFileName, false))) {
            writer.write("disease id");
            writer.write("  ");
            writer.write("PosttestProb");
            writer.newLine();
            for (String disease : Disease2PosttestProb.keySet()) {
                writer.write(disease);
                writer.write("  ");
                writer.write(String.valueOf(Disease2PosttestProb.get(disease)));
                writer.newLine();
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }*/




}
