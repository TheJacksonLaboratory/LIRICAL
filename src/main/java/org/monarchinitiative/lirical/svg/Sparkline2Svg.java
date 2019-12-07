package org.monarchinitiative.lirical.svg;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lirical.hpo.HpoCase;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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


    private final List<TermId> termIdList;
    private final List<TermId> excludedTermIdList;
    private final List<String> observedTermToolTipLabels;
    private final List<String> excludedTermToolTipLabels;

    private final int POSTTEST_WIDTH = 150;
    private final int POSTTEST_HEIGHT = 50;

    private final int total_width;

    private final int total_height;

    private final boolean hasGenotype;

    private final int n_hpo_terms;
    /** The indices of the original terms according to ordered terms. */
    private final List<Integer> indicesObserved;
    private final List<Integer> indicesExcluded;

    /**
     * Set up the Sparkline2Svg generator by sorting the HPO terms according to their likelihood
     * ratios in the top diagnosis (diseaseId).
     * @param hcase A representation of the Case
     * @param diseaseId The id of the disease at rank 1.
     */
    public Sparkline2Svg(HpoCase hcase, TermId diseaseId, boolean useGenotype, Ontology ontology) {
        this.termIdList = hcase.getObservedAbnormalities();
        this.excludedTermIdList = hcase.getExcludedAbnormalities();
        observedTermToolTipLabels = new ArrayList<>();
        for (TermId t : this.termIdList) {
            String label = ontology.getTermMap().get(t).getName();
            String tooltip = String.format("%s [%s]", label, t.getValue());
            this.observedTermToolTipLabels.add(tooltip);
        }
        excludedTermToolTipLabels = new ArrayList<>();
        for (TermId t : this.excludedTermIdList) {
            String label = ontology.getTermMap().get(t).getName();
            String tooltip = String.format("%s [%s]", label, t.getValue());
            this.excludedTermToolTipLabels.add(tooltip);
        }
        TestResult result = hcase.getResult(diseaseId);
        this.hasGenotype = useGenotype;
        // Sort the HPO findings according to lieklihood ratio.
        // use the internal valss Value2Index to keep track of the original index
        // after sorting, put the sorted original indices (i.e., as a sorted permutation) into
        // the list "indicesObserved"
        List<Value2Index> observedIndexList = new ArrayList<>();
        for (int i=0;i<termIdList.size();i++) {
            double ratio = result.getObservedPhenotypeRatio(i);
            observedIndexList.add(new Value2Index(i,ratio));
        }
        Collections.sort(observedIndexList);
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        for (int i = 0; i<observedIndexList.size(); i++) {
            builder.add(observedIndexList.get(i).getOriginalIndex());
        }
        this.indicesObserved = builder.build();
        // Now do the same for the excluded HPO terms
        List<Value2Index> excludedIndexList = new ArrayList<>();
        for (int i=0;i<excludedTermIdList.size();i++) {
            double ratio = result.getExcludedPhenotypeRatio(i);
            excludedIndexList.add(new Value2Index(i, ratio));
        }
        Collections.sort(excludedIndexList);

        builder = new ImmutableList.Builder<>();
        for (int i = 0; i<excludedIndexList.size(); i++) {
            builder.add(excludedIndexList.get(i).getOriginalIndex());
        }
        this.indicesExcluded =  builder.build();
        this.n_hpo_terms = indicesExcluded.size() + indicesObserved.size();
        // calculate total width of the SVG
        total_width = SPACING_WIDTH + n_hpo_terms * (BAR_WIDTH + INTERBAR_WIDTH);
        total_height = 2*MAXIMUM_BAR_HEIGHT + 10;
    }

    /**
     * This is used by Phenotype only output. We supply an empty string instead of a gene symbol. Note that
     * because the test results know that they do not have a genotype, this will not affect the output.
     * @param hcase The case we are writing output for
     * @param diseaseId ID of the current disease
     * @return an SVG string
     */
    public String getSparklineSvg(HpoCase hcase, TermId diseaseId) {
        String EMPTY_STRING = "";
        return getSparklineSvg(hcase, diseaseId, EMPTY_STRING);
    }

    public String getSparklineSvg(HpoCase hcase, TermId diseaseId, String gsymbol) {
        try {
            StringWriter swriter = new StringWriter();
            writeHeader(swriter);
            writeSparkline(hcase, diseaseId, swriter, gsymbol);
            writeFooter(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }

    public String getGeneSparklineSvg(HpoCase hcase, TermId diseaseId, String gsymbol) {
        try {
            StringWriter swriter = new StringWriter();
            //writeHeader(swriter);
            int geneSvgWidth = 150;
            writeHeader(swriter, geneSvgWidth, total_height);
            double LR = hcase.getResult(diseaseId).getGenotypeLR();
            writeGeneSpark(swriter, gsymbol, LR);
            writeFooter(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }


    private void writeGeneSpark(StringWriter swriter, String geneSymbol, double LR) throws IOException {
        final int MAX_LOG_LR = 4;
        int ybaseline = total_height / 2; // put everything right in the middle
        int xstart = 5;
        //int GENE_SYMBOL_WIDTH = 40;
        int linewidth = BAR_WIDTH + 2 * INTERBAR_WIDTH;
        swriter.write("<line fill=\"none\" stroke=\"" + BLACK + "\" stroke-width=\"1\" " +
                "x1=\"" + xstart + "\" y1=\"" + ybaseline + "\" x2=\"" + (xstart + linewidth) +
                "\" y2=\"" + ybaseline + "\"/>\n");
        int currentX = xstart + linewidth/2 - BAR_WIDTH/2;
        double logratio = Math.log10(LR);
        if (logratio > 0) {
            logratio = Math.min(MAX_LOG_LR, logratio);
            int height = (int) (logratio * (MAXIMUM_BAR_HEIGHT / MAX_LOG_LR));
            int ypos = ybaseline - height;
            if (height == 0) {
                writeSmallDiamond(swriter, currentX, ybaseline, geneSymbol);
            } else {
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos + "\" x=\"" + currentX + "\" " +
                        "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + BRIGHT_GREEN + "\" onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + geneSymbol + "')\"/>\n");
            }
        } else {
            logratio = Math.max((-1) * MAX_LOG_LR, logratio);
            int height = (int) ((-1) * logratio * (MAXIMUM_BAR_HEIGHT / MAX_LOG_LR));
            if (height == 0) {
                writeSmallDiamond(swriter, currentX, ybaseline, geneSymbol);
            } else {
                swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline + "\" x=\"" + currentX + "\" " +
                        "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + RED + "\"  onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + geneSymbol + "')\"/>\n");
            }
        }
        currentX += linewidth/2 + 2*BAR_WIDTH;
        swriter.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"18px\" font-style=\"italic\">%s</text>\n",
                currentX,
                ybaseline,
                geneSymbol));
    }


    /**
     * Create a String with SVG code for the sparkline
     * @param hcase The current case
     * @param diseaseId Disease for which we are writing the sparkline
     * @param swriter file handle (String writer)
     * @param geneSymbol symbol of gene associated with disease (or EMPTY STRING if there is no gene)
     * @throws IOException if there is an issue creating the SVG
     */
    private void writeSparkline(HpoCase hcase, TermId diseaseId, StringWriter swriter, String geneSymbol) throws IOException {
        final int MAX_LOG_LR = 4;
        // get the posttest probability
        TestResult result = hcase.getResult(diseaseId);
        int ybaseline = total_height / 2; // put everything right in the middle
        int xstart = 10;
        int linewidth =  n_hpo_terms * (BAR_WIDTH + INTERBAR_WIDTH) - INTERBAR_WIDTH;
        int currentX = xstart;
        swriter.write("<line fill=\"none\" stroke=\"" + BLACK + "\" stroke-width=\"1\" " +
                "x1=\"" + currentX + "\" y1=\"" + ybaseline + "\" x2=\"" + (currentX+linewidth) +
                "\" y2=\"" + ybaseline + "\"/>\n");
        for (int i=0; i<indicesObserved.size(); i++) {
            int originalIndex = indicesObserved.get(i);
            String msg = this.observedTermToolTipLabels.get(originalIndex);
            double logratio = Math.log10(result.getObservedPhenotypeRatio(originalIndex));
            if (logratio > 0) {
                logratio = Math.min(MAX_LOG_LR, logratio);
                int height = (int)(logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                int ypos = ybaseline - height;
                if (height == 0) {
                    writeSmallDiamond(swriter,currentX,ypos,msg);
                } else {
                    swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos + "\" x=\"" + currentX + "\" " +
                            "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + BRIGHT_GREEN + "\" onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + msg + "')\"/>\n");
                }
            } else {
                logratio = Math.max((-1)*MAX_LOG_LR, logratio);
                int height = (int)((-1)*logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                if (height == 0) {
                    writeSmallDiamond(swriter,currentX,ybaseline,msg);
                } else {
                    swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline + "\" x=\"" + currentX + "\" " +
                            "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + RED + "\" onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + msg + "')\"/>\n");
                }
            }
            currentX += BAR_WIDTH + INTERBAR_WIDTH;
        }
        for (int i=0; i<indicesExcluded.size(); i++) {
            int originalIndex = indicesExcluded.get(i);
            String msg = this.excludedTermToolTipLabels.get(originalIndex);
            double logratio = Math.log10(result.getExcludedPhenotypeRatio(originalIndex));
            if (logratio > 0) {
                logratio = Math.min(MAX_LOG_LR, logratio);
                int height = (int)(logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                int ypos = ybaseline - height;
                if (height == 0) {
                    writeSmallDiamond(swriter, currentX, ybaseline, msg);
                } else {
                    swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ypos + "\" x=\"" + currentX + "\" " +
                            "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + BRIGHT_GREEN + "\" onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + msg + "')\"/>\n");
                }
            } else {
                logratio = Math.max((-1)*MAX_LOG_LR, logratio);
                int height = (int)((-1)*logratio *(MAXIMUM_BAR_HEIGHT/MAX_LOG_LR));
                if (height == 0) {
                    writeSmallDiamond(swriter, currentX, ybaseline, msg);
                } else {
                    swriter.write("<rect height=\"" + height + "\" width=\"" + BAR_WIDTH + "\" y=\"" + ybaseline + "\" x=\"" + currentX + "\" " +
                            "stroke-width=\"0\" stroke=\"#000000\" fill=\"" + RED + "\" onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + msg + "')\"/>\n");
                }
            }
            currentX += BAR_WIDTH + INTERBAR_WIDTH;
        }
    }

    /**
     * We use a diamond symbol to show a value that would be too small to appear as a visible box.
     * We do this bothfor the likelihood ratio as well as for the post-test probability
     */
    private void writeSmallDiamond(Writer writer,int X, int Y, String msg) throws IOException
    {
        int diamondsize=4;
        Y -= diamondsize;
        X += diamondsize;
        writer.write(String.format("<polygon " +
                        "points=\"%d,%d %d,%d %d,%d %d,%d\" style=\"fill:grey;stroke:%s;stroke-width:1\"  onmouseout=\"hideTooltip();\" onmouseover=\"showTooltip(evt,'" + msg + "')\"/>\n",
                X,
                Y,
                X+diamondsize,
                Y+diamondsize,
                X,
                Y+2*diamondsize,
                X-diamondsize,
                Y+diamondsize,
                BROWN));
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
                        "stroke-width=\"0\" stroke=\"#000000\" fill=\"%s\"/>\n",
                BOX_HEIGHT,
                barwidth,
                BRIGHT_GREEN));
    }

    /**
     * A convenience class to allow us to keep track of the indices of the observed and excluded HPO terms. We want
     * to sort each class from highest to lowest likelihood ratio for the top candidate and maintain this ordering
     * for all sparklines. The order of the HPO terms in a TestResult is fixed and identical for all TestResult objects.
     * We need to sort these to get the desired order.
     */
    private static class Value2Index implements Comparable<Value2Index> {
        private final int originalIndex;
        private final double LR;
        Value2Index(int originalIndex, double LR) {
            this.originalIndex = originalIndex;
            this.LR = LR;
        }

        int getOriginalIndex() {
            return originalIndex;
        }

        @Override
        public int compareTo(Value2Index other) {
            return this.LR < other.LR ? 1 : this.LR == other.LR ? 0 : -1;
        }
    }

}
