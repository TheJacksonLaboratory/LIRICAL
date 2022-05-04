package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.core.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class PhenopacketAnalysisDataParser extends SanitizingAnalysisDataParser {

    private final PhenopacketImporter importer;
    protected PhenopacketAnalysisDataParser(HpoTermSanitizer sanitizer,
                                            VariantParserFactory variantParserFactory,
                                            HpoAssociationData associationData, PhenopacketImporter importer) {
        super(sanitizer, variantParserFactory, associationData);
        this.importer = Objects.requireNonNull(importer);
    }


    @Override
    public AnalysisData parse(InputStream is) throws LiricalParseException {
        PhenopacketData data = importer.read(is);

        GenesAndGenotypes genes = parseGeneToGenotype(data.getSampleId(), data.getVcfPath().orElse(null));

        return AnalysisData.of(data.getSampleId(),
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                data.getHpoTerms().map(this::sanitize).flatMap(Optional::stream).collect(Collectors.toList()),
                data.getNegatedHpoTerms().map(this::sanitize).flatMap(Optional::stream).collect(Collectors.toList()),
                genes);
    }

}
