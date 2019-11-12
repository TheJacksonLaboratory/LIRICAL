package org.monarchinitiative.lirical.svg;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.util.JsonFormat;
import org.antlr.v4.runtime.atn.SemanticContext;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;


/**
 * Make a set of sparklines for the top of the page. Note that in the "main" HPO/Gene plots for each disease, each
 * of the the features orders the HPO according to the individual likelihood ratios, but for the sparklines we want
 * to have the same order for each case.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Sparkline2Svg extends Lirical2Svg {
    private static final Logger logger = LoggerFactory.getLogger(Sparkline2Svg.class);
    /** width required to write the percentage to the left of the bars. */
    private final static int PERCENTAGE_WIDTH = 30;
    /** Amount of whitespace to put between the percentage and the bars. */
    private final static int SPACING_WIDTH = 10;
    /** Width of an individual bar representing one HPO term. */
    private final static int BAR_WIDTH = 10;
    /** Amount of space between two successive bars. */
    private final static int INTERBAR_WIDTH = 6;

    private final static int MAXIMUM_BAR_HEIGHT = 20;

    private final static int POSTTEST_BAR_HEIGHT = 40;

    private final List<TermId> termIdList;
    private final List<TermId> excludedTermIdList;

    int POSTTEST_WIDTH = 150;
    int POSTTEST_HEIGHT = 50;

    private final int total_width;

    private final int total_height;

    private final boolean hasGenotype;


    private final List<TermId> originalTerms;
    /** The HPO terms (observed and excluded) in the order we will use them for all sparklines. */
    private final List<TermId> orderedTerms;
    /** The indices of the original terms according to ordered terms. */
    private final List<Integer> indicesObserved;
    private final List<Integer> indicesExcluded;

    /**
     * Set up the Sparkline2Svg generator by sorting the HPO terms according to their likelihood
     * ratios in the top diagnosis (diseaseId).
     * @param hcase A representation of the Case
     * @param diseaseId The id of the disease at rank 1.
     */
    public Sparkline2Svg(HpoCase hcase, TermId diseaseId, boolean useGenotype) {
        this.termIdList = hcase.getObservedAbnormalities();
        this.excludedTermIdList = hcase.getExcludedAbnormalities();
        Map<TermId, Double> unsortedmap = new HashMap<>();
        Map<TermId, Double> unsortedexcludedmap = new HashMap<>();
        TestResult result = hcase.getResult(diseaseId);
        this.hasGenotype = useGenotype;
        double max=0;
        ImmutableList.Builder<TermId> bilder = new ImmutableList.Builder<>();
        for (int i=0;i<termIdList.size();i++) {
            TermId tid = termIdList.get(i);
            bilder.add(tid);
            double ratio = result.getObservedPhenotypeRatio(i);
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
            double lgratio = Math.log10(ratio);
            unsortedmap.put(tid,lgratio);
        }
        for (int i=0;i<excludedTermIdList.size();i++) {
            TermId tid = excludedTermIdList.get(i);
            bilder.add(tid);
            double ratio = result.getExcludedPhenotypeRatio(i);
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
            double lgratio = Math.log10(ratio);
            unsortedexcludedmap.put(tid,lgratio);
        }
        originalTerms = bilder.build();
        Map<TermId, Double> sortedmap = sortByValue(unsortedmap);
        Map<TermId, Double> sortedexcludedmap = sortByValue(unsortedexcludedmap);
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        int i=0;
        for (TermId tid : sortedmap.keySet()) {
            builder.add(tid);
        }
        for (TermId tid : sortedexcludedmap.keySet()) {
            builder.add(tid);
        }
        orderedTerms = builder.build();
        ImmutableList.Builder<Integer> bldr = new ImmutableList.Builder<>();
        for (TermId tid : termIdList) {
            int idx = getIndexObserved(tid);
            bldr.add(idx);
        }
        this.indicesObserved = bldr.build();
        bldr = new ImmutableList.Builder<>(); // reset
        for (TermId tid : excludedTermIdList) {
            int idx = getIndexExcluded(tid);
            bldr.add(idx);
        }
        this.indicesExcluded = bldr.build();
        // calculate total width of the SVG
        int genotypeWidth = 0;
        if (useGenotype) {
            genotypeWidth += BAR_WIDTH + INTERBAR_WIDTH;
        }
        total_width = PERCENTAGE_WIDTH + SPACING_WIDTH + orderedTerms.size() * (BAR_WIDTH + INTERBAR_WIDTH) + genotypeWidth;
        total_height = 2*MAXIMUM_BAR_HEIGHT + 10;
    }
    /** Not pretty but it works for now */
    private int getIndexObserved(TermId tid) {
        for (int i=0; i< this.termIdList.size(); i++) {
            if (tid.equals(this.termIdList.get(i)))
                return i;
        }
        // should never reach here
        logger.error("Could not find index of TermId {}", tid.getValue());
        return -1; // should never happen
    }

    /** Not pretty but it works for now */
    private int getIndexExcluded(TermId tid) {
        for (int i=0; i< this.excludedTermIdList.size(); i++) {
            if (tid.equals(this.excludedTermIdList.get(i)))
                return i;
        }
        // should never reach here
        logger.error("Could not find index of TermId {}", tid.getValue());
        return -1; // should never happen
    }



    public String getSparklineSvg(HpoCase hcase, TermId diseaseId) {
        try {
            StringWriter swriter = new StringWriter();
            writeHeader(swriter);
            writeSparkline(hcase, diseaseId, swriter);
            writeFooter(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }

    private void writeSparkline(HpoCase hcase, TermId diseaseId, StringWriter swriter) {
        final int MAX_LOG_LR = 4;
        // get the posttest probability
        TestResult result = hcase.getResult(diseaseId);
        double ptprob = result.getPosttestProbability();
        String posttestPercentage = String.format("%.0f%%", ptprob);
        int ybaseline = total_height / 2; // put everything right in the middle
        int xstart = 5;
        int linewidth = this.orderedTerms.size() * (BAR_WIDTH + INTERBAR_WIDTH) - INTERBAR_WIDTH;
        if (hasGenotype) {
            linewidth += BAR_WIDTH + INTERBAR_WIDTH;
        }
        int currentX = xstart + PERCENTAGE_WIDTH + SPACING_WIDTH;
        swriter.write("<line fill=\"none\" stroke=\"" + RED + "\" stroke-width=\"2\" " +
                "x1=\"" + currentX + "\" y1=\"" + ybaseline + "\" x2=\"" + (currentX+linewidth) +
                "\" y2=\"" + ybaseline + "\"/>\n");
        for (int i=0; i<indicesObserved.size(); i++) {
            double logratio = Math.log10(result.getObservedPhenotypeRatio(i));
            if (logratio > 0) {
                logratio = Math.min(MAX_LOG_LR, logratio);
                int height = (int)(logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                int ypos = ybaseline - height;
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos +"\" x=\"" + currentX + "\" " +
                                "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + BLACK + "\"/>\n");
            } else {
                logratio = Math.max((-1)*MAX_LOG_LR, logratio);
                int height = (int)((-1)*logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline +"\" x=\"" + currentX + "\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + BLACK + "\"/>\n");
            }
            currentX += BAR_WIDTH + INTERBAR_WIDTH;
        }
        for (int i=0; i<indicesExcluded.size(); i++) {
            double logratio = Math.log10(result.getExcludedPhenotypeRatio(i));
            if (logratio > 0) {
                logratio = Math.min(MAX_LOG_LR, logratio);
                int height = (int)(logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                int ypos = ybaseline - height;
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos +"\" x=\"" + currentX + "\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + BLACK + "\"/>\n");
            } else {
                logratio = Math.max((-1)*MAX_LOG_LR, logratio);
                int height = (int)((-1)*logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline +"\" x=\"" + currentX + "\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + BLACK + "\"/>\n");
            }
            currentX += BAR_WIDTH + INTERBAR_WIDTH;
        }
        if (result.hasGenotype()) {
            double logratio = Math.log10(result.getGenotypeLR());
            if (logratio > 0) {
                logratio = Math.min(MAX_LOG_LR, logratio);
                int height = (int)(logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                int ypos = ybaseline - height;
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos +"\" x=\"" + currentX + "\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + GREEN + "\"/>\n");
            } else {
                logratio = Math.max((-1)*MAX_LOG_LR, logratio);
                int height = (int)((-1)*logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline +"\" x=\"" + currentX + "\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"" + GREEN + "\"/>\n");
            }
        }


    }


    private void writeHeader(Writer writer) throws IOException {
        writeHeader(writer, total_width, total_height);
    }

    private void writeHeader(Writer writer, int width, int height) throws IOException {
        writer.write("<svg width=\""+width+"\" height=\""+height+"\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by LIRICAL -->\n");
        writer.write("<g>\n");
    }



    public String getPosttestBar(double ptprob) {
        try {
            StringWriter swriter = new StringWriter();
            int WIDTH_FUDGE_FACTOR = 40;
            writeHeader(swriter, POSTTEST_WIDTH + WIDTH_FUDGE_FACTOR, POSTTEST_HEIGHT);
            writePosttestBar(ptprob, swriter);
            writeFooter(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }

    private void writePosttestBar(double ptprob, Writer writer) throws IOException {
        int x = 0;
        String posttestPercentage = String.format("%.0f%%", ptprob*100.0);
        writer.write("<text x=\"10%\" y=\"70%\" dominant-baseline=\"middle\" text-anchor=\"middle\"  font-size=\"18\">" + posttestPercentage + "</text>\n");
        x += PERCENTAGE_WIDTH + SPACING_WIDTH;
        int total_remaining_width = POSTTEST_WIDTH - x;
        int barwidth = (int) (total_remaining_width * ptprob);
        // x and y are the left and top of the rectangle
        writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=25 x=\"25%%\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\"/>\n",
                BOX_HEIGHT,
                barwidth,
                ORANGE));
    }

}
