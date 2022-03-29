package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.model.GenotypedVariant;
import org.monarchinitiative.lirical.model.LiricalVariant;
import org.monarchinitiative.lirical.model.VariantMetadata;
import org.monarchinitiative.lirical.service.VariantMetadataService;

import java.util.Iterator;
import java.util.Objects;

class LiricalVariantIterator implements Iterator<LiricalVariant> {

    private final Iterator<GenotypedVariant> iterator;
    private final VariantMetadataService metadataService;

    LiricalVariantIterator(Iterator<GenotypedVariant> iterator, VariantMetadataService metadataService) {
        this.iterator = Objects.requireNonNull(iterator, "Iterator must not be null");
        this.metadataService = Objects.requireNonNull(metadataService, "Metadata service must not be null");
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public LiricalVariant next() {
        GenotypedVariant next = iterator.next();
        VariantMetadata metadata = metadataService.metadata(next.variant());
        return LiricalVariant.of(next, metadata);
    }
}
