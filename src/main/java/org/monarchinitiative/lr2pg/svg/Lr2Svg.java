package org.monarchinitiative.lr2pg.svg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.monarchinitiative.lr2pg.hpo.HpoCase;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * This class creates an SVG file representing the results of likelihood ratio analysis of an HPO case.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Lr2Svg {
    private static final Logger logger = LogManager.getLogger();
    /** An object representing the Human Phenotype Ontology */
    private final HpoOntology ontology;
    private final HpoCase hpocase;
    private final TestResult result;
    /** Height of entire image in px */
    private final static int HEIGHT=480;
    /** width of the bars part of the image in px */
    private final static int WIDTH=500;
    /** additional width of the text part of the image in px */
    private final static int TEXTPART_WIDTH=400;
    /** minimum distance to top of image of graphic elements */
    private final static int MIN_VERTICAL_OFFSET=10;
    /** distance between two adjacent "boxes" */
    private final static int BOX_OFFSET=10;

    private final static int BOX_HEIGHT=15;


    private final int heightOfMiddleLine;

    public Lr2Svg(HpoCase hcase,  TestResult result,HpoOntology ont) {
        this.hpocase=hcase;
        this.result=result;
        this.ontology=ont;
        this.heightOfMiddleLine=calculateHeightOfMiddleLine();
    }




    public void writeSvg(String path) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writeHeader(writer);
            writeMiddleLine(writer);
            writeLrBoxes(writer);
            writeFooter(writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Unable to write SVG file: "+path);
            e.printStackTrace();
        }
    }

    private void writeScale(Writer writer, double maxAmp, double scaling) throws IOException {
        int Y = heightOfMiddleLine +  MIN_VERTICAL_OFFSET + BOX_OFFSET*4;
        int maxTick = (int) Math.ceil(maxAmp);
        int maxX = (int)(maxTick * scaling);
        int midline=WIDTH/2;
        writer.write("<line fill=\"none\" stroke=\"midnightblue\" stroke-width=\"2\" " +
                "x1=\""+(midline-maxX)+"\" y1=\""+Y+"\" x2=\""+(midline+maxX)+"\" y2=\""+Y+"\"/>\n");
        int block = maxX/maxTick;
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
        writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%s</text>\n",
                (midline - (maxTick-1)*block),
                Y + 35,
                result.getDiseaseCurie()));

    }

    private void writeLrBoxes(Writer writer) throws IOException {
        int currentY= MIN_VERTICAL_OFFSET + BOX_OFFSET*2;
        int midline=WIDTH/2;
        List<TermId> termIdList=hpocase.getObservedAbnormalities();
        Map<TermId, Double> unsortedmap = new HashMap<>();
        double max=0;
        for (int i=0;i<termIdList.size();i++) {
            TermId tid = termIdList.get(i);
            double ratio = result.getRatio(i);
            if (max < Math.abs(ratio)) { max = Math.abs(ratio); }
            double lgratio = Math.log10(ratio);
            unsortedmap.put(tid,lgratio);
        }
        // maximum amplitude of the bars
        double maxAmp = Math.log10(max);
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
                int Y=currentY;
                int diamondsize=6;
                writer.write(String.format("<polygon " +
                                "points=\"%d,%d %d,%d %d,%d %d,%d\" style=\"fill:lime;stroke:purple;stroke-width:1\" />\n",
                        X,
                        Y,
                        X+diamondsize,
                        Y+diamondsize,
                        X,
                        Y+2*diamondsize,
                        X-diamondsize,
                        Y+diamondsize));
            } else {
                writer.write(String.format("<rect height=\"%d\" width=\"%d\" y=\"%d\" x=\"%d\" " +
                                "stroke-width=\"1\" stroke=\"#000000\" fill=\"#FF0000\"/>\n",
                        BOX_HEIGHT,
                        (int) boxwidth,
                        currentY,
                        (int) xstart));
            }
            // add label of corresponding HPO term
            Term term = ontology.getTermMap().get(tid);
            String label = String.format("%s [%s]",term.getName(),tid.getIdWithPrefix());
            writer.write(String.format("<text x=\"%d\" y=\"%d\" font-size=\"12px\" style=\"stroke: black; fill: black\">%s</text>\n",
                        WIDTH,
                        currentY + BOX_HEIGHT,
                        label));
            currentY += BOX_HEIGHT+BOX_OFFSET;
        }
        writeScale(writer,maxAmp,scaling);
    }

    /** The height of the middle line is the number of boxes times (BOX_HEIGHT + BOX_OFFSET)
     * plus one additional BOX_OFFSET on top.
     * @return Where to place the line underneath the boxes.
     */
    private int calculateHeightOfMiddleLine() {
        int n = result.getNumberOfTests();
        return 2*BOX_OFFSET + n*(BOX_HEIGHT+BOX_OFFSET);
    }




    /**
     * Draw the central line around which the likelihood ratio 'bars' will be drawn.
     * @param writer file handle
     * @throws IOException if we cannot write the SVG file.
     */
    private void writeMiddleLine(Writer writer) throws IOException {
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
        writer.write("<!-- Created by Exomiser - https://monarchinitiative.org -->\n");
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
