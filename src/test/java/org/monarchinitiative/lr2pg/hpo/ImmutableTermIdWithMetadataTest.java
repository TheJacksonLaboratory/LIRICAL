package org.monarchinitiative.lr2pg.hpo;

import com.github.phenomics.ontolib.formats.hpo.HpoFrequency;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.BeforeClass;
import org.junit.Test;
import org.monarchinitiative.lr2pg.io.HPOOntologyParserTest;
import org.monarchinitiative.lr2pg.io.HpoOntologyParser;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 * @version 0.0.2 (2017-11-24)
 */
public class ImmutableTermIdWithMetadataTest {
    private static TermPrefix HP_PREFIX=null;
    /** If no frequency is provided, the parser uses the default (100%) */
    private static HpoFrequency defaultFrequency=null;

    @BeforeClass
    public static void setup() throws IOException {
        HP_PREFIX = new ImmutableTermPrefix("HP");
        String DEFAULT_FREQUENCY="0040280";
        final TermId DEFAULT_FREQUENCY_ID = new ImmutableTermId(HP_PREFIX,DEFAULT_FREQUENCY);
        defaultFrequency=HpoFrequency.fromTermId(DEFAULT_FREQUENCY_ID);
    }


    @Test
    public void testEqualityOfTerms1() {
        TermId oxycephalyId = new ImmutableTermId(HP_PREFIX,"0000263");
        TermIdWithMetadata oxycephaly = new ImmutableTermIdWithMetadata( oxycephalyId,defaultFrequency,null);
        assertEquals(oxycephaly,oxycephaly);
    }

    @Test
    public void testEqualityOfTerms2() {
        TermId oxycephalyId = new ImmutableTermId(HP_PREFIX,"0000263");
        TermIdWithMetadata oxycephaly1 = new ImmutableTermIdWithMetadata( oxycephalyId,defaultFrequency,null);
        TermIdWithMetadata oxycephaly2 = new ImmutableTermIdWithMetadata( oxycephalyId,defaultFrequency,HpoOnset.ADULT_ONSET);
        assertNotEquals(oxycephaly1,oxycephaly2);
    }

    @Test
    public void testEqualityOfTerms3() {
        TermId oxycephalyId = new ImmutableTermId(HP_PREFIX,"0000263");
        TermIdWithMetadata oxycephaly1 = new ImmutableTermIdWithMetadata( oxycephalyId,defaultFrequency,HpoOnset.ADULT_ONSET);
        TermIdWithMetadata oxycephaly2 = new ImmutableTermIdWithMetadata( oxycephalyId,defaultFrequency,HpoOnset.ADULT_ONSET);
        assertEquals(oxycephaly1,oxycephaly2);
    }

    @Test
    public void testEqualityOfTerms4() {
        TermId oxycephalyId = new ImmutableTermId(HP_PREFIX,"0000263");
        TermIdWithMetadata oxycephaly1 = new ImmutableTermIdWithMetadata( oxycephalyId,HpoFrequency.ALWAYS_PRESENT,HpoOnset.ADULT_ONSET);
        TermIdWithMetadata oxycephaly2 = new ImmutableTermIdWithMetadata( oxycephalyId,HpoFrequency.OCCASIONAL,HpoOnset.ADULT_ONSET);
        assertNotEquals(oxycephaly1,oxycephaly2);
    }





}
