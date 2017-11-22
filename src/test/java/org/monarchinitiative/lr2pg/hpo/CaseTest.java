package org.monarchinitiative.lr2pg.hpo;

import org.junit.BeforeClass;

import java.io.IOException;

public class CaseTest {

    private static Case hpocase;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        String caseFile = classLoader.getResource("HPOTerms").getFile();
        hpocase = new Case(hpoPath,annotationPath,caseFile);

    }
}
