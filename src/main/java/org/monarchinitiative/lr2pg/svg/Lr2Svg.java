package org.monarchinitiative.lr2pg.svg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.*;

/**
 * This class creates an SVG file representing the results of likelihood ratio analysis of an HPO case.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Lr2Svg {
    private static final Logger logger = LogManager.getLogger();
    /** An object representing the Human Phenotype Ontology */
    private final Ontology ontology;
    /** This is the object that represents the case being analyzed with all results. */
    private final HpoCase hpocase;
    /** We show the results as an SVG diagram for this disease. */
    private final TermId diseaseCURIE;
    /** This is the name (label) of the disease. */
    private final String diseaseName;
    /** If applicable, the gene symbol associated with {@link #diseaseCURIE}.*/
    private final String geneSymbol;
    /** This is the {@link TestResult} object that corresponds to {@link #diseaseCURIE} being displayed as SVG. */
    private final TestResult result;
    /** Height of entire image in px */
    private int HEIGHT=480;
    /** width of the bars part of the image in px */
    private final int WIDTH=500;
    /** additional width of the text part of the image in px */
    private final static int TEXTPART_WIDTH=400;
    /** minimum distance to top of image of graphic elements */
    private final static int MIN_VERTICAL_OFFSET=10;
    /** distance between two adjacent "boxes" */
    private final static int BOX_OFFSET=10;

    private final static int BOX_HEIGHT=15;

    private final static String BLUE ="#4dbbd5";
    private final static String RED ="#e64b35";
    private final static String BROWN="#7e6148";
    /** The middle line is the central line around which the likelihood ratio 'bars' will be drawn. This
     * variable is calculated as the height that this bar will need to have in order to show all of the
     * likelihood ratio bars.
     */
    private int heightOfMiddleLine;

    /**
     * Constructor to draw an SVG representation of the phenotype and genotype likelihood ratios
     * @param hcase The proband (case) we are analyzing
     * @param diseaseId The current differential diagnosis id (e.g., OMIM:600123)
     * @param diseaseName  The current differential diagnosis name
     * @param ont Reference to HPO ontology
     * @param symbol Gene symbol (if any, can be null)
     */
    public Lr2Svg(HpoCase hcase, TermId diseaseId, String diseaseName, Ontology ont, String symbol) {
        this.hpocase=hcase;
        this.diseaseCURIE=diseaseId;
        // shorten the name to everything up to the first ;
        int i = diseaseName.indexOf(";");
        if (i>0) {
            this.diseaseName = diseaseName.substring(0,i);
        } else {
            this.diseaseName=diseaseName;
        }
        this.result = hpocase.getResult(diseaseId);
        this.geneSymbol = symbol;
        this.ontology=ont;
        determineTotalHeightOfSvg();
    }

    /**
     * This function determines the vertical dimension of the SVG that we will org.monarchinitiative.lr2pg.output.
     */
    private void determineTotalHeightOfSvg() {
        this.heightOfMiddleLine=calculateHeightOfMiddleLine();
        // The following adds sufficient height to include the remaining elements of the SVG
        HEIGHT = this.heightOfMiddleLine + 4*MIN_VERTICAL_OFFSET + 7*BOX_OFFSET;
    }

    /**
     * This method can be used to output the SVG code to any Java Writer. Currently,
     * we use this with a StringWriter to include the code in the HTML output (see {@link #getSvgString()}).
     * @param writer Handle to a Writer object
     * @throws IOException If there is an IO error
     */
    private void writeSvg(Writer writer)  throws IOException {
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
            System.err.println("[ERROR] Unable to write SVG file: "+path);
            e.printStackTrace();
        }
    }

    /**
     * Writes a horizontal scale ("X axis") with tick points.
     * @param writer File handle
     * @param maxAmp maximum amplitude of the likelihood ratio on a long10 scale
     * @param scaling proportion of width that should be taken up by the scale
     * @throws IOException if there is an issue writing the SVG code
     */
    private void writeScale(Writer writer, double maxAmp, double scaling) throws IOException {
        int Y = heightOfMiddleLine +  MIN_VERTICAL_OFFSET + BOX_OFFSET*4;
        int maxTick = (int) Math.ceil(maxAmp);
        int maxX = (int) (maxTick * scaling);
        int midline=WIDTH/2;
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " +
                "x1=\""+(midline- maxX)+"\" y1=\""+Y+"\" x2=\""+(midline+ maxX)+"\" y2=\""+Y+"\"/>\n");
        int block = maxX /maxTick;
        for (int i=1;i<=maxTick;++i) {
            int offset=block*i;
            // negative tick
            writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " +
                    "x1=\""+(midline-offset)+"\" y1=\""+(Y+5)+"\" x2=\""+(midline-offset)+"\" y2=\""+(Y-5)+"\"/>\n");
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">-%d</text>\n",
                    (midline-offset),
                    Y + 15,
                    i));
            // positive tick
            writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " +
                    "x1=\""+(midline+offset)+"\" y1=\""+(Y+5)+"\" x2=\""+(midline+offset)+"\" y2=\""+(Y-5)+"\"/>\n");
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%d</text>\n",
                    (midline+offset),
                    Y + 15,
                    i));
        }
        // Now get tick at the zero point
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"1\" " +
                "x1=\""+(midline)+"\" y1=\""+(Y+5)+"\" x2=\""+(midline)+"\" y2=\""+(Y-5)+"\"/>\n");
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">0</text>\n",
                (midline),
                Y + 15));


        int rank=hpocase.getResult(diseaseCURIE).getRank();
        double ptp = hpocase.getResult(diseaseCURIE).getPosttestProbability();
        String diseaseLabel=String.format("%s [%s]: Rank: #%d Posttest probability: %.1f%%",diseaseName,diseaseCURIE.getValue(),rank,(100.0*ptp));
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"16px\" font-weight=\"bold\">%s</text>\n",
                (midline - (maxTick-1)*block),
                Y + 35,
                diseaseLabel));

    }

    /** If the LR score is 1, then we draw a diamond around the middle axis. */
    private void writeDiamond(Writer writer,int X, int Y) throws IOException
    {
        int diamondsize=6;
        writer.write(String.format("<polygon " +
                        "points=\"%d,%d %d,%d %d,%d %d,%d\" style=\"fill:lime;stroke:%s;stroke-width:1\" />\n",
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


    /**
     * Writes the set of boxes representing the log10 amplitudes of the likelihood ratios for individual
     * features.
     * @param writer File handle
     * @throws IOException if there is an issue writing the SVG code
     */
    private void writeLrBoxes(Writer writer) throws IOException {
        int currentY= MIN_VERTICAL_OFFSET + BOX_OFFSET*2;
        int midline=WIDTH/2;
        List<TermId> termIdList=hpocase.getObservedAbnormalities();
        List<TermId> excludedTermIdList=hpocase.getExcludedAbnormalities();
        Map<TermId, Double> unsortedmap = new HashMap<>();
        Map<TermId, Double> unsortedexcludedmap = new HashMap<>();
        double max=0;
        for (int i=0;i<termIdList.size();i++) {
            TermId tid = termIdList.get(i);
            double ratio = result.getObservedPhenotypeRatio(i);
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
            double lgratio = Math.log10(ratio);
            unsortedmap.put(tid,lgratio);
        }
        for (int i=0;i<excludedTermIdList.size();i++) {
            TermId tid = excludedTermIdList.get(i);
            double ratio = result.getExcludedPhenotypeRatio(i);
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
            double lgratio = Math.log10(ratio);
            unsortedexcludedmap.put(tid,lgratio);
        }
        // also check if the genotype LR is better than any of the phenotype LR's, so that max will be
        // correctly calculated!
        if (result.hasGenotype()) {
            double ratio = result.getGenotypeLR();
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
        }
        // maximum amplitude of the bars
        // we want it to be at least 10_000
        double maxAmp = Math.max(4.0,Math.log10(max));
        // we want the maximum amplitude to take up 80% of the space
        // the available space starting from the center line is WIDTH/2
        // and so we calculate a factor
        double scaling = (0.4*WIDTH)/maxAmp;
        Map<TermId, Double> sortedmap = sortByValue(unsortedmap);
        for (Map.Entry<TermId,Double> entry : sortedmap.entrySet()) {
            TermId tid = entry.getKey();
            double ratio = entry.getValue();
            double boxwidth=ratio*scaling;
            double xstart = midline;
            if (ratio<0) {
                boxwidth=Math.abs(boxwidth);
                xstart = 1+ midline - boxwidth;
            }
            if ((int)boxwidth==0) {
                int X=(int)xstart;
                writeDiamond(writer,X,currentY);
            } else {
                // red for features that do not support the diagnosis, green for those that do
                String color = xstart<midline ? RED : BLUE;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\"/>\n",
                        BOX_HEIGHT,
                        (int) boxwidth,
                        currentY,
                        (int) xstart,
                        color));
            }
            // add label of corresponding HPO term
            Term term = ontology.getTermMap().get(tid);
            String label = String.format("%s [%s]",term.getName(),tid.getValue());
            //writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%s</text>\n",
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n",
                        WIDTH,
                        currentY + BOX_HEIGHT,
                        label));
            currentY += BOX_HEIGHT+BOX_OFFSET;
        }
        // Now add negated terms if any
        Map<TermId, Double> sortedexcludedmap = sortByValue(unsortedexcludedmap);
        for (Map.Entry<TermId,Double> entry : sortedexcludedmap.entrySet()) {
            TermId tid = entry.getKey();
            double ratio = entry.getValue();
            double boxwidth=ratio*scaling;
            double xstart = midline;
            if (ratio<0) {
                boxwidth=Math.abs(boxwidth);
                xstart = 1+ midline - boxwidth;
            }
            if ((int)boxwidth==0) {
                int X=(int)xstart;
                writeDiamond(writer,X,currentY);
            } else {
                // red for features that do not support the diagnosis, green for those that do
                String color = xstart<midline ? RED : BLUE;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\"/>\n",
                        BOX_HEIGHT,
                        (int) boxwidth,
                        currentY,
                        (int) xstart,
                        color));
            }
            // add label of corresponding HPO term
            Term term = ontology.getTermMap().get(tid);
            String label = String.format("Excluded: %s [%s]",term.getName(),tid.getValue());
            //writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%s</text>\n",
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"normal\">%s</text>\n",
                    WIDTH,
                    currentY + BOX_HEIGHT,
                    label));
            currentY += BOX_HEIGHT+BOX_OFFSET;
        }



        if (result.hasGenotype()) {
            currentY += 0.5*(BOX_HEIGHT+BOX_OFFSET);

            double ratio = result.getGenotypeLR();
            double lgratio = Math.log10(ratio);
            String color = lgratio<0? RED : BLUE;
            double boxwidth=lgratio*scaling;
            double xstart = midline;
            if (lgratio<0) {
                boxwidth=Math.abs(boxwidth);
                xstart = 1+ midline - boxwidth;
            }
            if ((int)boxwidth==0) {
                int X=(int)xstart;
                writeDiamond(writer,X,currentY);
            } else {
                // red for features that do not support the diagnosis, green for those that do
                color = xstart<midline ? RED : BLUE;
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                "stroke-width=\"1\" stroke=\"#000000\" fill=\"%s\"/>\n",
                        BOX_HEIGHT,
                        (int) boxwidth,
                        currentY,
                        (int) xstart,
                        color));
            }
            // add label of Genotype
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"14px\" font-style=\"italic\">%s</text>\n",
                    WIDTH,
                    currentY + BOX_HEIGHT,
                    geneSymbol));
        }
        maxAmp=Math.max(4.0,maxAmp); // show at least 10,000!
        writeScale(writer,maxAmp,scaling);
    }

    /** The height of the middle line is the number of boxes times (BOX_HEIGHT + BOX_OFFSET)
     * plus one additional BOX_OFFSET on top.
     * @return Where to place the line underneath the boxes.
     */
    private int calculateHeightOfMiddleLine() {
        Objects.requireNonNull(result);
        int n = result.getNumberOfTests();
        if (result.hasGenotype()) {
            n+=2; // add another unit for the genotype
        }
        return 2*BOX_OFFSET + n*(BOX_HEIGHT+BOX_OFFSET);
    }




    /**
     * Draw the central line around which the likelihood ratio 'bars' will be drawn.
     * @param writer file handle
     * @throws IOException if we cannot write the SVG file.
     */
    private void writeVerticalLine(Writer writer) throws IOException {
           int midline=WIDTH/2;
        int topY=MIN_VERTICAL_OFFSET;
        int bottomY=topY + calculateHeightOfMiddleLine();
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " +
                "x1=\""+midline+"\" y1=\""+topY+"\" x2=\""+midline+"\" y2=\""+bottomY+"\" id=\"svg_1\"/>\n");

    }




    private void writeHeader(Writer writer) throws IOException{
        int total_width=WIDTH+TEXTPART_WIDTH;
        writer.write("<svg width=\""+total_width+"\" height=\""+HEIGHT+"\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by LR2PG -->\n");
        writer.write("<g>\n");
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</g>\n</svg>\n");
    }


    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        // sort list according to value, i.e., the magnitude of the likelihood ratio.
        list.sort( (e1,e2) -> (e2.getValue()).compareTo(e1.getValue()) );
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


}
