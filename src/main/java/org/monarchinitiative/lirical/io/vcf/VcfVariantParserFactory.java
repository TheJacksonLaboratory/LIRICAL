package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.lirical.service.VariantMetadataService;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.util.Objects;

public class VcfVariantParserFactory implements VariantParserFactory {

    private final GenomicAssembly genomicAssembly;
    private final VariantMetadataService metadataService;

    public static VcfVariantParserFactory of(GenomicAssembly genomicAssembly,
                                      VariantMetadataService metadataService) {
        return new VcfVariantParserFactory(genomicAssembly, metadataService);
    }

    public VcfVariantParserFactory(GenomicAssembly genomicAssembly,
                                   VariantMetadataService metadataService) {
        this.genomicAssembly = Objects.requireNonNull(genomicAssembly);
        this.metadataService = Objects.requireNonNull(metadataService);
    }

    @Override
    public GenomicAssembly genomicAssembly() {
        return genomicAssembly;
    }

    @Override
    public VariantParser forPath(Path path) {
        return new VcfVariantParser(path, genomicAssembly, genomeBuild(), metadataService);
    }
}
