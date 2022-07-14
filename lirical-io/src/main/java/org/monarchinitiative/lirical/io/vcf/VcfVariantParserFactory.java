package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.util.Objects;

public class VcfVariantParserFactory implements VariantParserFactory {

    private final GenomicAssembly genomicAssembly;
    private final FunctionalVariantAnnotator variantAnnotator;
    private final VariantMetadataService metadataService;

    public static VcfVariantParserFactory of(GenomicAssembly genomicAssembly,
                                             FunctionalVariantAnnotator variantAnnotator,
                                             VariantMetadataService metadataService) {
        return new VcfVariantParserFactory(genomicAssembly, variantAnnotator, metadataService);
    }

    private VcfVariantParserFactory(GenomicAssembly genomicAssembly,
                                   FunctionalVariantAnnotator variantAnnotator,
                                   VariantMetadataService metadataService) {
        this.genomicAssembly = Objects.requireNonNull(genomicAssembly);
        this.variantAnnotator = Objects.requireNonNull(variantAnnotator, "Variant annotator must not be null!");
        this.metadataService = Objects.requireNonNull(metadataService);
    }

    @Override
    public GenomicAssembly genomicAssembly() {
        return genomicAssembly;
    }

    @Override
    public VariantParser forPath(Path path) {
        return new VcfVariantParser(path, genomicAssembly, genomeBuild(), variantAnnotator, metadataService);
    }
}
