package org.monarchinitiative.lirical.configuration.impl;

import org.monarchinitiative.lirical.core.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

public class BundledBackgroundVariantFrequencyServiceFactory implements BackgroundVariantFrequencyServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundledBackgroundVariantFrequencyServiceFactory.class);

    private static final BundledBackgroundVariantFrequencyServiceFactory INSTANCE = new BundledBackgroundVariantFrequencyServiceFactory();

    public static BundledBackgroundVariantFrequencyServiceFactory getInstance() {
        return INSTANCE;
    }

    private BundledBackgroundVariantFrequencyServiceFactory() {
    }

    @Override
    public Optional<BackgroundVariantFrequencyService> forGenomeBuild(GenomeBuild genomeBuild, double defaultVariantBackgroundFrequency) {
        try (BufferedReader br = openBundledBackgroundFrequencyFile(genomeBuild)) {
            Map<TermId, Double> frequencyMap = BackgroundVariantFrequencyParser.parse(br);
            return Optional.of(BackgroundVariantFrequencyService.of(frequencyMap, defaultVariantBackgroundFrequency));
        } catch (IOException e) {
            LOGGER.warn("Cannot configure background variant frequency service for {}: {}", genomeBuild, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private static BufferedReader openBundledBackgroundFrequencyFile(GenomeBuild genomeBuild) {
        String name = switch (genomeBuild) {
            case HG19 -> "/background/background-hg19.tsv";
            case HG38 -> "/background/background-hg38.tsv";
        };
        InputStream is = BundledBackgroundVariantFrequencyServiceFactory.class.getResourceAsStream(name);
        if (is == null)
            throw new LiricalRuntimeException("Background file for " + genomeBuild + " is not present at '" + name + '\'');
        LOGGER.debug("Loading bundled background variant frequencies from {}", name);
        return new BufferedReader(new InputStreamReader(is));
    }
}
