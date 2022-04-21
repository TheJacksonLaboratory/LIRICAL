package org.monarchinitiative.lirical.io.analysis;

import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Period;
import java.util.List;
import java.util.Optional;

class PhenopacketV2Importer implements PhenopacketImporter {

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
                .toList();

        List<TermId> negatedHpoTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .toList();

        // Age
        TimeElement timeAtLastEncounter = subject.getTimeAtLastEncounter();
        Age age = (timeAtLastEncounter.hasAge())
                ? mapToAge(timeAtLastEncounter.getAge())
                : null;

        // Sex
        org.monarchinitiative.lirical.core.model.Sex sex = toSex(subject.getSex());

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
                firstVcfPath);
    }

    private static Age mapToAge(org.phenopackets.schema.v2.core.Age age) {
        Period iso8601 = Period.parse(age.getIso8601Duration());
        return Age.of(iso8601.getYears(), iso8601.getMonths(), iso8601.getDays());
    }

    private static org.monarchinitiative.lirical.core.model.Sex toSex(Sex sex) {
        return switch (sex) {
            case FEMALE -> org.monarchinitiative.lirical.core.model.Sex.FEMALE;
            case MALE -> org.monarchinitiative.lirical.core.model.Sex.MALE;
            case UNKNOWN_SEX, OTHER_SEX, UNRECOGNIZED -> org.monarchinitiative.lirical.core.model.Sex.UNKNOWN;
        };
    }
}
