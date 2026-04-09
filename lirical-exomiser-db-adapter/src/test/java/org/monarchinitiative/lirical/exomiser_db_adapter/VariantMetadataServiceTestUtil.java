package org.monarchinitiative.lirical.exomiser_db_adapter;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.ClinVarAlleleData;
import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

class VariantMetadataServiceTestUtil {

    static void testLmnaMetadata(VariantMetadataService service) {
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

    static void testDmdMetadata(VariantMetadataService service) {
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
