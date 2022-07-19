package org.monarchinitiative.lirical.io.analysis;

import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.lirical.core.io.AgeParseException;
import org.monarchinitiative.lirical.core.io.AgeParser;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class PhenopacketV2Importer implements PhenopacketImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketV2Importer.class);
    private static final JsonFormat.Parser PARSER = JsonFormat.parser();
    private static final PhenopacketV2Importer INSTANCE = new PhenopacketV2Importer();

    static PhenopacketV2Importer instance() {
        return INSTANCE;
    }

    @Override
    public PhenopacketData read(InputStream is) throws PhenopacketImportException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Phenopacket.Builder builder = Phenopacket.newBuilder();
        try {
            PARSER.merge(reader, builder);
        } catch (IOException e) {
            throw new PhenopacketImportException(e);
        }
        Phenopacket phenopacket = builder.build();

        // Sample ID
        Individual subject = phenopacket.getSubject();
        String sampleId = subject.getId();

        // Phenotype terms
        List<TermId> presentTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(pf -> !pf.getExcluded())
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        List<TermId> negatedHpoTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        // Age
        TimeElement timeAtLastEncounter = subject.getTimeAtLastEncounter();
        Age age = (timeAtLastEncounter.hasAge())
                ? mapToAge(timeAtLastEncounter.getAge())
                : null;

        // Sex
        org.monarchinitiative.lirical.core.model.Sex sex = toSex(subject.getSex());

        // Disease IDs
        List<TermId> diseaseIds = phenopacket.getDiseasesList().stream()
                .map(Disease::getTerm)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        // Variants
        List<GenotypedVariant> variants = List.of(); // TODO - implement real variant parsing.
        long genomicInterpretationCount = phenopacket.getInterpretationsList().stream()
                .map(Interpretation::getDiagnosis)
                .map(Diagnosis::getGenomicInterpretationsList)
                .mapToLong(Collection::size)
                .sum();
        if (genomicInterpretationCount > 0)
            LOGGER.warn("There are {} genomic interpretations in the phenopacket, but the variant parsing is currently not implemented.", genomicInterpretationCount);

        // VCF path and genome assembly
        Optional<File> firstVcf = phenopacket.getFilesList().stream()
                .filter(file -> "vcf".equalsIgnoreCase(file.getFileAttributesOrDefault("fileFormat", "")))
                .findFirst();
        Path firstVcfPath = firstVcf.flatMap(file -> SanitizingAnalysisDataParser.toUri(file.getUri()))
                .map(Path::of)
                .orElse(null);
        String genomeAssembly = firstVcf.map(f -> f.getFileAttributesOrDefault("genomeAssembly", null))
                .orElse(null);

        return new PhenopacketData(genomeAssembly,
                sampleId,
                presentTerms,
                negatedHpoTerms,
                age,
                sex,
                diseaseIds,
                variants,
                firstVcfPath);
    }

    private static Age mapToAge(org.phenopackets.schema.v2.core.Age age) {
        try {
            return AgeParser.parse(age.getIso8601Duration());
        } catch (AgeParseException e) {
            LOGGER.warn("Unable to parse age: {}", e.getMessage(), e);
            return null;
        }
    }

    private static org.monarchinitiative.lirical.core.model.Sex toSex(Sex sex) {
        return switch (sex) {
            case FEMALE -> org.monarchinitiative.lirical.core.model.Sex.FEMALE;
            case MALE -> org.monarchinitiative.lirical.core.model.Sex.MALE;
            case UNKNOWN_SEX, OTHER_SEX, UNRECOGNIZED -> org.monarchinitiative.lirical.core.model.Sex.UNKNOWN;
        };
    }
}
