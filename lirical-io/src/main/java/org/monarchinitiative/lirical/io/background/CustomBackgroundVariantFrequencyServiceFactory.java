package org.monarchinitiative.lirical.io.background;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyService;
import org.monarchinitiative.lirical.core.service.BackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link BackgroundVariantFrequencyServiceFactory} implementation that uses user-provided frequency files.
 */
public class CustomBackgroundVariantFrequencyServiceFactory implements BackgroundVariantFrequencyServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomBackgroundVariantFrequencyServiceFactory.class);

    private final Map<GenomeBuild, Path> backgroundFilePaths;

    public static CustomBackgroundVariantFrequencyServiceFactory of(Map<GenomeBuild, Path> backgroundFilePaths) {
        return new CustomBackgroundVariantFrequencyServiceFactory(backgroundFilePaths);
    }

    private CustomBackgroundVariantFrequencyServiceFactory(Map<GenomeBuild, Path> backgroundFilePaths) {
        this.backgroundFilePaths = Objects.requireNonNull(backgroundFilePaths);
    }

    @Override
    public Optional<BackgroundVariantFrequencyService> forGenomeBuild(GenomeBuild genomeBuild, double defaultVariantBackgroundFrequency) {
        Path backgroundFile = backgroundFilePaths.get(genomeBuild);
        if (backgroundFile == null) {
            return Optional.empty();
        } else {
            try (BufferedReader reader = Files.newBufferedReader(backgroundFile)) {
                Map<TermId, Double> frequencyMap = BackgroundVariantFrequencyParser.parse(reader);
                return Optional.of(BackgroundVariantFrequencyService.of(frequencyMap, defaultVariantBackgroundFrequency));
            } catch (IOException e) {
                LOGGER.warn("Unable to read background frequency file at {}", backgroundFile.toAbsolutePath());
                return Optional.empty();
            }
        }
    }

}
