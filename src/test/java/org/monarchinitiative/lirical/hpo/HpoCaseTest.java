package org.monarchinitiative.lirical.hpo;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;



import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.likelihoodratio.PhenotypeLikelihoodRatioTest;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;


import java.io.File;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test whether we can successfully create HpoCaseOld objects.
 */
class HpoCaseTest {
    /** Name of the disease we are simulating in this test, i.e., OMIM:108500. */
    private static String diseasename="108500";
    private static HpoCase hpocase;


    @BeforeAll
    static void setup() throws NullPointerException {
        ClassLoader classLoader = PhenotypeLikelihoodRatioTest.class.getClassLoader();
        String hpoPath = Objects.requireNonNull(Objects.requireNonNull(classLoader.getResource("hp.small.obo")).getFile());
        String annotationPath = Objects.requireNonNull(Objects.requireNonNull(classLoader.getResource("small.hpoa")).getFile());
        /* parse ontology */
        // The HPO is in the default  curie map and only contains known relationships / HP terms
        Ontology ontology = OntologyLoader.loadOntology(new File(hpoPath));
        Map<TermId, HpoDisease> diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(annotationPath, ontology);

        /* these are the phenotypic abnormalties of our "case" */
        String HP_PREFIX = "HP";
        TermId t1 = TermId.of(HP_PREFIX, "0000028");
        TermId t2 = TermId.of(HP_PREFIX, "0000047");
        TermId t3 = TermId.of(HP_PREFIX, "0000185");
        TermId t4 = TermId.of(HP_PREFIX, "0000632");
        TermId t5 = TermId.of(HP_PREFIX, "0000528");
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
    void testNotNull() {
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
    void testNumberOfAnnotations() {
        int expected=5;
        assertEquals(expected,hpocase.getNumberOfObservations());
    }

    @Test
    void testGetObservedAbnormalities() {
        assertEquals(5, hpocase.getObservedAbnormalities().size());
    }

    @Test
    void testGetExcludedAbnormalities() {
        assertEquals(0,  hpocase.getExcludedAbnormalities().size());
    }

    @Test
    void testAge() {
        // we did not specify the age, so it should return not known
        assertEquals(Age.ageNotKnown(),hpocase.getAge());
    }

    @Test
    void testSex() {
        // we did not specify sex, so it should return unknown
        assertEquals(Sex.UNKNOWN, hpocase.getSex());
    }



}
