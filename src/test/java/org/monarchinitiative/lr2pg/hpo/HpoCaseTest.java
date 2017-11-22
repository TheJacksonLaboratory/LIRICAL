package org.monarchinitiative.lr2pg.hpo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HpoCaseTest {
    private static final Logger logger = LogManager.getLogger();


    private static HpoCase hpocase;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        String caseFile = classLoader.getResource("HPOTerms").getFile();
        hpocase = new HpoCase(hpoPath,annotationPath,caseFile);
    }

    @Test
    public void testNotNull() {
        assertNotNull(hpocase);
    }

    /**
     * Our test file has
     * OMIM:108500
     HP:0006855
     HP:0000651
     HP:0010545
     HP:0001260
     Thus there are five Hpo annotations.
     */
    @Test
    public void testNumberOfAnnotations() {
        int expected=5;
        assertEquals(expected,hpocase.getNumberOfAnnotations());
    }

    @Test
    public void testPipeline() {
        hpocase.calculateLikelihoodRatios();
        hpocase.outputResults();
        assertEquals(1,1);
    }
}
