package org.monarchinitiative.lr2pg.genotype;

import org.junit.BeforeClass;
import org.monarchinitiative.lr2pg.io.GenotypeDataIngestor;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class Genotype2LRTest {

    private static Map<TermId,Double> gene2backgroundFrequency;


    @BeforeClass
    public static void setup() {
        ClassLoader classLoader = Genotype2LRTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String backgroundFreqPath="/home/robinp/IdeaProjects/LR2PG/background-freq.txt";
        GenotypeDataIngestor ingestor = new GenotypeDataIngestor(backgroundFreqPath);
//        gene2backgroundFrequency=ingestor.parse();
    }

//
//    @Test
//    public void testit(){
//        assertNotNull(gene2backgroundFrequency);
//    }


}
