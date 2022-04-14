package org.monarchinitiative.lirical.core.output;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.AssignedMoleculeType;
import org.monarchinitiative.svart.assembly.SequenceRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class DifferentialDiagnosisTest {

    private final String expectedShortName="BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";


    @BeforeAll
    public static void beforeAll() {
        Contig contig = Contig.of(1, "ctg1", SequenceRole.ASSEMBLED_MOLECULE, "ctg1", AssignedMoleculeType.CHROMOSOME, 1000, "", "", "");
        GenomicVariant gv = GenomicVariant.of(contig, "id", Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 1, 1), "C", "G");
        GenotypedVariant gtv = GenotypedVariant.of(GenomeBuild.HG38, gv, Map.of(), true);
        LiricalVariant variant = LiricalVariant.of(gtv, VariantMetadata.empty());
    }

    /** Test that the private function prettifyDiseaseName (which is called from the constructor)
     * can remove all of the grunge from this long original disease name.
     */
    @Test
    public void prettifyNameTest1() {
        String originalName="#101600 PFEIFFER SYNDROME;;ACROCEPHALOSYNDACTYLY, TYPE V; ACS5;;ACS V;;NOACK SYNDROMECRANIOFACIAL-SKELETAL-DERMATOLOGIC DYSPLASIA, INCLUDED;";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.posttestProbability()).thenReturn(0.001);
        when(result.diseaseId()).thenReturn(TermId.of("OMIM:101600"));
        when(result.posttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.genotypeLr()).thenReturn(Optional.empty());
        String expectedShortName="PFEIFFER SYNDROME";
        DifferentialDiagnosis dd = new DifferentialDiagnosis("SampleId", TermId.of("OMIM:101600"), originalName, result, 1, List.of(), "", "");
        assertEquals(expectedShortName,dd.getDiseaseName());
    }



    DifferentialDiagnosis brachyphalangy() {
        String originalName="609945 BRACHYPHALANGY, POLYDACTYLY, AND TIBIAL APLASIA/HYPOPLASIA";
        TestResult result;
        result = Mockito.mock(TestResult.class);
        when(result.posttestProbability()).thenReturn(0.001);
        when(result.diseaseId()).thenReturn(TermId.of("OMIM:101600"));
        when(result.posttestProbability()).thenReturn(0.1);
        when(result.getCompositeLR()).thenReturn(0.3);
        when(result.genotypeLr()).thenReturn(Optional.empty());
        return new DifferentialDiagnosis("SampleId", TermId.of("OMIM:101600"), originalName, result, 1, List.of(), "", "");
    }


    /** Test that the leading number is removed from the disease name. */
    @Test
    public void prettifyNameTest2() {
        DifferentialDiagnosis dd = brachyphalangy();
        assertEquals(expectedShortName,dd.getDiseaseName());
    }

}
