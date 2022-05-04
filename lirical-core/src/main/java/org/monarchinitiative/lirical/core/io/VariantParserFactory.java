package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;

public interface VariantParserFactory {

    GenomicAssembly genomicAssembly();

    default GenomeBuild genomeBuild() {
        return GenomeBuild.parse(genomicAssembly().name())
                .orElseThrow(() -> new IllegalArgumentException("Unknown genomic assembly '" + genomicAssembly().name()+  '\''));
    }

    VariantParser forPath(Path variantResource);
}
