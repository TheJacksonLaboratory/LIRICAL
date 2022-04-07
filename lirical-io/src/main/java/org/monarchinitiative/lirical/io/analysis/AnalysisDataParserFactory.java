package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisDataParser;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;

import java.util.Objects;

public class AnalysisDataParserFactory {

    private final HpoTermSanitizer sanitizer;
    private final VariantParserFactory variantParserFactory;
    private final HpoAssociationData associationData;

    public AnalysisDataParserFactory(HpoTermSanitizer sanitizer,
                                     VariantParserFactory variantParserFactory,
                                     HpoAssociationData associationData) {
        this.sanitizer = Objects.requireNonNull(sanitizer);
        this.variantParserFactory = variantParserFactory; // nullable
        this.associationData = associationData; // nullable
    }

    public AnalysisDataParser forFormat(AnalysisDataFormat format) {
        return switch (format) {
            case PHENOPACKET_v1 -> new PhenopacketV1AnalysisDataParser(sanitizer, variantParserFactory, associationData);
            case PHENOPACKET_v2 -> new PhenopacketV2AnalysisDataParser(sanitizer, variantParserFactory, associationData);
            case YAML -> new YamlAnalysisDataParser(sanitizer, variantParserFactory, associationData);
        };
    }
}
