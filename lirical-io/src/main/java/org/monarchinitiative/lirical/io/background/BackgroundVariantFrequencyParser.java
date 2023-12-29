package org.monarchinitiative.lirical.io.background;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class coordinates the input of the background frequency file. Note that this file is added as a resource to the
 * JAR file, i.e., {@code /background/background-hg19.tsv}.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class BackgroundVariantFrequencyParser {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundVariantFrequencyParser.class);

    private final static String ENTREZ_GENE_PREFIX="NCBIGene";

    private BackgroundVariantFrequencyParser() {
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
                if (entrezNumber==null || entrezNumber.isEmpty()) {
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
