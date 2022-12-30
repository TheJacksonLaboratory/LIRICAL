package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.nio.file.Path;
import java.util.Optional;

public interface VariantParserFactory {

    Optional<VariantParser> forPath(Path variantResource, GenomeBuild genomeBuild, TranscriptDatabase transcriptDatabase);

    @Deprecated(forRemoval = true)
    default VariantParser forPath(Path variantResource) {
        return forPath(variantResource, GenomeBuild.HG38, TranscriptDatabase.REFSEQ).orElse(null);
    }

}
