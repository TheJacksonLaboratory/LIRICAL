package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.*;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.File;
import java.io.IOException;
import java.util.Map;

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
        /* parse ontology */
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        Ontology<HpoTerm, HpoTermRelation>phenotypeSubOntology = parser.getPhenotypeSubontology();
        Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationPath,phenotypeSubOntology,inheritanceSubontology);
        Map<String,HpoDiseaseWithMetadata> diseaseMap=annotationParser.getDiseaseMap();
        Disease2TermFrequency d2fmap=new Disease2TermFrequency(hpoPath,annotationPath); //todo pass in the other objects


        String caseFile = classLoader.getResource("HPOTerms").getFile();

        String diseasename="OMIM:108500";
        TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");
        ImmutableTermIdWithMetadata t1 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0006855"));
        ImmutableTermIdWithMetadata t2 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0000651"));
        ImmutableTermIdWithMetadata t3 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0010545"));
        ImmutableTermIdWithMetadata t4 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001260"));
        ImmutableTermIdWithMetadata t5 = new ImmutableTermIdWithMetadata(new ImmutableTermId(HP_PREFIX,"0001332"));
        ImmutableList.Builder<TermIdWithMetadata> builder = new ImmutableList.Builder<>();
        builder.add(t1,t2,t3,t4,t5);


        hpocase = new HpoCase(phenotypeSubOntology,d2fmap,diseasename,builder.build());
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
