package org.monarchinitiative.lirical.io.service;

import de.charite.compbio.jannovar.annotation.Annotation;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.util.List;
import java.util.Objects;

class JannovarTranscriptAnnotation implements TranscriptAnnotation {

    private final GeneIdentifier geneIdentifier;
    private final Annotation annotation;

    JannovarTranscriptAnnotation(GeneIdentifier geneIdentifier, Annotation annotation) {
        this.geneIdentifier = Objects.requireNonNull(geneIdentifier);
        this.annotation = Objects.requireNonNull(annotation);
    }


    @Override
    public GeneIdentifier getGeneId() {
        return geneIdentifier;
    }

    @Override
    public String getAccession() {
        return annotation.getTranscript().getAccession();
    }

    @Override
    public List<VariantEffect> getVariantEffects() {
        return List.copyOf(annotation.getEffects());
    }

    @Override
    public String getVariantEffect() {
        return String.join("+",
                getVariantEffects().stream()
                        .map(VariantEffect::name)
                        .toList());
    }

    @Override
    public String getHgvsCdna() {
        return annotation.getCDSNTChangeStr();
    }

    @Override
    public String getHgvsProtein() {
        return annotation.getProteinChangeStr();
    }
}
