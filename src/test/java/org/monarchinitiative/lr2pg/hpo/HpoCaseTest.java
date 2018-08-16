package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatio;
import org.monarchinitiative.lr2pg.likelihoodratio.PhenotypeLikelihoodRatioTest;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpOboParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test whether we can successfully create HpoCaseOld objects.
 */
public class HpoCaseTest {
    private static final Logger logger = LogManager.getLogger();
    /** Name of the disease we are simulating in this test, i.e., OMIM:108500. */
    private static String diseasename="108500";
    private static HpoCase hpocase;
    private static HpoOntology ontology;
    private static PhenotypeLikelihoodRatio backforeFreq;


    @BeforeClass
    public static void setup() throws IOException,PhenolException,NullPointerException {
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        /* parse ontology */
        HpOboParser parser = new HpOboParser(new File(hpoPath));
        ontology =parser.parse();
        HpoDiseaseAnnotationParser annotationParser=new HpoDiseaseAnnotationParser(annotationPath,ontology);
        Map<TermId,HpoDisease> diseaseMap=annotationParser.parse();
        backforeFreq=new PhenotypeLikelihoodRatio(ontology,diseaseMap);

        /* these are the phenotypic abnormalties of our "case" */
        TermPrefix HP_PREFIX=new TermPrefix("HP");
        TermId t1 = new TermId(HP_PREFIX,"0006855");
        TermId t2 = new TermId(HP_PREFIX,"0000651");
        TermId t3 = new TermId(HP_PREFIX,"0010545");
        TermId t4 = new TermId(HP_PREFIX,"0001260");
        TermId t5 = new TermId(HP_PREFIX,"0001332");
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2,t3,t4,t5);
        // We need to provide a list of TestResult objects for the API, but they are not required for this unit test
        // therefore, pass an empty list.
        ImmutableMap<TermId,TestResult> results = ImmutableMap.of();
        HpoCase.Builder casebuilder = new HpoCase.Builder(builder.build()).
                results(results);

        hpocase = casebuilder.build();
    }


    @Test
    public void testNotNull() {
        assertNotNull(hpocase);
    }

    /**
     * Our test case has
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
        assertEquals(expected,hpocase.getNumberOfObservations());
    }



}
