package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoDiseaseAnnotation;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.prototype.Disease;

import java.util.List;
import java.util.Map;

public class HPOParserTest {

    private static String hpoPath="hp.obo";
    private static Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;
    /** 2000 randomly chosen HPO Annotations */
    private static List<HpoDiseaseAnnotation> annotations=null;
    private static HPOParser parser=null;
    private static Map<String,Disease> diseaseMap=null;

    @BeforeClass
    public static void setup() {
        ClassLoader classLoader = HPOParserTest.class.getClassLoader();
        String hpoPath=classLoader.getResource("hp.obo").getFile();
        parser = new HPOParser();
        hpoOntology = parser.parseOntology(hpoPath);
        String annotationPath=classLoader.getResource("small_phenoannot.tab").getFile();
        parser.parseAnnotation(annotationPath);
        annotations =parser.getAnnotList();
        parser.initializeTermMap();
        diseaseMap = parser.createDiseaseModels();
    }


    @Test
    public void testGetHpoOntology(){
        Assert.assertTrue(hpoOntology != null);
        int expectedNumberOfAnnotationLines=2000;
        Assert.assertEquals(expectedNumberOfAnnotationLines,annotations.size());
    }


    @Test
    public void testCreateDiseaseMap() {
        int expectedNumberOfDiseases=196;
        Assert.assertEquals(expectedNumberOfDiseases, diseaseMap.size());
    }

    @Test
    public void testAarskogSyndrome () {
        String aarskogKey="100050";
        Disease aarskog = diseaseMap.get(aarskogKey);
        Assert.assertNotNull(aarskog);
        int expectedTotalHpoForAarskog=40; // non-inheritance terms only!
        Assert.assertEquals(expectedTotalHpoForAarskog,aarskog.getHpoIds().size());
        aarskog.

    }


}
