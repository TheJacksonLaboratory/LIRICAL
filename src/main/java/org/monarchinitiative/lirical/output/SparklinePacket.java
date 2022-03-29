package org.monarchinitiative.lirical.output;


import org.monarchinitiative.lirical.analysis.AnalysisResults;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.svg.Sparkline2Svg;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class encapsulates all of the information we need to produce one row of the sparkline table.
 */
public class SparklinePacket {

    private final int rank;
    private final String posttestBarSvg;
    private final String sparklineSvg;
    private final String geneSparklineSvg;
    private final double compositeLikelihoodRatio;
    private final String geneSymbol;
    private final String diseaseName;
    private final String diseaseAnchor;

    private final static String EMPTY_STRING ="";

    /** Factory method for genotype-phenotype analysis.*/
    public static List<SparklinePacket> sparklineFactory(HpoCase hcase, int N, Map<TermId,String> geneid2sym, Ontology ontology) {
        AnalysisResults results = hcase.results();
        if (results.isEmpty())
            return List.of();

        List<SparklinePacket> packets = new ArrayList<>(N);
        // we're checking that results are not empty
        //noinspection OptionalGetWithoutIsPresent
        TestResult topResult = results.resultsWithDescendingPostTestProbability().findFirst().get();
        TermId topCandidateId = topResult.diseaseId();
        AtomicInteger r = new AtomicInteger();
        Sparkline2Svg sparkline2Svg = new Sparkline2Svg(topResult,topCandidateId, true, ontology);
        results.resultsWithDescendingPostTestProbability().forEachOrdered(result -> {
            int current = r.incrementAndGet();
            if (current == N)
                return;
            TermId diseaseId = result.diseaseId();
            String geneSymbol = EMPTY_STRING;
            Optional<GenotypeLrWithExplanation> genotypeLr = result.genotypeLr();
            if (genotypeLr.isPresent()) {
                TermId geneId = genotypeLr.get().geneId();
                geneSymbol = geneid2sym.getOrDefault(geneId, EMPTY_STRING);
            }
            double compositeLR = result.getCompositeLR();
            double posttestProb = result.posttestProbability();
            String posttestSVG = sparkline2Svg.getPosttestBar(posttestProb);
            String sparkSVG = sparkline2Svg.getSparklineSvg(geneSymbol, result);
            String geneSparkSvg = sparkline2Svg.getGeneSparklineSvg(hcase, diseaseId, geneSymbol);
            String disname = prettifyDiseaseName(result.getDiseaseName());
            String diseaseAnchor = getDiseaseAnchor(diseaseId);
            SparklinePacket sp = new SparklinePacket(current, posttestSVG, sparkSVG, geneSparkSvg, compositeLR, geneSymbol, disname, diseaseAnchor);
            packets.add(sp);
        });

        return packets;
    }

    /** Factory method for phenotype only analysis.*/
    public static List<SparklinePacket> sparklineFactory(AnalysisResults results, int N, Ontology ontology) {
        Optional<TestResult> topTestResultOptional = results.resultsWithDescendingPostTestProbability().findFirst();
        if (topTestResultOptional.isEmpty())
            return List.of();

        TestResult topCandidate = topTestResultOptional.get();
        Sparkline2Svg sparkline2Svg = new Sparkline2Svg(topCandidate, topCandidate.diseaseId(),false, ontology);
        final AtomicInteger r = new AtomicInteger();
        return results.resultsWithDescendingPostTestProbability()
                .limit(N)
                .map(result -> {
                    int rank = r.incrementAndGet();
                    TermId diseaseId = result.diseaseId();
                    double compositeLR = result.getCompositeLR();
                    double posttestProb = result.posttestProbability();
                    String posttestSVG = sparkline2Svg.getPosttestBar(posttestProb);
                    String sparkSVG = sparkline2Svg.getSparklineSvg("", result);
                    String disname = prettifyDiseaseName(result.getDiseaseName());
                    String diseaseAnchor = getDiseaseAnchor(diseaseId);
                    return new SparklinePacket(rank, posttestSVG, sparkSVG, compositeLR, EMPTY_STRING, disname, diseaseAnchor);
                }).toList();
    }

    private static String getDiseaseAnchor(TermId diseaseId) {
        String diseaseURI = String.format("https://hpo.jax.org/app/browse/disease/%s",diseaseId.getValue());
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>",diseaseURI, diseaseId.getValue() );
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
        if (originalDiseaseName.length()>60) {
            originalDiseaseName = String.format("%s (...)", originalDiseaseName.substring(0,55) );
        }
        return originalDiseaseName;
    }



    private SparklinePacket(int rank, String posttest, String spark, double compLR, String sym, String disname, String diseaseAnchor){
        this.rank = rank;
        this.posttestBarSvg = posttest;
        this.sparklineSvg = spark;
        this.geneSparklineSvg = EMPTY_STRING;
        this.compositeLikelihoodRatio = Math.log10(compLR);
        this.geneSymbol = sym;
        this.diseaseName = disname;
        this.diseaseAnchor = diseaseAnchor;
    }

    private SparklinePacket(int rank, String posttest, String spark, String geneSpark, double compLR, String sym, String disname, String diseaseAnchor){
        this.rank = rank;
        this.posttestBarSvg = posttest;
        this.sparklineSvg = spark;
        this.geneSparklineSvg = geneSpark;
        this.compositeLikelihoodRatio = Math.log10(compLR);
        this.geneSymbol = sym;
        this.diseaseName = disname;
        this.diseaseAnchor = diseaseAnchor;
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

    public String getDiseaseAnchor() {
        return diseaseAnchor;
    }

    public String getGeneSparklineSvg() {
        return geneSparklineSvg;
    }
}
