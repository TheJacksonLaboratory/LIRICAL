package org.monarchinitiative.lirical.output.svg;


import com.google.common.collect.ImmutableList;
import org.monarchinitiative.lirical.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * This class creates an SVG file representing the results of likelihood ratio analysis of an HPO case.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Lr2Svg extends Lirical2Svg {
    private static final Logger logger = LoggerFactory.getLogger(Lr2Svg.class);
    /**
     * An object representing the Human Phenotype Ontology
     */
    private final Ontology ontology;
    /**
     * We show the results as an SVG diagram for this disease.
     */
    private final TermId diseaseCURIE;
    /**
     * This is the name (label) of the disease.
     */
    private final String diseaseName;
    /**
     * If applicable, the gene symbol associated with {@link #diseaseCURIE}.
     */
    private final String geneSymbol;
    /**
     * This is the {@link TestResult} object that corresponds to {@link #diseaseCURIE} being displayed as SVG.
     */
    private final TestResult result;
    private final int rank;
    /**
     * Height of entire image in px
     */
    private int HEIGHT = 480;
    /**
     * width of the bars part of the image in px
     */
    private final int WIDTH = 500;
    /**
     * additional width of the text part of the image in px
     */
    private final static int TEXTPART_WIDTH = 500;

    /**
     * The middle line is the central line around which the likelihood ratio 'bars' will be drawn. This
     * variable is calculated as the height that this bar will need to have in order to show all of the
     * likelihood ratio bars.
     */
    private int heightOfMiddleLine;

    /** The indices of the original terms according to ordered terms. */
    private final List<Integer> indicesObserved;
    private final List<Integer> indicesExcluded;
    private final List<LrWithExplanation> observedTerms;
    private final List<LrWithExplanation> excludedTerms;

    private final double maximumIndividualLR;

    /**
     * Constructor to draw an SVG representation of the phenotype and genotype likelihood ratios
     * @param result              The test result
     * @param rank
     * @param diseaseId           The current differential diagnosis id (e.g., OMIM:600123)
     * @param originalDiseaseName The current differential diagnosis name
     * @param ont                 Reference to HPO ontology
     * @param symbol              Gene symbol (if any, can be null)
     */
    public Lr2Svg(TestResult result,
                  int rank,
                  TermId diseaseId,
                  String originalDiseaseName,
                  Ontology ont,
                  String symbol) {
        this.diseaseCURIE = diseaseId;
        this.diseaseName = prettifyDiseaseName(originalDiseaseName);
        this.result = result;
        this.geneSymbol = symbol;
        this.ontology = ont;
        this.rank = rank;
        this.determineTotalHeightOfSvg();

        // Sort the HPO findings according to lieklihood ratio.
        // use the internal valss Value2Index to keep track of the original index
        // after sorting, put the sorted original indices (i.e., as a sorted permutation) into
        // the list "indicesObserved"
        List<Value2Index> observedIndexList = new ArrayList<>();
        this.observedTerms = result.observedResults();
        for (int i = 0; i< observedTerms.size(); i++) {
            double ratio = result.getObservedPhenotypeRatio(i);
            observedIndexList.add(new Value2Index(i,ratio));
        }
        Collections.sort(observedIndexList);
        ImmutableList.Builder<Integer> builder = new ImmutableList.Builder<>();
        for (Value2Index value2Index : observedIndexList) {
            builder.add(value2Index.getOriginalIndex());
        }
        this.indicesObserved = builder.build();
        // Now do the same for the excluded HPO terms
        this.excludedTerms = result.excludedResults();
        List<Value2Index> excludedIndexList = new ArrayList<>();
        for (int i = 0; i< excludedTerms.size(); i++) {
            double ratio = result.getExcludedPhenotypeRatio(i);
            excludedIndexList.add(new Value2Index(i, ratio));
        }
        Collections.sort(excludedIndexList);

        builder = new ImmutableList.Builder<>();
        for (Value2Index value2Index : excludedIndexList) {
            builder.add(value2Index.getOriginalIndex());
        }
        this.indicesExcluded =  builder.build();
        this.maximumIndividualLR = result.getMaximumIndividualLR();
    }


    /**
     * This function determines the vertical dimension of the SVG that we will org.monarchinitiative.lirical.output.
     */
    private void determineTotalHeightOfSvg() {
        this.heightOfMiddleLine = calculateHeightOfMiddleLine();
        // The following adds sufficient height to include the remaining elements of the SVG
        HEIGHT = this.heightOfMiddleLine + 4 * MIN_VERTICAL_OFFSET + 7 * BOX_OFFSET;
    }

    /**
     * This method can be used to output the SVG code to any Java Writer. Currently,
     * we use this with a StringWriter to include the code in the HTML output (see {@link #getSvgString()}).
     *
     * @param writer Handle to a Writer object
     * @throws IOException If there is an IO error
     */
    private void writeSvg(Writer writer) throws IOException {
        writeHeader(writer);
        writeVerticalLine(writer);
        writeLrBoxes(writer);
        writeFooter(writer);
    }

    public String getSvgString() {
        try {
            StringWriter swriter = new StringWriter();
            writeSvg(swriter);
            return swriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ""; // return empty string upon failure
    }

    public void writeSvg(String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writeSvg(writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to write SVG file: " + path);
            e.printStackTrace();
        }
    }

    /**
     * Writes a horizontal scale ("X axis") with tick points.
     *
     * @param writer  File handle
     * @param maxAmp  maximum amplitude of the likelihood ratio on a long10 scale
     * @param scaling proportion of width that should be taken up by the scale
     * @throws IOException if there is an issue writing the SVG code
     */
    private void writeScale(Writer writer, double maxAmp, double scaling) throws IOException {
        int Y = heightOfMiddleLine + MIN_VERTICAL_OFFSET + BOX_OFFSET * 4;
        int maxTick = (int) Math.ceil(maxAmp);
        int maxX = (int) (maxTick * scaling);
        int midline = WIDTH / 2;
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " + "x1=\"" + (midline - maxX) + "\" y1=\"" + Y + "\" x2=\"" + (midline + maxX) + "\" y2=\"" + Y + "\"/>\n");
        int block = maxX / maxTick;
        for (int i = 1; i <= maxTick; ++i) {
            int offset = block * i;
            // negative tick
            writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " + "x1=\"" + (midline - offset) + "\" y1=\"" + (Y + 5) + "\" x2=\"" + (midline - offset) + "\" y2=\"" + (Y - 5) + "\"/>\n");
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">-%d</text>\n", (midline - offset), Y + 15, i));
            // positive tick
            writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " + "x1=\"" + (midline + offset) + "\" y1=\"" + (Y + 5) + "\" x2=\"" + (midline + offset) + "\" y2=\"" + (Y - 5) + "\"/>\n");
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%d</text>\n", (midline + offset), Y + 15, i));
        }
        // Now get tick at the zero point
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " + "x1=\"" + (midline) + "\" y1=\"" + (Y + 5) + "\" x2=\"" + (midline) + "\" y2=\"" + (Y - 5) + "\"/>\n");
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">0</text>\n", (midline), Y + 15));


        double ptp = result.posttestProbability();
        String diseaseLabel = String.format("%s [%s]", diseaseName, diseaseCURIE.getValue());
        String diseaseResult = String.format("Rank: #%d Posttest probability: %.1f%%", rank, (100.0 * ptp));
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"16px\" font-weight=\"bold\">%s</text>\n", (midline - (maxTick - 1) * block), Y + 35, diseaseLabel));
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"16px\" font-weight=\"bold\">%s</text>\n", (midline - (maxTick - 1) * block), Y + 55, diseaseResult));

    }

    public int rank() {
        return rank;
    }

    /**
     * Writes the set of boxes representing the log10 amplitudes of the likelihood ratios for individual
     * features.
     *
     * @param writer File handle
     * @throws IOException if there is an issue writing the SVG code
     */
    private void writeLrBoxes(Writer writer) throws IOException {
        int currentY = MIN_VERTICAL_OFFSET + BOX_OFFSET * 2;
        int midline = WIDTH / 2;
        int XbeginOfText = WIDTH - 30;
        // maximum amplitude of the bars
        // we want it to be at least 10_000
        double maxAmp = Math.max(4.0, Math.log10(this.maximumIndividualLR));
        // we want the maximum amplitude to take up 80% of the space
        // the available space starting from the center line is WIDTH/2
        // and so we calculate a factor
        double scaling = (0.4 * WIDTH) / maxAmp;
        int explanationIndex = 0;
        for (int originalIndex : indicesObserved) {
            TermId tid = this.observedTerms.get(originalIndex).queryTerm();
            double ratio = result.getObservedPhenotypeRatio(originalIndex);
            ratio = Math.log10(ratio);
            double boxwidth = ratio * scaling;
            double xstart = midline;
            if (ratio < 0) {
                boxwidth = Math.abs(boxwidth);
                xstart = midline - boxwidth;
            }
            if ((int) boxwidth == 0) {
                int X = (int) xstart;
                writeDiamond(writer, X, currentY, observedTerms.get(originalIndex).escapedExplanation());
            } else {
                // red for features that do not support the diagnosis, green for those that do
                String color = xstart < midline ? RED : BRIGHT_GREEN;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " + "stroke-width=\"0\" " +
                        "stroke=\"#000000\" fill=\"%s\" onmouseout=\"hideTooltip();\" " +
                        "onmouseover=\"showTooltip(evt,'%s')\"/>\n", BOX_HEIGHT, (int) boxwidth, currentY, (int) xstart, color, observedTerms.get(originalIndex).escapedExplanation()));
            }
            // add label of corresponding HPO term
            Term term = ontology.getTermMap().get(tid);
            String label = String.format("%s [%s]", term.getName(), tid.getValue());
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n", XbeginOfText, currentY + BOX_HEIGHT, label));
            currentY += BOX_HEIGHT + BOX_OFFSET;
            explanationIndex++;
        }
        // Now add negated terms if any
        explanationIndex = 0;
        for (int originalIndex : indicesExcluded) {
            TermId tid = this.excludedTerms.get(originalIndex).queryTerm();
            double ratio = result.getExcludedPhenotypeRatio(originalIndex);
            ratio = Math.log10(ratio);
            double boxwidth = ratio * scaling;
            double xstart = midline;
            if (ratio < 0) {
                boxwidth = Math.abs(boxwidth);
                xstart = 1 + midline - boxwidth;
            }
            if ((int) boxwidth == 0) {
                int X = (int) xstart;
                writeDiamond(writer, X, currentY, excludedTerms.get(originalIndex).escapedExplanation());
            } else {
                // red for features that do not support the diagnosis, green for those that do
                String color = xstart < midline ? RED : BRIGHT_GREEN;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                "stroke-width=\"0\" stroke=\"#000000\" fill=\"%s\" onmouseout=\"hideTooltip();\" " +
                                "onmouseover=\"showTooltip(evt,'%s')\"/>\n",
                        BOX_HEIGHT, (int) boxwidth, currentY, (int) xstart, color, excludedTerms.get(originalIndex).escapedExplanation()));
            }
            // add label of corresponding HPO term
            Term term = ontology.getTermMap().get(tid);
            String label = String.format("Excluded: %s [%s]", term.getName(), tid.getValue());
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n", XbeginOfText, currentY + BOX_HEIGHT, label));
            currentY += BOX_HEIGHT + BOX_OFFSET;
            explanationIndex++;
        }


        Optional<GenotypeLrWithExplanation> genotypeLr = result.genotypeLr();
        if (genotypeLr.isPresent()) {
            currentY += 0.5 * (BOX_HEIGHT + BOX_OFFSET);

            double ratio = genotypeLr.get().lr();
             double lgratio = Math.log10(ratio);
            String lrstring = String.format("LR: %.3f",lgratio);
            double boxwidth = lgratio * scaling;
            double xstart = midline;
            if (lgratio < 0) {
                boxwidth = Math.abs(boxwidth);
                xstart = 1 + midline - boxwidth;
            }
            if ((int) boxwidth == 0) {
                int X = (int) xstart;
                writeDiamond(writer, X, currentY, lrstring);
            } else {
                // red for features that do not support the diagnosis, green for those that do
                String color = xstart < midline ? RED : BRIGHT_GREEN;
                int X = (int) xstart;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                        "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\" onmouseout=\"hideTooltip();\" " +
                                "onmouseover=\"showTooltip(evt,'%s')\"/>\n",
                        BOX_HEIGHT, (int) boxwidth, currentY, X, color, lrstring));
            }
            // add label of Genotype
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"italic\">%s</text>\n", XbeginOfText, currentY + BOX_HEIGHT, geneSymbol));
        }
        maxAmp = Math.max(4.0, maxAmp); // show at least 10,000!
        writeScale(writer, maxAmp, scaling);
    }

    /**
     * The height of the middle line is the number of boxes times (BOX_HEIGHT + BOX_OFFSET)
     * plus one additional BOX_OFFSET on top.
     *
     * @return Where to place the line underneath the boxes.
     */
    private int calculateHeightOfMiddleLine() {
        Objects.requireNonNull(result);
        int n = result.getNumberOfTests();
        if (result.genotypeLr().isPresent()) {
            n += 2; // add another unit for the genotype
        }
        return 2 * BOX_OFFSET + n * (BOX_HEIGHT + BOX_OFFSET);
    }


    /**
     * Draw the central line around which the likelihood ratio 'bars' will be drawn.
     *
     * @param writer file handle
     * @throws IOException if we cannot write the SVG file.
     */
    private void writeVerticalLine(Writer writer) throws IOException {
        int midline = WIDTH / 2;
        int topY = MIN_VERTICAL_OFFSET;
        int bottomY = topY + calculateHeightOfMiddleLine();
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " + "x1=\"" + midline + "\" y1=\"" + topY + "\" x2=\"" + midline + "\" y2=\"" + bottomY + "\" id=\"svg_1\"/>\n");

    }

    private void writeHeader(Writer writer) throws IOException {
        int total_width = WIDTH + TEXTPART_WIDTH;
        writer.write("<svg width=\"" + total_width + "\" height=\"" + HEIGHT + "\" " + "xmlns=\"http://www.w3.org/2000/svg\" " + "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by LIRICAL -->\n");
        writer.write("<g>\n");
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
            return Double.compare(other.LR, this.LR);
        }
    }

}
