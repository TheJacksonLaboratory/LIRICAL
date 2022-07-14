package org.monarchinitiative.lirical.core.io;


import org.monarchinitiative.lirical.core.model.LiricalVariant;

import java.util.Collection;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface VariantParser extends Iterable<LiricalVariant>, AutoCloseable {

    Collection<String> sampleNames();

    default Stream<LiricalVariant> variantStream() {
        return StreamSupport.stream(spliterator(), false);
    }

}
