package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.*;
import com.github.phenomics.ontolib.ontology.data.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HpoAnnotation2DiseaseParser;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class TestBackgroundFrequency {
    /** THe file name of the HPO ontology file. */
    private static final String HP_OBO="hp.obo";
    /** The file name of the HPO annotation file. */
    private static final String HP_PHENOTYPE_ANNOTATION="phenotype_annotation.tab";

    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;



    private Disease2TermFrequency d2termFreqMap;

    private static Map<String,HpoDiseaseWithMetadata> diseaseMap;

    private static TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");

    private static HpoFrequency defaultFrequency=null;

    private static final int DEFAULT_N_TERMS_PER_CASE=4;

    private int n_terms_per_case = DEFAULT_N_TERMS_PER_CASE;

    private static final int DEFAULT_N_RANDOM_TERMS=2;

    private int n_n_random_terms_per_case=DEFAULT_N_RANDOM_TERMS;

    private static final HpoFrequency[] FREQUENCYARRAY={
            HpoFrequency.ALWAYS_PRESENT,
            HpoFrequency.VERY_FREQUENT,
            HpoFrequency.FREQUENT,
            HpoFrequency.OCCASIONAL,
            HpoFrequency.VERY_RARE,
            HpoFrequency.EXCLUDED};

    private static final HpoOnset[] ONSETARRAY={
            HpoOnset.ANTENATAL_ONSET,
            HpoOnset.EMBRYONAL_ONSET,
            HpoOnset.FETAL_ONSET,
            HpoOnset.CONGENITAL_ONSET,
            HpoOnset.NEONATAL_ONSET,
            HpoOnset.INFANTILE_ONSET,
            HpoOnset.CHILDHOOD_ONSET,
            HpoOnset.JUVENILE_ONSET,
            HpoOnset.ADULT_ONSET,
            HpoOnset.YOUNG_ADULT_ONSET,
            HpoOnset.MIDDLE_AGE_ONSET,
            HpoOnset.LATE_ONSET};


    @Before
    public void init() throws IOException {
        ClassLoader classLoader = TestBackgroundFrequency.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        String annotationpath=classLoader.getResource("phenotype_annotation.tab").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        phenotypeSubOntology = parser.getPhenotypeSubontology();
        inheritanceSubontology = parser.getInheritanceSubontology();
        HpoAnnotation2DiseaseParser annotationParser=new HpoAnnotation2DiseaseParser(annotationpath,phenotypeSubOntology,inheritanceSubontology);
        diseaseMap=annotationParser.getDiseaseMap();
        String DEFAULT_FREQUENCY="0040280";
        final TermId DEFAULT_FREQUENCY_ID = new ImmutableTermId(HP_PREFIX,DEFAULT_FREQUENCY);
        defaultFrequency=HpoFrequency.fromTermId(DEFAULT_FREQUENCY_ID);
        this.d2termFreqMap=new Disease2TermFrequency(phenotypeSubOntology,inheritanceSubontology,diseaseMap);
        //this.d2termFreqMap = new Disease2TermFrequency(hpopath,annotationpath); //todo pass in the other objects
    }



    /**
     * Set up test case for disease OMIM:613172, Cardiomyopathy, dilated, 1DD
     * The term HP:0031301 (Peripheral arterial calcification). No diseases in our
     * corpus are annotated to this term.
     */
    @Test
    public void testTermNotInCorpusBackgroundFreqeuncy() throws Exception {
        HpoDiseaseWithMetadata disease = diseaseMap.get("613172");
        assertNotNull(disease);
        int expected_n_annotations=3;
        assertEquals(expected_n_annotations,disease.getPhenotypicAbnormalities().size());
        TermId autosomalDominant = new ImmutableTermId(HP_PREFIX,"0000006");
        assertEquals(autosomalDominant,disease.getModesOfInheritance().get(0));

        TermId queryTerm = new ImmutableTermId(HP_PREFIX,"0031301");

        double backgroundFrequency = 42;//d2termFreqMap.getBackgroundFrequency(queryTerm);
        assertTrue(backgroundFrequency>0);

    }



}
