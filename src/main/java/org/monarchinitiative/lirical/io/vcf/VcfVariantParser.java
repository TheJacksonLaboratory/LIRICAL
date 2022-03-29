package org.monarchinitiative.lirical.io.vcf;

import org.monarchinitiative.lirical.io.GenotypedVariantParser;
import org.monarchinitiative.lirical.io.VariantParser;
import org.monarchinitiative.lirical.model.LiricalVariant;
import org.monarchinitiative.lirical.service.VariantMetadataService;

import java.util.Iterator;
import java.util.Objects;

// TODO - improve API
public class VcfVariantParser implements VariantParser {

    private final GenotypedVariantParser genotypedVariantParser;
    private final VariantMetadataService metadataService;

    public VcfVariantParser(GenotypedVariantParser genotypedVariantParser, VariantMetadataService metadataService) {
        this.genotypedVariantParser = Objects.requireNonNull(genotypedVariantParser);
        this.metadataService = Objects.requireNonNull(metadataService);
    }

    @Override
    public Iterator<LiricalVariant> iterator() {
        return new LiricalVariantIterator(genotypedVariantParser.iterator(), metadataService);
    }


}
