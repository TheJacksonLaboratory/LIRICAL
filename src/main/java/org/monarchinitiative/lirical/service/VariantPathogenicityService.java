package org.monarchinitiative.lirical.service;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.Optional;

public interface VariantPathogenicityService {

    Optional<VariantPathogenicity> getPathogenicity(GenomicVariant variant);

}
