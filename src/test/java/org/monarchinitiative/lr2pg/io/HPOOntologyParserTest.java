package org.monarchinitiative.lr2pg.io;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import org.junit.BeforeClass;
import org.monarchinitiative.lr2pg.hpo.HPOParserTest;
import org.monarchinitiative.lr2pg.old.HPOParser;

public class HPOOntologyParserTest {
    private static Ontology<HpoTerm, HpoTermRelation> hpoOntology=null;

    @BeforeClass
    public static void setup() {
        ClassLoader classLoader = HPOOntologyParserTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        HPOOntologyParserTest parser = new HPOOntologyParserTest();
        //hpoOntology = parser.parseOntology(hpoPath);

    }
}
