package org.monarchinitiative.lirical.svg;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lirical2Svg {

    /** minimum distance to top of image of graphic elements */
    protected final static int MIN_VERTICAL_OFFSET=10;

    /** distance between two adjacent "boxes" */
    protected final static int BOX_OFFSET=10;

    protected final static int BOX_HEIGHT=15;

    protected final static String BLUE ="#4dbbd5";
    protected final static String RED ="#e64b35";
    protected final static String BROWN="#7e6148";
    protected final static String DARKBLUE = "#3c5488";
    protected final static String VIOLET = "#8491b4";
    protected final static String ORANGE = "#ff9900";
    protected final static String BLACK = "#000000";
    protected final static String GREEN = "#00A087";
    protected final static String BRIGHT_GREEN = "#19a000";

    protected void writeFooter(Writer writer) throws IOException {
        writer.write("</g>\n</svg>\n");
    }

    /**
     * This method shortens items such as #101200 APERT SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE I; ACS1;;ACS IAPERT-CROUZON DISEASE, INCLUDED;;
     * to simply APERT SYNDROME
     * @param originalDiseaseName original String from HPO database, derived from OMIM and potentially historic
     * @return simplified and prettified name
     */
    protected String prettifyDiseaseName(String originalDiseaseName) {
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

    /**
     * We use a diamond symbol to show a value that would be too small to appear as a visible box.
     * We do this bothfor the likelihood ratio as well as for the post-test probability
     */
    protected void writeDiamond(Writer writer,int X, int Y) throws IOException
    {
        int diamondsize=6;
        writer.write(String.format("<polygon " +
                        "points=\"%d,%d %d,%d %d,%d %d,%d\" style=\"fill:grey;stroke:%s;stroke-width:1\" />\n",
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



    protected static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
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
