package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.exception.LiricalParseException;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

class PhenopacketV1AnalysisDataParser extends BasePhenopacketAnalysisDataParser<Phenopacket.Builder> {

    PhenopacketV1AnalysisDataParser(HpoTermSanitizer sanitizer,
                                    VariantParserFactory variantParserFactory,
                                    HpoAssociationData associationData) {
        super(sanitizer, variantParserFactory, associationData);
    }

    @Override
    protected Phenopacket.Builder getBuilder() {
        return Phenopacket.newBuilder();
    }

    @Override
    protected AnalysisData mapToAnalysisData(Phenopacket.Builder builder) throws LiricalParseException {
        Phenopacket phenopacket = builder.build();
        PhenopacketImporter importer = PhenopacketImporter.of(phenopacket);
        String sampleId = importer.getSampleId();

        List<TermId> present = importer.getHpoTerms()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        List<TermId> excluded = importer.getNegatedHpoTerms()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        Age age = importer.getAge().filter(a -> !org.phenopackets.schema.v1.core.Age.getDefaultInstance().equals(a))
                .map(org.phenopackets.schema.v1.core.Age::getAge)
                .map(BaseAnalysisDataParser::parseAge)
                .orElse(Age.ageNotKnown());

        Sex sex = importer.getSex()
                .map(PhenopacketV1AnalysisDataParser::toSex)
                .orElse(Sex.UNKNOWN);

        Path vcfPath = phenopacket.getHtsFilesList().stream()
                .filter(hts -> hts.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .findFirst()
                .flatMap(hts -> SanitizingAnalysisDataParser.toUri(hts.getUri()))
                .map(Path::of)
                .orElse(null);

        GenesAndGenotypes genes = parseGeneToGenotype(sampleId, vcfPath);

        return AnalysisData.of(sampleId, age, sex, present, excluded, genes);
    }

    private static org.monarchinitiative.lirical.core.model.Sex toSex(org.phenopackets.schema.v1.core.Sex sex) {
        return switch (sex) {
            case MALE -> org.monarchinitiative.lirical.core.model.Sex.MALE;
            case FEMALE -> org.monarchinitiative.lirical.core.model.Sex.FEMALE;
            case OTHER_SEX, UNKNOWN_SEX, UNRECOGNIZED -> org.monarchinitiative.lirical.core.model.Sex.UNKNOWN;
        };
    }
}
