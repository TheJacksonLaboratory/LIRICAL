package org.monarchinitiative.lirical.io.output;


import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.io.output.svg.Sparkline2Svg;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
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

    private final static String EMPTY_STRING = "";

    private final int rank;
    private final String posttestBarSvg;
    private final String sparklineSvg;
    private final String geneSparklineSvg;
    private final double compositeLikelihoodRatio;
    private final String geneSymbol;
    private final String diseaseName;
    private final String diseaseAnchor;

    /**
     * Factory method for genotype-phenotype analysis.
     */
    public static List<SparklinePacket> sparklineFactory(AnalysisResults results,
                                                         HpoDiseases diseases,
                                                         MinimalOntology ontology,
                                                         int N) {
        if (results.isEmpty())
            return List.of();

        List<SparklinePacket> packets = new ArrayList<>(N);
        // we're checking that results are not empty
        //noinspection OptionalGetWithoutIsPresent
        TestResult topResult = results.resultsWithDescendingPostTestProbability().findFirst().get();
        AtomicInteger rank = new AtomicInteger();
        Map<TermId, HpoDisease> diseaseById = diseases.diseaseById();
        Sparkline2Svg sparkline2Svg = new Sparkline2Svg(topResult, true, ontology);
        results.resultsWithDescendingPostTestProbability()
                .limit(N)
                .forEachOrdered(result -> {
                    double posttestProb = result.posttestProbability();
                    String posttestSVG = sparkline2Svg.getPosttestBar(posttestProb);
                    Optional<GenotypeLrWithExplanation> genotypeLr = result.genotypeLr();
                    String geneSymbol = genotypeLr.map(GenotypeLrWithExplanation::geneId)
                            .map(GeneIdentifier::symbol)
                            .orElse(EMPTY_STRING);
                    String sparkSVG = sparkline2Svg.getSparklineSvg(geneSymbol, result);
                    double compositeLR = result.getCompositeLR();

                    HpoDisease disease = diseaseById.get(result.diseaseId());
                    String diseaseName = prettifyDiseaseName(disease.diseaseName());
                    TermId diseaseId = result.diseaseId();
                    String diseaseAnchor = getDiseaseAnchor(diseaseId);
                    String geneSparkSvg = genotypeLr.isPresent() ? sparkline2Svg.getGeneSparklineSvg(results, diseaseId, geneSymbol) : EMPTY_STRING;
                    SparklinePacket sp = new SparklinePacket(rank.incrementAndGet(), posttestSVG, sparkSVG, compositeLR, geneSymbol, diseaseName, diseaseAnchor, geneSparkSvg);
                    packets.add(sp);
                });

        return packets;
    }

    private static String getDiseaseAnchor(TermId diseaseId) {
        String diseaseURI = String.format("https://hpo.jax.org/app/browse/disease/%s", diseaseId.getValue());
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", diseaseURI, diseaseId.getValue());
    }


    /**
     * This method shortens items such as #101200 APERT SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE I; ACS1;;ACS IAPERT-CROUZON DISEASE, INCLUDED;;
     * to simply APERT SYNDROME
     *
     * @param originalDiseaseName original String from HPO database, derived from OMIM and potentially historic
     * @return simplified and prettified name
     */
    private static String prettifyDiseaseName(String originalDiseaseName) {
        // shorten the name to everything up to the first semicolon
        int i = originalDiseaseName.indexOf(";");
        if (i > 0) {
            originalDiseaseName = originalDiseaseName.substring(0, i);
        }
        originalDiseaseName = originalDiseaseName.trim();
        final Pattern omimid = Pattern.compile("#?\\d{6}");
        Matcher m = omimid.matcher(originalDiseaseName);
        if (m.find(0)) {
            // last position of match
            i = m.end() + 1;
            originalDiseaseName = originalDiseaseName.substring(i).trim();
        }
        if (originalDiseaseName.length() > 60) {
            originalDiseaseName = String.format("%s (...)", originalDiseaseName.substring(0, 55));
        }
        return originalDiseaseName;
    }

    private SparklinePacket(int rank,
                            String posttest,
                            String spark,
                            double compLR,
                            String geneSymbol,
                            String diseaseName,
                            String diseaseAnchor,
                            String geneSparklineSvg) {
        this.rank = rank;
        this.posttestBarSvg = posttest;
        this.sparklineSvg = spark;
        this.compositeLikelihoodRatio = Math.log10(compLR);
        this.geneSymbol = geneSymbol;
        this.diseaseName = diseaseName;
        this.diseaseAnchor = diseaseAnchor;
        this.geneSparklineSvg = geneSparklineSvg; // is EMPTY_STRING if no gene data is available
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
