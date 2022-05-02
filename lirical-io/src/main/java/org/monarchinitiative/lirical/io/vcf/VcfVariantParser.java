package org.monarchinitiative.lirical.io.vcf;

import htsjdk.variant.vcf.VCFFileReader;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.service.FunctionalVariantAnnotator;
import org.monarchinitiative.lirical.core.service.VariantMetadataService;
import org.monarchinitiative.lirical.io.GenotypedVariantParser;
import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

class VcfVariantParser implements VariantParser {

    private final VCFFileReader reader;
    private final GenotypedVariantParser parser;
    private final FunctionalVariantAnnotator variantAnnotator;
    private final VariantMetadataService metadataService;

    VcfVariantParser(Path path,
                     GenomicAssembly genomicAssembly,
                     GenomeBuild genomeBuild,
                     FunctionalVariantAnnotator variantAnnotator,
                     VariantMetadataService metadataService) {
        this.reader = new VCFFileReader(Objects.requireNonNull(path), false);;
        this.parser = new VcfGenotypedVariantParser(genomicAssembly, genomeBuild, reader);
        this.variantAnnotator = Objects.requireNonNull(variantAnnotator, "Variant annotator must not be null!");
        this.metadataService = Objects.requireNonNull(metadataService, "Variant metadata service must not be null!");
    }

    @Override
    public Iterator<LiricalVariant> iterator() {
        return new LiricalVariantIterator(parser.iterator(), variantAnnotator, metadataService);
    }

    @Override
    public List<String> sampleNames() {
        return reader.getFileHeader().getSampleNamesInOrder();
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
