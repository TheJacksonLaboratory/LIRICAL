package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class BackgroundVariantFrequencyServiceImpl implements BackgroundVariantFrequencyService {

    private final Map<TermId, Double> frequencyMap;
    private final double defaultVariantBackgroundFrequency;

    BackgroundVariantFrequencyServiceImpl(Map<TermId, Double> frequencyMap,
                                          double defaultVariantBackgroundFrequency) {
        this.frequencyMap = Objects.requireNonNull(frequencyMap);
        this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
    }

    @Override
    public double defaultVariantBackgroundFrequency() {
        return defaultVariantBackgroundFrequency;
    }

    @Override
    public Optional<Double> frequencyForGene(TermId geneId) {
        return Optional.ofNullable(frequencyMap.get(geneId));
    }
}
