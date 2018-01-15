package org.monarchinitiative.lr2pg.io;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class HPOOntologyParserTest {
    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private static Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private static Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;
    private static TermPrefix hpoPrefix=null;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = HPOOntologyParserTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        parser.parseOntology();
        phenotypeSubOntology = parser.getPhenotypeSubontology();
        inheritanceSubontology = parser.getInheritanceSubontology();
        hpoPrefix = new ImmutableTermPrefix("HP");
    }


    @Test
    public void testNonNullPhenotypeOntology() {
        Assert.assertNotNull(phenotypeSubOntology);
    }

    /** The root term of the HPO is HP:0000118 */
    @Test
    public void testGetRootTermId() {
        TermId rootId = new ImmutableTermId(hpoPrefix,"0000118");
        Assert.assertEquals(rootId, phenotypeSubOntology.getRootTermId());
    }

    /** The root term of the inheritance submodule of the HPO is HP:0000005 */
    @Test
    public void testGetInheritanceRootTermId() {
        TermId rootId = new ImmutableTermId(hpoPrefix,"0000005");
        Assert.assertEquals(rootId,inheritanceSubontology.getRootTermId());
    }


    /** There are currently over 13000 terms, don't know exact number, but we should get over 10,000 */
    @Test
    public void testGetAtLeastTenThousandTerms() {
        int count = phenotypeSubOntology.countAllTerms();
        Assert.assertTrue(count>10_000);
    }

    /** The term for "Sporadic" is in the inheritance subontology and not the phenotype subontology. */
    @Test
    public void testInCorrectSubontology1() {
        TermId sporadic = new ImmutableTermId(hpoPrefix,"0003745");
        Assert.assertTrue(inheritanceSubontology.getNonObsoleteTermIds().contains(sporadic));
        Assert.assertFalse(phenotypeSubOntology.getNonObsoleteTermIds().contains(sporadic));
    }

    /** The term for "Otitis media" is in the phenotype subontology and not the inheritance subontology */
    @Test
    public void testInCorrectSubontology2() {
        TermId otitisMedia = new ImmutableTermId(hpoPrefix,"0000388");
        Assert.assertFalse(inheritanceSubontology.getNonObsoleteTermIds().contains(otitisMedia));
        Assert.assertTrue(phenotypeSubOntology.getNonObsoleteTermIds().contains(otitisMedia));
    }

    /** Abnormality of the middle ear (HP:0000370) should have the ancestors Abnormality of the ear (HP:0000598)
     * and Phenotypic abnormality (HP:0000118). Note that ancestors includes the term itself! */
    @Test
    public void testGetAncestors() {
        TermId abnMiddleEar = new ImmutableTermId(hpoPrefix,"0000370");
        TermId abnEar = new ImmutableTermId(hpoPrefix,"0000598");
        TermId rootId = new ImmutableTermId(hpoPrefix,"0000118");
        Set<TermId> ancTermIds = phenotypeSubOntology.getAncestorTermIds(abnMiddleEar);
        Set<TermId> expected = new HashSet<TermId>();
        expected.add(rootId);
        expected.add(abnEar);
        expected.add(abnMiddleEar);
        Assert.assertEquals(expected,ancTermIds);
    }
    /** The term for "Autosomal dominant inheritance" is in the inheritance subontology and not the phenotype subontology. */
    @Test
    public void testInCorrectSubontology3() {
        TermId sporadic = new ImmutableTermId(hpoPrefix,"0000006");
        Assert.assertTrue(inheritanceSubontology.getNonObsoleteTermIds().contains(sporadic));
        Assert.assertFalse(phenotypeSubOntology.getNonObsoleteTermIds().contains(sporadic));
    }

    /** The term for "Functional abnormality of the bladder" is in the phenotype subontology and not the inheritance subontology. */
    @Test
    public void testInCorrectSubontology4() {
        TermId sporadic = new ImmutableTermId(hpoPrefix,"0000009");
        Assert.assertFalse(inheritanceSubontology.getNonObsoleteTermIds().contains(sporadic));
        Assert.assertTrue(phenotypeSubOntology.getNonObsoleteTermIds().contains(sporadic));
    }


}
