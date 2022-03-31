package org.monarchinitiative.lirical.hpo;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.analysis.AnalysisResults;

import org.monarchinitiative.phenol.ontology.data.TermId;


import java.util.ArrayList;
import java.util.List;

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
        /* these are the phenotypic abnormalties of our "case" */
        String HP_PREFIX = "HP";
        List<TermId> builder = new ArrayList<>();
        builder.add(TermId.of(HP_PREFIX, "0000028"));
        builder.add(TermId.of(HP_PREFIX, "0000047"));
        builder.add(TermId.of(HP_PREFIX, "0000185"));
        builder.add(TermId.of(HP_PREFIX, "0000632"));
        builder.add(TermId.of(HP_PREFIX, "0000528"));
        // We need to provide a list of TestResult objects for the API, but they are not required for this unit test
        // therefore, pass an empty list.
        HpoCase.Builder casebuilder = new HpoCase.Builder("SampleId", builder).
                results(AnalysisResults.empty());

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
