package org.monarchinitiative.lirical.io.vcf;

import htsjdk.variant.vcf.VCFFileReader;
import org.monarchinitiative.lirical.io.GenotypedVariantParser;
import org.monarchinitiative.lirical.model.GenotypedVariant;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.util.Iterator;
import java.util.Objects;

public class VcfGenotypedVariantParser implements GenotypedVariantParser {

    private final GenomicAssembly assembly;
    private final VCFFileReader reader;

    public VcfGenotypedVariantParser(GenomicAssembly assembly, VCFFileReader reader) {
        this.assembly = Objects.requireNonNull(assembly, "Assembly must not be null");
        this.reader = Objects.requireNonNull(reader, "VCF reader must not be null");
    }

    @Override
    public Iterator<GenotypedVariant> iterator() {
        return new GenotypedVariantIterator(assembly, reader.iterator());
    }

}
