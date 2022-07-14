package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class BackgroundVariantFrequencyServiceImpl implements BackgroundVariantFrequencyService {

    private final Map<TermId, Double> frequencyMap;
    private final double defaultVariantFrequency;

    BackgroundVariantFrequencyServiceImpl(Map<TermId, Double> frequencyMap,
                                          double defaultVariantFrequency) {
        this.frequencyMap = Objects.requireNonNull(frequencyMap);
        this.defaultVariantFrequency = defaultVariantFrequency;
    }

    @Override
    public double defaultVariantFrequency() {
        return defaultVariantFrequency;
    }

    @Override
    public Optional<Double> frequencyForGene(TermId geneId) {
        return Optional.ofNullable(frequencyMap.get(geneId));
    }
}
