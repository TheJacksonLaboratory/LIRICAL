package org.monarchinitiative.lr2pg.hpo;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.phenol.formats.hpo.*;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;


import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ravanv on 1/8/18.
 */
public class HpoCaseTest2 {

    private static final Logger logger = LogManager.getLogger();
    /** Name of the disease we are simulating in this test, i.e., OMIM:108500. */
    private static String diseasename="2466";
    private static HpoCase hpocase;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("phenotype.hpoa").getFile();
        /* parse ontology */
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology =parser.parse();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,ontology);
        Map<String,HpoDisease> diseaseMap=annotationParser.getDiseaseMap();
        BackgroundForegroundTermFrequency d2fmap=new BackgroundForegroundTermFrequency(ontology,diseaseMap);
        //String caseFile = classLoader.getResource("HPOTerms").getFile();

        /* these are the phenpotypic abnormalties of our "case" */
        TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");
        HpoTermId t1 = new ImmutableHpoTermId(new ImmutableTermId(HP_PREFIX,"0000750"));
        HpoTermId t2 = new ImmutableHpoTermId(new ImmutableTermId(HP_PREFIX,"0001258"));
         //ImmutableTermIdWithMetadata t3 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001188"));
        //ImmutableTermIdWithMetadata t4 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001274"));


        ImmutableList.Builder<HpoTermId> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2);
        hpocase = new HpoCase(ontology,d2fmap,diseasename,builder.build());
    }

    @Test
    public void testNotNull() {
        assertNotNull(hpocase);
    }
    @Test
    public void testNumberOfAnnotations() {
        int expected=2;
        assertEquals(expected,hpocase.getNumberOfAnnotations());
    }


    @Test
    public void testPipeline() throws Lr2pgException {
        hpocase.calculateLikelihoodRatios();
        int expected=4;
        hpocase.outputResults();
        int actual=hpocase.getRank(diseasename);
        assertEquals(expected ,actual);
    }


}
