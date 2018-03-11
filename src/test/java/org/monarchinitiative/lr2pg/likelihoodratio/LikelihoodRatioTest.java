package org.monarchinitiative.lr2pg.likelihoodratio;


import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermId;
import org.monarchinitiative.phenol.ontology.data.ImmutableTermPrefix;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermPrefix;

import static org.junit.Assert.assertEquals;

public class LikelihoodRatioTest {

    public static final TermPrefix HP_PREFIX=new ImmutableTermPrefix("HP");

    @BeforeClass
    public static void init() {


    }


    /**
     * Test that equivalent TermId objects are recognized as equal
     */
    @Test
    public void testTermIdEquality() {
        TermId t1 = new ImmutableTermId(HP_PREFIX,"0123456");
        TermId t2 = new ImmutableTermId(HP_PREFIX, "0123456");
        assertEquals(t1,t2);

    }


}
