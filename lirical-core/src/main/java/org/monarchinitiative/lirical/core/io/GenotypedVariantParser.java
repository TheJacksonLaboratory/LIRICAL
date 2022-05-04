package org.monarchinitiative.lirical.core.io;


import org.monarchinitiative.lirical.core.model.GenotypedVariant;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface GenotypedVariantParser extends Iterable<GenotypedVariant> {

    default Stream<GenotypedVariant> variantStream() {
        return StreamSupport.stream(spliterator(), false);
    }

}
