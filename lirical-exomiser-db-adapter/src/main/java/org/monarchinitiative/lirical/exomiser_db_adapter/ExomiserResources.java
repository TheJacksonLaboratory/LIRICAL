package org.monarchinitiative.lirical.exomiser_db_adapter;

import java.nio.file.Path;

/**
 * Paths to Exomiser resources needed to support LIRICAL analysis
 * for a {@link org.monarchinitiative.lirical.core.model.GenomeBuild}.
 *
 * @param exomiserAlleleDb path to the variant database file.
 *                         Usually, the database file is named <code>2406_hg38_variants.mv.db</code> or similar.
 * @param exomiserClinvarDb path to the clinvar database file.
 *                          Usually, the database file is named <code>2406_hg38_clinvar.mv.db</code> or similar.
 */
public record ExomiserResources(
        Path exomiserAlleleDb, Path exomiserClinvarDb
) {
}
