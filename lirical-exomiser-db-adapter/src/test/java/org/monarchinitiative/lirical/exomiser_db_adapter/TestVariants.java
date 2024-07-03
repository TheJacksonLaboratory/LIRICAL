package org.monarchinitiative.lirical.exomiser_db_adapter;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

/**
 * A few variants with Clinvar, pathogenicity, and frequency data.
 */
public class TestVariants {

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final GenomicVariant LMNA_VARIANT = GenomicVariant.of(
            ASSEMBLY.contigByName("chr1"), "rs80338938", Strand.POSITIVE,
            Coordinates.of(CoordinateSystem.ZERO_BASED, 156_137_755, 156_137_756), "C", "A");
    private static final GenomicVariant DMD_VARIANT = GenomicVariant.of(
            ASSEMBLY.contigByName("chrX"), "rs104894788", Strand.POSITIVE,
            Coordinates.of(CoordinateSystem.ZERO_BASED, 31_180_436, 31_180_437), "C", "T");

    private TestVariants() {}

    public static GenomicVariant lmnaVariant() {
        // https://www.ncbi.nlm.nih.gov/clinvar/variation/14485
        return LMNA_VARIANT;
    }

    public static GenomicVariant dmdVariant() {
        https://www.ncbi.nlm.nih.gov/clinvar/variation/11236/
        return DMD_VARIANT;
    }
}
