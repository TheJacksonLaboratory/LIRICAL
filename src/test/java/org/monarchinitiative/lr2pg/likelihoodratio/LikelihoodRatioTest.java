package org.monarchinitiative.lr2pg.likelihoodratio;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.BeforeClass;
import org.junit.Test;

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
