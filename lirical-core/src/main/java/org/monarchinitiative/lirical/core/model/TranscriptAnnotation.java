package org.monarchinitiative.lirical.core.model;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.Comparator;
import java.util.List;

/**
 * The interface represents variant functional annotation data required by LIRICAL.
 */
public interface TranscriptAnnotation {

    // Note: the interface data is used in HTML template, hence the get prefixes must be present in the method names.
    //  It looks like Freemarker uses reflection to get field values instead of calling the methods below though.

    /** @return gene identifier of the transcript. */
    GeneIdentifier getGeneId();

    /** @return transcript accession (e.g. <code>NM_000130.4</code>). */
    String getAccession();

    /** @return list of variant effects (e.g. <code>MISSENSE_VARIANT</code>). */
    List<VariantEffect> getVariantEffects();

    /** @return the most pathogenic variant effect. */
    default VariantEffect getMostPathogenicVariantEffect() {
        return getVariantEffects().stream()
                .min(Comparator.comparingInt(VariantEffect::ordinal))
                .orElse(VariantEffect.SEQUENCE_VARIANT);
    }

    /** @return string of variant effects joined by <code>+</code> character. */
    String getVariantEffect();

    /** @return effect of the variant on the cDNA sequence in HGVS format (e.g. <code>c.578G>T</code>). */
    String getHgvsCdna();

    /** @return effect of the variant on the protein sequence in HGVS format (e.g. <code>p.(Cys193Phe)</code>). */
    String getHgvsProtein();

}
