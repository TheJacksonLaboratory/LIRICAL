package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.nio.file.Path;
import java.util.Optional;

public interface VariantParserFactory {

    /**
     * Get a factory that can be used when LIRICAL seems to be configured for phenotype only analyses.
     *
     * @return a {@link VariantParserFactory} that never provides a {@link VariantParser}.
     */
    static VariantParserFactory noOpFactory() {
        return NoOpVariantParserFactory.getInstance();
    }

    Optional<VariantParser> forPath(Path variantResource, GenomeBuild genomeBuild, TranscriptDatabase transcriptDatabase);

}
