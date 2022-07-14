package org.monarchinitiative.lirical.io;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
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

    private final static String ENTREZ_GENE_PREFIX="NCBIGene";

    private GenotypeDataIngestor() {
    }

    /**
     * symbol	geneID	freqsum-benign	count-benign	freqsum-path	count-path
     */
    public static Map<TermId, Double> parse(BufferedReader reader)  {
        // Key: the TermId of a gene.
        // Value. Its background frequency in the current genome build. This variable is only initialized for runs with a VCF file.
        Map<TermId, Double> geneFrequency = new HashMap<>();
        try {
            reader.readLine(); // this is the header -- discard
            String line;
            while ((line=reader.readLine())!=null) {
                String[] a = line.split("\t");
                if (a.length <10) {
                    logger.warn("malformed line with {} instead of 10 fields: {}", a.length, line);
                    continue;
                }
                String entrezNumber=a[1]; // e.g., 2200 for FBN1
                if (entrezNumber==null || entrezNumber.length()==0) {
                    continue; // no EntrezId available -- this happens with many genes
                }
                TermId entrezId=TermId.of(ENTREZ_GENE_PREFIX,entrezNumber);
                String fsumpath=a[9];
                try {
                    Double pathSum = Double.parseDouble(fsumpath);
                    geneFrequency.put(entrezId,pathSum);
                } catch (NumberFormatException e) {
                    logger.warn("Skipping line with non-parseable value {}: {}", e.getMessage(), line);
                }
            }
            return Collections.unmodifiableMap(geneFrequency);
        } catch (IOException e) {
            logger.warn("Error {}", e.getMessage(), e);
            return Map.of();
        }

    }


}
