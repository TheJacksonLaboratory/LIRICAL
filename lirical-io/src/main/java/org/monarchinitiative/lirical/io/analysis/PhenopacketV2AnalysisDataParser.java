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
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Individual;
import org.phenopackets.schema.v2.core.OntologyClass;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.phenopackets.schema.v2.core.TimeElement;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

class PhenopacketV2AnalysisDataParser extends BasePhenopacketAnalysisDataParser<Phenopacket.Builder> {

    PhenopacketV2AnalysisDataParser(HpoTermSanitizer sanitizer,
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
        Individual subject = phenopacket.getSubject();
        String sampleId = subject.getId();

        List<TermId> present = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(pf -> !pf.getExcluded())
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(this::toTermId)
                .flatMap(Optional::stream)
                .toList();

        List<TermId> excluded = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(this::toTermId)
                .flatMap(Optional::stream)
                .toList();

        TimeElement timeAtLastEncounter = subject.getTimeAtLastEncounter();
        Age age = timeAtLastEncounter.hasAge()
                ? Optional.of(timeAtLastEncounter.getAge())
                .filter(a -> !a.equals(org.phenopackets.schema.v2.core.Age.getDefaultInstance()))
                .map(org.phenopackets.schema.v2.core.Age::getIso8601Duration)
                .map(BaseAnalysisDataParser::parseAge)
                .orElse(Age.ageNotKnown())
                : Age.ageNotKnown();

        Sex sex = Optional.of(subject.getSex())
                .map(PhenopacketV2AnalysisDataParser::toSex)
                .orElse(Sex.UNKNOWN);

        Path vcfPath = phenopacket.getFilesList().stream()
                .filter(file -> "vcf".equalsIgnoreCase(file.getFileAttributesOrDefault("fileFormat", "")))
                .findFirst()
                .flatMap(file -> SanitizingAnalysisDataParser.toUri(file.getUri()))
                .map(Path::of)
                .orElse(null);
        GenesAndGenotypes genes = parseGeneToGenotype(sampleId, vcfPath);

        return AnalysisData.of(sampleId, age, sex, present, excluded, genes);
    }

    private static Sex toSex(org.phenopackets.schema.v2.core.Sex sex) {
        return switch (sex) {
            case MALE -> Sex.MALE;
            case FEMALE -> Sex.FEMALE;
            case UNKNOWN_SEX, OTHER_SEX, UNRECOGNIZED -> Sex.UNKNOWN;
        };
    }
}
