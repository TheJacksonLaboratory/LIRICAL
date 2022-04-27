package org.monarchinitiative.lirical.beta.cmd;

import org.monarchinitiative.lirical.core.model.ClinvarClnSig;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.core.service.VariantPathogenicity;
import org.monarchinitiative.lirical.core.service.VariantPathogenicityService;
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
import java.util.Optional;

public class SquirlsAwarePathogenicityService implements VariantPathogenicityService {

    private final VariantSplicingEvaluator evaluator;
    private final VariantPathogenicityService variantPathogenicityService;
    private final float pathogenicityThreshold;
    private final boolean capDeleterious;

    public static SquirlsAwarePathogenicityService of(VariantPathogenicityService variantPathogenicityService,
                                                      Path squirlsDataDirectory,
                                                      TranscriptDatabase transcriptDatabase,
                                                      float pathogenicityThreshold,
                                                      boolean capPathogenic) throws LiricalDataException {
        SimpleSquirlsProperties squirlsProperties = SimpleSquirlsProperties.builder().build();
        SquirlsOptions options = SquirlsOptions.of(mapToFeatureSource(transcriptDatabase));
        try {
            SquirlsConfigurationFactory factory = SquirlsConfigurationFactory.of(squirlsDataDirectory, squirlsProperties, options);
            return new SquirlsAwarePathogenicityService(factory.getSquirls(), variantPathogenicityService, pathogenicityThreshold, capPathogenic);
        } catch (SquirlsResourceException e) {
            throw new LiricalDataException(e);
        }
    }

    public SquirlsAwarePathogenicityService(Squirls squirls,
                                            VariantPathogenicityService variantPathogenicityService,
                                            float pathogenicityThreshold,
                                            boolean capDeleterious) {
        this.evaluator = squirls.variantSplicingEvaluator();
        this.variantPathogenicityService = variantPathogenicityService;
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
    public Optional<VariantPathogenicity> getPathogenicity(GenomicVariant variant) {

        Optional<VariantPathogenicity> backgroundPathogenicity = variantPathogenicityService.getPathogenicity(variant);
        SquirlsResult squirlsResult = evaluator.evaluate(variant);
        if (backgroundPathogenicity.isEmpty() && squirlsResult.isEmpty()) {
            return Optional.empty();
        }

        if (squirlsResult.isEmpty())
            // We have only the background pathogenicity
            return backgroundPathogenicity;

        float pathogenicity;
        ClinvarClnSig clinvarClnSig;
        float squirlsScore;
        if (capDeleterious) {
            // Squirls is not calibrated, so we cap the pathogenicity score to ensure we find the splice variants.
            squirlsScore = squirlsResult.isPathogenic()
                    ? Math.max((float) squirlsResult.maxPathogenicity(), pathogenicityThreshold)
                    : (float) squirlsResult.maxPathogenicity();
        } else {
            squirlsScore = (float) squirlsResult.maxPathogenicity();
        }

        if (backgroundPathogenicity.isEmpty()) {
            // We have only Squirls result
            pathogenicity = squirlsScore;
            clinvarClnSig = ClinvarClnSig.NOT_PROVIDED;
        } else {
            // We have both Squirls result and the background pathogenicity
            VariantPathogenicity vp = backgroundPathogenicity.get();
            pathogenicity = Math.max(squirlsScore, vp.pathogenicity());
            clinvarClnSig = vp.clinvarClnSig().orElse(ClinvarClnSig.NOT_PROVIDED);
        }

        return Optional.of(VariantPathogenicity.of(pathogenicity, clinvarClnSig));
    }

}
