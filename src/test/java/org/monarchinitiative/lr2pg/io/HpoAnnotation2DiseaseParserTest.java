package org.monarchinitiative.lr2pg.io;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.monarchinitiative.lr2pg.hpo.HpoDiseaseWithMetadata;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class HpoAnnotation2DiseaseParserTest {
    /** The subontology of the HPO with all the phenotypic abnormality terms. */
    private static Ontology<HpoTerm, HpoTermRelation> phenotypeSubOntology =null;
    /** The subontology of the HPO with all the inheritance terms. */
    private static Ontology<HpoTerm, HpoTermRelation> inheritanceSubontology=null;
    private static TermPrefix hpoPrefix=null;

    private static HpoAnnotation2DiseaseParser annotationParser=null;

    private static Map<String,HpoDiseaseWithMetadata> diseaseMap;

    @BeforeClass
    public static void setup() throws IOException {
        ClassLoader classLoader = HPOOntologyParserTest.class.getClassLoader();
        String hpoPath = classLoader.getResource("hp.obo").getFile();
        HpoOntologyParser parser = new HpoOntologyParser(hpoPath);
        String annotationPath = classLoader.getResource("small_phenoannot.tab").getFile();
        parser.parseOntology();
        phenotypeSubOntology = parser.getPhenotypeSubontology();
        inheritanceSubontology = parser.getInheritanceSubontology();
        hpoPrefix = new ImmutableTermPrefix("HP");
        annotationParser = new HpoAnnotation2DiseaseParser(annotationPath,phenotypeSubOntology,inheritanceSubontology);
        diseaseMap=annotationParser.getDiseaseMap();
    }


    @Test
    public void notNullTest() {
        assertNotNull(annotationParser!=null);
    }

    /**
     * small_phenoannot.tab contains annotations for a total of 196 diseases:
     * <pre>
     * $ cut -f 2 small_phenoannot.tab | sort | uniq | wc -l
     * 196
     * </pre>
     */
    @Test
    public void testGetRightNumberOfDiseases() {
        int expected = 196;
        assertEquals(expected,diseaseMap.size());
    }

    /*
    OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000006      OMIM:101120     IEA                             I               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000263      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000272      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000274      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000303      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000316      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000327      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000369      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000377      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000470      OMIM:101120     TAS                             O               2009-02-17      HPO:probinson
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000586      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0000678      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0001159      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0001177      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0001363      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0009816      OMIM:101120     IEA                             O               2009-02-17      HPO:curators
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0010055      OMIM:101120     TAS                             O               2012-06-08      HPO:probinson
OMIM    101120  ACROCEPHALOPOLYSYNDACTYLY TYPE III              HP:0011304      OMIM:101120     TAS                             O               2009-02-17      HPO:probinson

     */


}
