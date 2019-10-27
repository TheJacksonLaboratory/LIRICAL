package org.monarchinitiative.lirical.io;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class coordinates the input of the background frequency file. Note that this file is added as a resource to the
 * JAR file, i.e., {@code LIRICAL.jar!/background/background-hg19.tsv} (or -hg38.tsv), and so it cannot be opened using
 * a path. The user is allowed to provide their own background file, in which case a path is used. There are two
 * factory methods, one for the path and one for the name of a resource (both are strings).
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class GenotypeDataIngestor {
    private static final Logger logger = LoggerFactory.getLogger(GenotypeDataIngestor.class);
    /** Key: the TermId of a gene. Value. Its background frequency in the current genome build. This variable
     * is only initialized for runs with a VCF file. */
    private Map<TermId, Double> gene2freq;

    private final static String ENTREZ_GENE_PREFIX="NCBIGene";

    private GenotypeDataIngestor() {
    }

    private Map<TermId, Double> getGene2backgroundFrequency(){
        return gene2freq;
    }

    public static Map<TermId, Double> fromPath(String backgroundFrequencyPath) {
        GenotypeDataIngestor gdi = new GenotypeDataIngestor();
        try {
            BufferedReader br = new BufferedReader(new FileReader(backgroundFrequencyPath));
            gdi.parse(br);
        } catch (IOException e) {
            logger.error("Could not read background frequency file from {}",backgroundFrequencyPath);
            throw new RuntimeException("Could not read background frequency file from " + backgroundFrequencyPath);
        }
        return gdi.getGene2backgroundFrequency();
    }
    public static Map<TermId, Double> fromResource(String resourceString) {
        ClassLoader classLoader = GenotypeDataIngestor.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(resourceString);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        GenotypeDataIngestor gdi = new GenotypeDataIngestor();
        gdi.parse(br);
        return gdi.getGene2backgroundFrequency();
    }

    /**
     * symbol	geneID	freqsum-benign	count-benign	freqsum-path	count-path
     */
    private void parse(BufferedReader reader)  {
        //ImmutableMap.Builder<TermId,Double> builder = new ImmutableMap.Builder<>();
        this.gene2freq=new HashMap<>();
        try {

            String line = reader.readLine(); // this is the header -- discardR
            while ((line=reader.readLine())!=null) {
                String[] a = line.split("\t");
                if (a.length <10) {
                    System.err.println(String.format("[ERROR GenotypeDataIngestor] malformed line with %d instead of 10 fields: %s",
                            a.length, line));
                }
                //String symbol=a[0]; not needed.
                String entrezNumber=a[1]; // e.g., 2200 for FBN1
                if (entrezNumber==null || entrezNumber.length()==0) {
                    continue; // no EntrezId available -- this happens with many genes
                }
                TermId entrezId=TermId.of(ENTREZ_GENE_PREFIX,entrezNumber);
                String fsumpath=a[9];
                try {
                    Double pathSum = Double.parseDouble(fsumpath);
                    gene2freq.put(entrezId,pathSum);
                } catch (NumberFormatException e) {
                    e.printStackTrace();// should really never happen--TODO throw Exception
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
