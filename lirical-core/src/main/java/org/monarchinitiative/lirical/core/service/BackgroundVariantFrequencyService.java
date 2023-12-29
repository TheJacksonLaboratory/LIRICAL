package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Optional;

public interface BackgroundVariantFrequencyService {

    static BackgroundVariantFrequencyService of(Map<TermId, Double> frequencyMap, double defaultVariantBackgroundFrequency) {
        return new BackgroundVariantFrequencyServiceImpl(frequencyMap, defaultVariantBackgroundFrequency);
    }

    double defaultVariantBackgroundFrequency();

    Optional<Double> frequencyForGene(TermId geneId);

}
