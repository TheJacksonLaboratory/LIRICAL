package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Optional;

public interface BackgroundVariantFrequencyService {

    static BackgroundVariantFrequencyService of(Map<TermId, Double> frequencyMap, double defaultVariantFrequency) {
        return new BackgroundVariantFrequencyServiceImpl(frequencyMap, defaultVariantFrequency);
    }

    double defaultVariantFrequency();

    Optional<Double> frequencyForGene(TermId geneId);

}
