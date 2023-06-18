package org.monarchinitiative.lirical.core.model;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GenesAndGenotypesTest {

    private static final GenomicAssembly HG38 = GenomicAssemblies.GRCh38p13();

    @Test
    public void computeFilteringStats() {
        GenesAndGenotypes gag = prepareToyGenesAndGenotypes();

        FilteringStats filteringStats = gag.computeFilteringStats();
        assertThat(filteringStats.nFilteredVariants(), equalTo(13L));
        assertThat(filteringStats.nPassingVariants(), equalTo(2L));
        assertThat(filteringStats.genesWithVariants(), equalTo(1L));
    }

    private static GenesAndGenotypes prepareToyGenesAndGenotypes() {
        return GenesAndGenotypes.of(List.of(
                Gene2Genotype.of(
                        GeneIdentifier.of(TermId.of("HGNC:1234"), "FAKE1234"),
                        List.of(
                                LiricalVariant.of(
                                GenotypedVariant.of(GenomeBuild.HG38,
                                        GenomicVariant.of(HG38.contigByName("1"), "SNP1",
                                                Strand.POSITIVE, CoordinateSystem.ONE_BASED, 101,
                                                "C", "G"),
                                        List.of(),
                                        true),
                                List.of(), VariantMetadata.empty()), // irrelevant
                                LiricalVariant.of(
                                        GenotypedVariant.of(GenomeBuild.HG38,
                                                GenomicVariant.of(HG38.contigByName("1"), "SNP1",
                                                        Strand.POSITIVE, CoordinateSystem.ONE_BASED, 201,
                                                        "T", "A"),
                                                List.of(), true),
                                        List.of(), VariantMetadata.empty()) // irrelevant
                        ),
                        3),
                Gene2Genotype.of(
                        GeneIdentifier.of(TermId.of("HGNC:1234"), "FAKE1234"),
                        List.of(),
                        10
                )
        ));
    }
}