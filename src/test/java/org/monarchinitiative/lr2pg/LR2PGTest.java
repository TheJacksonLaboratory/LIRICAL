package org.monarchinitiative.lr2pg;
import org.junit.*;
import org.monarchinitiative.lr2pg.io.CommandParser;

public class LR2PGTest {




    @Test
    public void testLR2PG() {
        ClassLoader classLoader = LR2PGTest.class.getClassLoader();
        String filename = classLoader.getResource("small_phenoannot.tab").getFile();
        String pathToHpObo=classLoader.getResource("hp.obo").getFile();

        String annotationPath="/home/robinp/data/hpo/phenotype_annotation.tab"; // path to phenotype_annotation.tab



        String args[] = {"-o",pathToHpObo, "-a",annotationPath, "-i", filename };
        LR2PG lr2pg = new LR2PG(args);
        lr2pg.getPatientHPOTermsFromFile(filename);
        lr2pg.calculateLikelihoodRatio();
    }
}
