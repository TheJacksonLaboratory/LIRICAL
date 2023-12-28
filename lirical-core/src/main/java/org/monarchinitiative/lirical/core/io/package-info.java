/**
 * APIs for reading and annotation of genomic variants.
 * <p>
 * LIRICAL needs to read genomic variants, perform functional annotation, and fetch variant frequencies for the variants.
 * LIRICAL does not care about how this is done, as long as the variants meet
 * the {@link org.monarchinitiative.lirical.core.model.LiricalVariant} requirements.
 * <p>
 * One way to configure the functional annotation is to implement {@link org.monarchinitiative.lirical.core.io.VariantParserFactory}
 * which can provide a {@link org.monarchinitiative.lirical.core.io.VariantParser} to read variants
 * from a {@link java.nio.file.Path} given {@link org.monarchinitiative.lirical.core.model.GenomeBuild}
 * and {@link org.monarchinitiative.lirical.core.model.TranscriptDatabase}. For instance, to read variants
 * from a VCF file.
 */
package org.monarchinitiative.lirical.core.io;