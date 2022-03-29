package org.monarchinitiative.lirical.output;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.lirical.vcf.SimpleVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class DifferentialDiagnosisTest {

    private final String phenotypeExplanation="example1";
    private final String expectedShortName="BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";


    /** Test that the private function prettifyDiseaseName (which is called from the constructor)
     * can remove all of the grunge from this long original disease name.
     */
    @Test
    public void prettifyNameTest1() {
        String originalName="#101600 PFEIFFER SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE V; ACS5;;ACS V;;NOACK SYNDROMECRANIOFACIAL-SKELETAL-DERMATOLOGIC DYSPLASIA, INCLUDED;";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.getDiseaseName()).thenReturn(originalName);
        when(result.calculatePosttestProbability()).thenReturn(0.001);
        when(result.diseaseId()).thenReturn(TermId.of("OMIM:101600"));
        when(result.getRank()).thenReturn(1);
        when(result.calculatePosttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.genotypeLr()).thenReturn(Optional.empty());
        String expectedShortName="PFEIFFER SYNDROME";
        DifferentialDiagnosis dd = new DifferentialDiagnosis(result);
        assertEquals(expectedShortName,dd.getDiseaseName());
    }



    DifferentialDiagnosis brachyphalangy() {
        String originalName="609945 BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.getDiseaseName()).thenReturn(originalName);
        when(result.calculatePosttestProbability()).thenReturn(0.001);
        when(result.diseaseId()).thenReturn(TermId.of("OMIM:101600"));
        when(result.getRank()).thenReturn(1);
        when(result.calculatePosttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.genotypeLr()).thenReturn(Optional.empty());
        DifferentialDiagnosis dd = new DifferentialDiagnosis(result);
        dd.setPhenotypeExplanation(phenotypeExplanation);
        return dd;
    }


    /** Test that the leading number is removed from the disease name. */
    @Test
    public void prettifyNameTest2() {
        DifferentialDiagnosis dd = brachyphalangy();
        assertEquals(expectedShortName,dd.getDiseaseName());
    }






    /** Test that we are correctly setting the flag that is used by the FreeMarker to show variants. */
    @Test
    public void checkShowVariants() {
        DifferentialDiagnosis dd = brachyphalangy();
        Gene2Genotype g2g = Mockito.mock(Gene2Genotype.class);
        when(g2g.getSymbol()).thenReturn("FakeSymbol");
        List<SimpleVariant> emptylist= ImmutableList.of();
        when(g2g.getVarList()).thenReturn(emptylist);
        dd.addG2G(g2g);
        assertEquals("yes",dd.getHasVariants());
    }


    @Test
    public void checkHasPhenotypeExplanation() {
        DifferentialDiagnosis dd = brachyphalangy();
        dd.setPhenotypeExplanation(phenotypeExplanation);
        assertTrue(dd.hasPhenotypeExplanation());
        assertFalse(dd.hasGenotypeExplanation());
    }


}
