package org.monarchinitiative.lirical.output;


import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.svg.Sparkline2Svg;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates all of the information we need to produce one row of the sparkline table.
 */
public class SparklinePacket {

    private final int rank;
    private final String posttestBarSvg;
    private final String sparklineSvg;
    private final double compositeLikelihoodRatio;
    private final String geneSymbol;
    private final String diseaseName;

    private final static String EMPTY_STRING ="";

    /** Factory method for genotype-phenotype analysis.*/
    public static List<SparklinePacket> sparklineFactory(HpoCase hcase, int N, Map<TermId,String> geneid2sym) {
        ImmutableList.Builder<SparklinePacket> builder = new ImmutableList.Builder<>();
        List<TestResult> results = hcase.getResults(); // this is a sorted list!
        if (results.isEmpty()) {
            return builder.build();
        }
        TermId topCandidateId = results.get(0).getDiseaseCurie();
        int rank = 0;
        Sparkline2Svg sparkline2Svg = new Sparkline2Svg(hcase,topCandidateId, true);
        while (rank < N && rank < results.size()) {
            TestResult result = results.get(rank);
            rank++;
            TermId diseaseId = result.getDiseaseCurie();
            String geneSymbol = EMPTY_STRING;
            if (result.hasGenotype() ) {
                TermId geneId = result.getEntrezGeneId();
                geneSymbol = geneid2sym.getOrDefault(geneId, EMPTY_STRING);
            }
            double compositeLR = result.getCompositeLR();
            double posttestProb = result.getPosttestProbability();
            String posttestSVG = sparkline2Svg.getPosttestBar(posttestProb);
            String sparkSVG = sparkline2Svg.getSparklineSvg(hcase, diseaseId);
            String disname = prettifyDiseaseName(result.getDiseaseName());
            SparklinePacket sp = new SparklinePacket(rank, posttestSVG, sparkSVG, compositeLR, geneSymbol, disname);
            builder.add(sp);
        }
        return builder.build();
    }

    /** Factory method for phenotype only analysis.*/
    public static List<SparklinePacket> sparklineFactory(HpoCase hcase, int N) {
        ImmutableList.Builder<SparklinePacket> builder = new ImmutableList.Builder<>();
        List<TestResult> results = hcase.getResults(); // this is a sorted list!
        if (results.isEmpty()) {
            return builder.build();
        }
        TermId topCandidateId = results.get(0).getDiseaseCurie();
        int rank = 0;
        Sparkline2Svg sparkline2Svg = new Sparkline2Svg(hcase,topCandidateId, false);
        while (rank < N && rank < results.size()) {
            TestResult result = results.get(rank);
            rank++;
            TermId diseaseId = result.getDiseaseCurie();
            double compositeLR = result.getCompositeLR();
            double posttestProb = result.getPosttestProbability();
            String posttestSVG = sparkline2Svg.getPosttestBar(posttestProb);
            String sparkSVG = sparkline2Svg.getSparklineSvg(hcase, diseaseId);
            String disname = prettifyDiseaseName(result.getDiseaseName());
            SparklinePacket sp = new SparklinePacket(rank, posttestSVG, sparkSVG, compositeLR, EMPTY_STRING, disname);
            builder.add(sp);
        }
        return builder.build();
    }


    /**
     * This method shortens items such as #101200 APERT SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE I; ACS1;;ACS IAPERT-CROUZON DISEASE, INCLUDED;;
     * to simply APERT SYNDROME
     * @param originalDiseaseName original String from HPO database, derived from OMIM and potentially historic
     * @return simplified and prettified name
     */
    private static String prettifyDiseaseName(String originalDiseaseName) {
        // shorten the name to everything up to the first semicolon
        int i = originalDiseaseName.indexOf(";");
        if (i>0) {
            originalDiseaseName = originalDiseaseName.substring(0,i);
        }
        originalDiseaseName=originalDiseaseName.trim();
        final Pattern omimid = Pattern.compile("#?\\d{6}");
        Matcher m = omimid.matcher(originalDiseaseName);
        if (m.find(0)) {
            // last position of match
            i = m.end() + 1;
            originalDiseaseName = originalDiseaseName.substring(i).trim();
        }
        return originalDiseaseName;
    }



    private SparklinePacket(int rank, String posttest, String spark, double compLR, String sym, String disname){
        this.rank = rank;
        this.posttestBarSvg = posttest;
        this.sparklineSvg = spark;
        this.compositeLikelihoodRatio = Math.log10(compLR);
        this.geneSymbol = sym;
        this.diseaseName = disname;
    }

    public int getRank() {
        return rank;
    }

    public String getPosttestBarSvg() {
        return posttestBarSvg;
    }

    public String getSparklineSvg() {
        return sparklineSvg;
    }

    public double getCompositeLikelihoodRatio() {
        return compositeLikelihoodRatio;
    }

    public String getGeneSymbol() {
        return geneSymbol;
    }

    public String getDiseaseName() {
        return diseaseName;
    }
}
