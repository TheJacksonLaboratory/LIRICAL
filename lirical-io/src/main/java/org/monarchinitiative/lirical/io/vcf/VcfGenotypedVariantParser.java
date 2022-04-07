package org.monarchinitiative.lirical.io.vcf;

import htsjdk.variant.vcf.VCFFileReader;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.io.GenotypedVariantParser;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.util.Iterator;
import java.util.Objects;

public class VcfGenotypedVariantParser implements GenotypedVariantParser {

    private final GenomicAssembly assembly;
    private final GenomeBuild genomeBuild;
    private final VCFFileReader reader;

    public VcfGenotypedVariantParser(GenomicAssembly assembly, GenomeBuild genomeBuild, VCFFileReader reader) {
        this.assembly = Objects.requireNonNull(assembly, "Assembly must not be null");
        this.genomeBuild = Objects.requireNonNull(genomeBuild, "Genome build must not be null");
        this.reader = Objects.requireNonNull(reader, "VCF reader must not be null");
    }

    @Override
    public Iterator<GenotypedVariant> iterator() {
        return new GenotypedVariantIterator(assembly, this.genomeBuild, reader.iterator());
    }

}
