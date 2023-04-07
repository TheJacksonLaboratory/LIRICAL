package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.util.Optional;

/**
 * {@linkplain FunctionalVariantAnnotatorService} knows about {@link FunctionalVariantAnnotator}s for all genome build
 * and transcript database combinations that have been configured for a given
 * {@link org.monarchinitiative.lirical.core.Lirical} instance.
 */
public interface FunctionalVariantAnnotatorService {

    /**
     * Get {@link FunctionalVariantAnnotator} for a combination of {@link GenomeBuild} and {@link TranscriptDatabase}.
     * <p>
     * The returned value is empty if LIRICAL resources do not allow configuring the annotator for given combination.
     */
    Optional<FunctionalVariantAnnotator> getFunctionalAnnotator(GenomeBuild genomeBuild,
                                                                TranscriptDatabase transcriptDatabase);

}
