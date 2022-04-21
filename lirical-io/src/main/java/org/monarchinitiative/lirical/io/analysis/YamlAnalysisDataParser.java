package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

class YamlAnalysisDataParser extends SanitizingAnalysisDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlAnalysisDataParser.class);

    YamlAnalysisDataParser(HpoTermSanitizer sanitizer,
                           VariantParserFactory variantParserFactory,
                           HpoAssociationData associationData) {
        super(sanitizer, variantParserFactory, associationData);
    }

    @Override
    public AnalysisData parse(InputStream is) throws LiricalParseException {
        YamlConfig config;
        try {
            config = YamlParser.parse(is);
        } catch (IOException e) {
            throw new LiricalParseException(e);
        }

        String sampleId = config.getSampleId();
        Age age = parseAge(config.age());
        Sex sex = parseSex(config.sex());

        List<TermId> presentTerms = config.getHpoIds().stream()
                .map(this::toTermId)
                .flatMap(Optional::stream)
                .toList();
        List<TermId> absentTerms = config.getNegatedHpoIds().stream()
                .map(this::toTermId)
                .flatMap(Optional::stream)
                .toList();

        GenesAndGenotypes genes = parseGeneToGenotype(sampleId, config.vcfPath().orElse(null));

        return AnalysisData.of(sampleId, age, sex, presentTerms, absentTerms, genes);
    }


    private static Sex parseSex(String sex) {
        return switch (sex.toLowerCase()) {
            case "male" -> Sex.MALE;
            case "female" -> Sex.FEMALE;
            default -> {
                LOGGER.warn("Unknown sex {}", sex);
                yield Sex.UNKNOWN;
            }
        };
    }
}
