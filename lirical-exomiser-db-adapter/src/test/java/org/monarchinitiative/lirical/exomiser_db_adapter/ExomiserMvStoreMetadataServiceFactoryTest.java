package org.monarchinitiative.lirical.exomiser_db_adapter;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.model.ClinVarAlleleData;
import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ExomiserMvStoreMetadataServiceFactoryTest {

    private static final Path TESTDATA_DIR = Path.of("src/test/resources/testdata");

    private ExomiserMvStoreMetadataServiceFactory factory;

    @BeforeEach
    public void setUp() {
        Map<GenomeBuild, Path> alleleDbPaths = Map.of(
                GenomeBuild.HG38, TESTDATA_DIR.resolve("9999_hg38_variants.mv.db")
        );
        Map<GenomeBuild, Path> clinvarDbPaths = Map.of(
                GenomeBuild.HG38, TESTDATA_DIR.resolve("9999_hg38_clinvar.mv.db")
        );
        factory = ExomiserMvStoreMetadataServiceFactory.of(alleleDbPaths, clinvarDbPaths);
    }

    @AfterEach
    public void tearDown() {
        factory.close();
        factory = null;
    }

    @Test
    public void checkWeHaveTheServiceForHg38() {
        Optional<VariantMetadataService> vmso = factory.getVariantMetadataService(GenomeBuild.HG38);

        assertThat(vmso.isPresent(), equalTo(true));
    }

    @Test
    public void fetchLmnaMetadata() {
        VariantMetadataService service = factory.getVariantMetadataService(GenomeBuild.HG38).orElseThrow();

        VariantMetadata metadata = service.metadata(TestVariants.lmnaVariant(), List.of(VariantEffect.MISSENSE_VARIANT, VariantEffect.INTRON_VARIANT));

        assertThat(metadata.frequency().isPresent(), equalTo(true));
        assertThat((double) metadata.frequency().get(), closeTo(0.003521949f, 1E-5));

        assertThat((double) metadata.pathogenicity(), closeTo(1.0, 1E-5));

        assertThat(metadata.clinVarAlleleData().isPresent(), equalTo(true));
        ClinVarAlleleData clinVarAlleleData = metadata.clinVarAlleleData().get();
        assertThat(clinVarAlleleData.getClinvarClnSig(), equalTo(ClinvarClnSig.CONFLICTING_PATHOGENICITY_INTERPRETATIONS));
        assertThat(clinVarAlleleData.getAlleleId().isPresent(), equalTo(true));
        assertThat(clinVarAlleleData.getAlleleId().get(), equalTo(14485L));
    }

    @Test
    public void fetchDmdMetadata() {
        VariantMetadataService service = factory.getVariantMetadataService(GenomeBuild.HG38).orElseThrow();

        VariantMetadata metadata = service.metadata(TestVariants.dmdVariant(), List.of(VariantEffect.MISSENSE_VARIANT));

        assertThat(metadata.frequency().isPresent(), equalTo(true));
        assertThat((double) metadata.frequency().get(), closeTo(0., 1E-5));

        assertThat((double) metadata.pathogenicity(), closeTo(1.0, 1E-5));

        assertThat(metadata.clinVarAlleleData().isPresent(), equalTo(true));
        ClinVarAlleleData clinVarAlleleData = metadata.clinVarAlleleData().get();
        assertThat(clinVarAlleleData.getClinvarClnSig(), equalTo(ClinvarClnSig.PATHOGENIC));
        assertThat(clinVarAlleleData.getAlleleId().isPresent(), equalTo(true));
        assertThat(clinVarAlleleData.getAlleleId().get(), equalTo(11236L));
    }
}