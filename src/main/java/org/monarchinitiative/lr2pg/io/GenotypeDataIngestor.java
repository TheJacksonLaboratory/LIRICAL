package org.monarchinitiative.lr2pg.io;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GenotypeDataIngestor {


    private final String backgroundFrequencyPath;

    private final static TermPrefix ENTREZ_GENE_PREFIX=new TermPrefix("NCBIGene");

    public GenotypeDataIngestor(String path) {
        this.backgroundFrequencyPath=path;
    }

    /**
     * symbol	geneID	freqsum-benign	count-benign	freqsum-path	count-path

     * @return a map with key: EntrezGene id, value: sum of frequencies
     */
    public Map<TermId,Double> parse() {
        //ImmutableMap.Builder<TermId,Double> builder = new ImmutableMap.Builder<>();
        Map<TermId, Double> gene2freq=new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(this.backgroundFrequencyPath));
            String line = br.readLine(); // this is the header -- discard
            // TODO put # on header line from G2GIT
            while ((line=br.readLine())!=null) {
                String a[] = line.split("\t");
                if (a.length != 6) {
                    System.err.println("[ERROR] malformed line: " + line);
                }
                String symbol=a[0];
                String entrezNumber=a[1]; // e.g., 2200 for FBN1
                if (entrezNumber==null || entrezNumber.length()==0) {
                    continue; // no EntrezId available -- this happens with many genes
                }
                TermId entrezId=new TermId(ENTREZ_GENE_PREFIX,entrezNumber);
                String fsumpath=a[9];
                try {
                    Double pathSum = Double.parseDouble(a[9]);
                    //builder.put(entrezId,pathSum);
                    gene2freq.put(entrezId,pathSum);
                } catch (NumberFormatException e) {
                    e.printStackTrace();// should really never happen--TODO throw Exception
                }
                System.out.println(symbol + "\t" + entrezId.getIdWithPrefix() +"\t"+fsumpath);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gene2freq;
       // return builder.build();
    }


}
