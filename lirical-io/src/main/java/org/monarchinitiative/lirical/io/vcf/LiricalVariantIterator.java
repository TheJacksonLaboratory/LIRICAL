package org.monarchinitiative.lirical.io.vcf;


import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;

import java.util.*;

class LiricalVariantIterator implements Iterator<LiricalVariant> {

    private final Iterator<GenotypedVariant> iterator;
    private final FunctionalVariantAnnotator variantAnnotator;
    private final VariantMetadataService metadataService;

    LiricalVariantIterator(Iterator<GenotypedVariant> iterator,
                           FunctionalVariantAnnotator variantAnnotator,
                           VariantMetadataService metadataService) {
        this.iterator = Objects.requireNonNull(iterator, "Iterator must not be null!");
        this.variantAnnotator = Objects.requireNonNull(variantAnnotator, "Variant annotator must not be null!");
        this.metadataService = Objects.requireNonNull(metadataService, "Metadata service must not be null!");
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public LiricalVariant next() {
        GenotypedVariant gv = iterator.next();

        List<TranscriptAnnotation> annotations = variantAnnotator.annotate(gv.variant());
        if (gv.failedFilters())
            // No point in further variant annotation if the variant failed the initial filtering.
            return new LiricalVariantFailingFilters(gv, annotations);

        List<VariantEffect> effects = annotations.stream()
                .map(TranscriptAnnotation::getVariantEffects)
                .flatMap(Collection::stream)
                .distinct()
                .toList();

        VariantMetadata metadata = metadataService.metadata(gv.variant(), effects);
        return LiricalVariant.of(gv, annotations, metadata);
    }
}
