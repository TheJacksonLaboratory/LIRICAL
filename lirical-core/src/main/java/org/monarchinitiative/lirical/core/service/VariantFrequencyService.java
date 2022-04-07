package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Optional;

public interface VariantFrequencyService {

    Optional<Float> getFrequency(GenomicVariant variant);

}
