package org.monarchinitiative.lr2pg.output;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.lr2pg.analysis.Gene2Genotype;
import org.monarchinitiative.lr2pg.likelihoodratio.TestResult;
import org.monarchinitiative.lr2pg.vcf.SimpleVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class DifferentialDiagnosisTest {


    /** Test that the private function prettifyDiseaseName (which is called from the constructor)
     * can remove all of the grunge from this long original disease name.
     */
    @Test
    void prettifyNameTest1() {
        String originalName="#101600 PFEIFFER SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE V; ACS5;;ACS V;;NOACK SYNDROMECRANIOFACIAL-SKELETAL-DERMATOLOGIC DYSPLASIA, INCLUDED;";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.getDiseaseName()).thenReturn(originalName);
        when(result.getPosttestProbability()).thenReturn(0.001);
        when(result.getDiseaseCurie()).thenReturn(TermId.of("OMIM:101600"));
        when(result.getRank()).thenReturn(1);
        when(result.getPosttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.hasGenotype()).thenReturn(false);
        String expectedShortName="PFEIFFER SYNDROME";
        DifferentialDiagnosis dd = new DifferentialDiagnosis(result);
        assertEquals(expectedShortName,dd.getDiseaseName());
    }

    /** Test that the leading number is removed from the disease name. */
    @Test
    void prettifyNameTest2() {
        String originalName="609945 BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.getDiseaseName()).thenReturn(originalName);
        when(result.getPosttestProbability()).thenReturn(0.001);
        when(result.getDiseaseCurie()).thenReturn(TermId.of("OMIM:101600"));
        when(result.getRank()).thenReturn(1);
        when(result.getPosttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.hasGenotype()).thenReturn(false);
        String expectedShortName="BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        DifferentialDiagnosis dd = new DifferentialDiagnosis(result);
        assertEquals(expectedShortName,dd.getDiseaseName());
    }


    /** Test that we are correctly setting the flag that is used by the FreeMarker to show variants. */
    @Test
    void checkShowVariants() {
        String originalName="609945 BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.getDiseaseName()).thenReturn(originalName);
        when(result.getPosttestProbability()).thenReturn(0.001);
        when(result.getDiseaseCurie()).thenReturn(TermId.of("OMIM:101600"));
        when(result.getRank()).thenReturn(1);
        when(result.getPosttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.hasGenotype()).thenReturn(false);
        String expectedShortName="BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        DifferentialDiagnosis dd = new DifferentialDiagnosis(result);
        Gene2Genotype g2g = Mockito.mock(Gene2Genotype.class);
        when(g2g.getSymbol()).thenReturn("FakeSymbol");
        List<SimpleVariant> emptylist= ImmutableList.of();
        when(g2g.getVarList()).thenReturn(emptylist);
        dd.addG2G(g2g);
        assertEquals("yes",dd.getHasVariants());
    }
}
