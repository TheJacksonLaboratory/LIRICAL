package org.monarchinitiative.lr2pg.hpo;


import com.google.common.collect.ImmutableList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.formats.hpo.HpoOntology;
import org.monarchinitiative.phenol.io.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.obo.hpo.HpoOboParser;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import java.io.File;
import java.io.IOException;
import java.util.Map;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ravanv on 1/19/18.
 */
//In this class, we test the frequency of terms in a disease and background frequencies and finally we calculate the likelihood ratio.
// The disease is:  ADIE PUPIL 103100 and there are 3 terms for this disease, where one of them is 0000006  Autosomal dominant inheritance. The two other terms
// HP:0001265 and HP:0012074 are phenotype terms.
public class LikelihoodRatioTestTest {
    private static TermPrefix HP_PREFIX = null;

    private static BackgroundForegroundTermFrequency d2tf = null;

    private static final double EPSILON = 0.000001;

    private static Map<String, HpoDisease> diseaseMap;




    @BeforeClass
    public static void setup() throws IOException,PhenolException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        ClassLoader classLoader = BackgroundForegroundTermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small.hpoa").getFile();
        HpoOboParser parser = new HpoOboParser(new File(hpoPath));
        HpoOntology ontology = parser.parse();

        HpoDiseaseAnnotationParser annotationParser = new HpoDiseaseAnnotationParser(annotationPath, ontology);
        diseaseMap = annotationParser.parse();
        String DEFAULT_FREQUENCY = "0040280";
        d2tf = new BackgroundForegroundTermFrequency(ontology, diseaseMap);
        TermPrefix HP_PREFIX = new ImmutableTermPrefix("HP");
        //ImmutableTermIdWithMetadata t1 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX, "0000006"));
//        TermId t2 = ImmutableTermId.constructWithPrefix( "HP:0001265");
//        TermId t3 = ImmutableTermId.constructWithPrefix("HP:0012074");
//        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
//        builder.add(t2,t3);
//        hpocase = new HpoCase(ontology,d2tf,diseaseName,builder.build());
//        disease = diseaseMap.get(diseaseName);
    }

    /**
     * The term HP:0001265 is a phenotype term in the disease 103100. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_1() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000185");
        String diseaseName = "OMIM:216300";
        HpoDisease disease = diseaseMap.get(diseaseName);
        assertNotNull(disease);
        double expected =1.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(disease,tid),EPSILON);
    }



}
