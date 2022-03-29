package org.monarchinitiative.lirical.io;

import org.monarchinitiative.lirical.model.LiricalVariant;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface VariantParser extends Iterable<LiricalVariant> {

    default Stream<LiricalVariant> variantStream() {
        return StreamSupport.stream(spliterator(), false);
    }

}
