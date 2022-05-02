package org.monarchinitiative.lirical.beta.cmd;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import org.monarchinitiative.lirical.core.model.VariantMetadata;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.squirls.bootstrap.SimpleSquirlsProperties;
import org.monarchinitiative.squirls.bootstrap.SquirlsConfigurationFactory;
import org.monarchinitiative.squirls.core.Squirls;
import org.monarchinitiative.squirls.core.SquirlsResult;
import org.monarchinitiative.squirls.core.VariantSplicingEvaluator;
import org.monarchinitiative.squirls.core.config.FeatureSource;
import org.monarchinitiative.squirls.core.config.SquirlsOptions;
import org.monarchinitiative.squirls.io.SquirlsResourceException;
import org.monarchinitiative.svart.GenomicVariant;

import java.nio.file.Path;
import java.util.List;

public class SquirlsAwareVariantMetadataService implements VariantMetadataService {

    private final VariantSplicingEvaluator evaluator;
    private final VariantMetadataService variantMetadataService;
    private final float pathogenicityThreshold;
    private final boolean capDeleterious;

    public static SquirlsAwareVariantMetadataService of(VariantMetadataService variantMetadataService,
                                                        Path squirlsDataDirectory,
                                                        TranscriptDatabase transcriptDatabase,
                                                        float pathogenicityThreshold,
                                                        boolean capPathogenic) throws LiricalDataException {
        SimpleSquirlsProperties squirlsProperties = SimpleSquirlsProperties.builder().build();
        SquirlsOptions options = SquirlsOptions.of(mapToFeatureSource(transcriptDatabase));
        try {
            SquirlsConfigurationFactory factory = SquirlsConfigurationFactory.of(squirlsDataDirectory, squirlsProperties, options);
            return new SquirlsAwareVariantMetadataService(factory.getSquirls(), variantMetadataService, pathogenicityThreshold, capPathogenic);
        } catch (SquirlsResourceException e) {
            throw new LiricalDataException(e);
        }
    }

    private SquirlsAwareVariantMetadataService(Squirls squirls,
                                              VariantMetadataService variantMetadataService,
                                              float pathogenicityThreshold,
                                              boolean capDeleterious) {
        this.evaluator = squirls.variantSplicingEvaluator();
        this.variantMetadataService = variantMetadataService;
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.capDeleterious = capDeleterious;
    }

    private static FeatureSource mapToFeatureSource(TranscriptDatabase transcriptDatabase) {
        return switch (transcriptDatabase) {
            case REFSEQ -> FeatureSource.REFSEQ;
            case UCSC -> FeatureSource.UCSC;
        };
    }

    @Override
    public VariantMetadata metadata(GenomicVariant variant, List<VariantEffect> effects) {
        VariantMetadata metadata = variantMetadataService.metadata(variant, effects);
        SquirlsResult squirlsResult = evaluator.evaluate(variant);

        if (squirlsResult.isEmpty())
            // No SQUIRLS scores.
            return metadata;

        // We have both Squirls result and the background pathogenicity.
        float squirlsScore;
        if (capDeleterious) {
            // Squirls is not calibrated, so we cap the pathogenicity score to ensure we find the splice variants.
            squirlsScore = squirlsResult.isPathogenic()
                    ? Math.max((float) squirlsResult.maxPathogenicity(), pathogenicityThreshold)
                    : (float) squirlsResult.maxPathogenicity();
        } else {
            squirlsScore = (float) squirlsResult.maxPathogenicity();
        }

        float pathogenicity = Math.max(squirlsScore, metadata.pathogenicity());

        return VariantMetadata.of(metadata.frequency().orElse(0F),
                pathogenicity,
                metadata.clinvarClnSig());
    }

}
