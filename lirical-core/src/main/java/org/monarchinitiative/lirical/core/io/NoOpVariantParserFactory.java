package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.nio.file.Path;
import java.util.Optional;

class NoOpVariantParserFactory implements VariantParserFactory {

    private static final NoOpVariantParserFactory INSTANCE = new NoOpVariantParserFactory();

    static NoOpVariantParserFactory getInstance() {
        return INSTANCE;
    }

    private NoOpVariantParserFactory() {
    }

    @Override
    public Optional<VariantParser> forPath(Path variantResource, GenomeBuild genomeBuild, TranscriptDatabase transcriptDatabase) {
        return Optional.empty();
    }

}
