package org.monarchinitiative.lirical.io.service;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.List;
import java.util.stream.Collectors;

record SimpleTxAnnotation(
        GeneIdentifier geneId,
        String accession,
        List<VariantEffect> variantEffects,
        String hgvsCdna,
        String hgvsProtein
) implements TranscriptAnnotation {

    @Override
    public GeneIdentifier getGeneId() {
        return geneId;
    }

    @Override
    public String getAccession() {
        return accession;
    }

    @Override
    public List<VariantEffect> getVariantEffects() {
        return variantEffects;
    }

    @Override
    public String getVariantEffect() {
        return getVariantEffects().stream()
                .map(VariantEffect::name)
                .collect(Collectors.joining("+"));
    }

    @Override
    public String getHgvsCdna() {
        return hgvsCdna;
    }

    @Override
    public String getHgvsProtein() {
        return hgvsProtein;
    }

}
