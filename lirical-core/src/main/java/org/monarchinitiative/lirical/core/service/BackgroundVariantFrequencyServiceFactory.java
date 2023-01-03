package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.GenomeBuild;

import java.util.Optional;

public interface BackgroundVariantFrequencyServiceFactory {

    Optional<BackgroundVariantFrequencyService> forGenomeBuild(GenomeBuild genomeBuild, double defaultVariantBackgroundFrequency);

}
