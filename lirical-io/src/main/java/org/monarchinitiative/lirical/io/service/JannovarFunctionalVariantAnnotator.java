package org.monarchinitiative.lirical.io.service;

import de.charite.compbio.jannovar.annotation.Annotation;
import de.charite.compbio.jannovar.annotation.VariantAnnotations;
import de.charite.compbio.jannovar.annotation.VariantAnnotator;
import de.charite.compbio.jannovar.annotation.builders.AnnotationBuilderOptions;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.PositionType;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifiers;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JannovarFunctionalVariantAnnotator implements FunctionalVariantAnnotator {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarFunctionalVariantAnnotator.class);

    private static final AnnotationBuilderOptions OPTIONS = new AnnotationBuilderOptions();
    private final ReferenceDictionary rd;
    private final VariantAnnotator annotator;
    private final Map<String, GeneIdentifier> symbolToGeneId;

    /**
     * @deprecated to be removed in v2.0.0, use {@link #of(JannovarData, GeneIdentifiers)} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    public static JannovarFunctionalVariantAnnotator of(JannovarData jannovarData, List<GeneIdentifier> geneIdentifiers) {
        return of(jannovarData, GeneIdentifiers.of(geneIdentifiers));
    }

    public static JannovarFunctionalVariantAnnotator of(JannovarData jannovarData, GeneIdentifiers geneIdentifiers) {
        return new JannovarFunctionalVariantAnnotator(jannovarData, geneIdentifiers);
    }

    private JannovarFunctionalVariantAnnotator(JannovarData jannovarData, GeneIdentifiers geneIdentifiers) {
        this.rd = Objects.requireNonNull(jannovarData).getRefDict();
        this.annotator = new VariantAnnotator(rd, jannovarData.getChromosomes(), OPTIONS);
        this.symbolToGeneId = Objects.requireNonNull(geneIdentifiers).stream()
                .collect(Collectors.toMap(GeneIdentifier::symbol, Function.identity()));
    }

    @Override
    public List<TranscriptAnnotation> annotate(GenomicVariant variant) {
        if (variant.isSymbolic() || variant.isBreakend()) {
            LOGGER.debug("Skipping symbolic/breakend variant {}", formatVariant(variant));
            return List.of();
        }

        Integer id = rd.getContigNameToID().get(variant.contigName());
        int pos = variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased());
        if (id == null) {
            LOGGER.debug("Unknown contig {} in variant {}", variant.contigName(), formatVariant(variant));
            return List.of();
        }

        VariantAnnotations annotations;
        try {
            annotations = annotator.buildAnnotations(id, pos, variant.ref(), variant.alt(), PositionType.ONE_BASED);
        } catch (Exception e) {
            LOGGER.warn("Error annotating variant {}: {}", formatVariant(variant), e.getMessage());
            return List.of();
        }

        return annotations.getAnnotations().stream()
                .map(toTranscriptAnnotation())
                .flatMap(Optional::stream)
                .toList();
    }

    private static String formatVariant(GenomicVariant variant) {
        return "%s:%s:%d:%s>%s".formatted(variant.id(),
                variant.contigName(),
                variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()),
                variant.ref(),
                variant.alt());
    }

    private Function<Annotation, Optional<TranscriptAnnotation>> toTranscriptAnnotation() {
        return ann -> {
            GeneIdentifier id = symbolToGeneId.get(ann.getGeneSymbol());
            if (id == null) {
                LOGGER.trace("Unknown gene symbol {}", ann.getGeneSymbol());
                return Optional.empty();
            }

            return Optional.of(new JannovarTranscriptAnnotation(id, ann));
        };
    }
}
