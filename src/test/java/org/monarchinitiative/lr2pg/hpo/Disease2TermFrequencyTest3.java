package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by ravanv on 1/19/18.
 */

public class Disease2TermFrequencyTest3 {
    private static TermPrefix HP_PREFIX=null;

    private static Disease2TermFrequency d2tf=null;

    private static final double EPSILON=0.000001;
    private static String diseaseName = "100300";

// We test the frequency of HPO terms in a disease.

    @BeforeClass
    public static void setup() throws IOException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        ClassLoader classLoader = Disease2TermFrequencyTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology = parser.getPhenotypeSubontology();
        Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,phenotypeSubOntology,inheritanceSubontology);
        Map<String,HpoDiseaseWithMetadata> diseaseMap=annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY="0040280";
        d2tf=new Disease2TermFrequency(phenotypeSubOntology,inheritanceSubontology,diseaseMap);
    }

    @Test
    public void notNullTest() {
        assertNotNull(d2tf);
    }

    /**
     * $ cut -f 2 small_phenoannot.tab | sort | uniq | wc -l
     196
     */
    @Test
    public void testGetNumberOfDiseases() {
        int expected = 196;
        assertEquals(expected,d2tf.getNumberOfDiseases());
    }

    /**
     * The term HP:0000965 is a phenotype term in the disease 100300. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0000965");
        double expected =1.0;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

    /**
     * The term HP:0010957 is not a phenotype term in the disease 100300. The frequency of term in disease is almost zero, 0.000510.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_1() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0010957");
        double expected =0.000510;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }
    /**
     * The term HP:0004415 is  a phenotype term in the disease 100300. The frequency of term in disease is 1.
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_3() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0004415");
        double expected =1;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }
    /**
     * The term HP:0011304 is not a phenotype term in the disease 100300. The frequency of term in disease is 0.003061224489795918?
     */
    @Test
    public void testGetFrequencyOfTermInDieases1_4() {
        ImmutableTermId tid = new ImmutableTermId(HP_PREFIX,"0011304");
        double expected =0.003061224489795918;
        assertEquals(expected,d2tf.getFrequencyOfTermInDisease(diseaseName,tid),EPSILON);
    }

}
