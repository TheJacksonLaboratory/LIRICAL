package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.phenol.formats.hpo.HpoDiseaseWithMetadata;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.formats.hpo.ImmutableTermIdWithMetadata;
import org.monarchinitiative.phenol.formats.hpo.TermIdWithMetadata;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HpoCaseTest {
    private static final Logger logger = LogManager.getLogger();
    /** Name of the disease we are simulating in this test, i.e., OMIM:108500. */
    private static String diseasename="108500";
    private static HpoCase hpocase;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        /* parse ontology */
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology =parser.parse();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,ontology);
        Map<String,HpoDiseaseWithMetadata> diseaseMap=annotationParser.getDiseaseMap();
        Disease2TermFrequency d2fmap=new Disease2TermFrequency(ontology,diseaseMap);
        //String caseFile = classLoader.getResource("HPOTerms").getFile();

        /* these are the phenotypic abnormalties of our "case" */
        TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");
        ImmutableTermIdWithMetadata t1 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0006855"));
        ImmutableTermIdWithMetadata t2 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0000651"));
        ImmutableTermIdWithMetadata t3 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0010545"));
        ImmutableTermIdWithMetadata t4 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001260"));
        ImmutableTermIdWithMetadata t5 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001332"));
        ImmutableList.Builder<TermIdWithMetadata> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2,t3,t4,t5);

        hpocase = new HpoCase(ontology,d2fmap,diseasename,builder.build());
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
        assertEquals(expected,hpocase.getNumberOfAnnotations());
    }
//
//    @Test
//    public void testPipeline() throws Lr2pgException {
//        hpocase.calculateLikelihoodRatios();
//        int expected=1;
//       hpocase.outputResults();
//        int actual=hpocase.getRank(diseasename);
//        assertEquals(expected ,actual);
//    }
}
