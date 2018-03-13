package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.phenol.formats.hpo.HpoDiseaseWithMetadata;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.formats.hpo.ImmutableHpoTermId;
import org.monarchinitiative.phenol.formats.hpo.HpoTermId;
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
 * Created by ravanv on 1/9/18.
 */
public class HpoCaseTest3 {
    /**
     * Name of the disease we are simulating in this test, i.e., OMIM:108500.
     */
    private static String kniestDysplasia = "156550";
    private static HpoCase hpocase;


    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("phenotype.hpoa").getFile();
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology = parser.parse();
        HpoAnnotation2DiseaseParser annotationParser = new HpoAnnotation2DiseaseParser(annotationPath, ontology);
        Map<String, HpoDiseaseWithMetadata> diseaseMap = annotationParser.getDiseaseMap();
        BackgroundForegroundTermFrequency d2fmap = new BackgroundForegroundTermFrequency(ontology, diseaseMap);
        /* these are the phenpotypic abnormalties of our "case" */
        TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
        HpoTermId t1 = new ImmutableHpoTermId(new ImmutableTermId(HP_PREFIX, "0410009"));

        ImmutableList.Builder<HpoTermId> builder = new ImmutableList.Builder<>();
        builder.add(t1);

        ImmutableList lst = ImmutableList.of(new ImmutableHpoTermId( "HP:0002812"),
                new ImmutableHpoTermId("HP:0003521"),
                new ImmutableHpoTermId("HP:0000541"),
                new ImmutableHpoTermId("HP:0011800"),
                new ImmutableHpoTermId("HP:0003015"),
                new ImmutableHpoTermId("HP:0008271"));


        hpocase = new HpoCase(ontology, d2fmap, kniestDysplasia, lst);
    }

    @Test
    public void testNotNull() {
        assertNotNull(hpocase);
    }

    @Test
    public void testNumberOfAnnotations() {
        int expected = 6;
        assertEquals(expected, hpocase.getNumberOfAnnotations());
    }

    /**
     * Likelihood ratio is 1, but it has rank 4. I think those with the same LR should have one rank.
     */
    @Test
    public void testPipeline() throws Lr2pgException {
        hpocase.calculateLikelihoodRatios();
        int expected = 1;
        hpocase.outputResults();
        int actual = hpocase.getRank(kniestDysplasia);
        assertEquals(expected, actual);
    }

}


