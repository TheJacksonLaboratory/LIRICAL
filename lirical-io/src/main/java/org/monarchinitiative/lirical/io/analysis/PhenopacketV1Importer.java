package org.monarchinitiative.lirical.io.analysis;

import com.google.protobuf.util.JsonFormat;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;


/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 *
 * @author Peter Robinson
 */
class PhenopacketV1Importer implements PhenopacketImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketV1Importer.class);
    private static final JsonFormat.Parser PARSER = JsonFormat.parser();

    private static final PhenopacketV1Importer INSTANCE = new PhenopacketV1Importer();

    static PhenopacketV1Importer instance() {
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

        Individual subject = phenopacket.getSubject();
        String sampleId = subject.getId();
        List<TermId> observedTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(pf -> !pf.getNegated())
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        List<TermId> negatedTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getNegated)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();


        org.monarchinitiative.lirical.core.model.Age age = subject.getAgeCase().equals(Individual.AgeCase.AGE_AT_COLLECTION)
                ? mapToAge(subject.getAgeAtCollection())
                : null;

        org.monarchinitiative.lirical.core.model.Sex sex = toSex(subject.getSex());


        Optional<HtsFile> firstVcf = phenopacket.getHtsFilesList().stream()
                .filter(f -> f.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .findFirst();
        String genomeAssembly = firstVcf.map(HtsFile::getGenomeAssembly).orElse(null);

        Path vcfPath = firstVcf.flatMap(hts -> SanitizingAnalysisDataParser.toUri(hts.getUri()))
                .map(Path::of)
                .orElse(null);
        return new PhenopacketData(genomeAssembly, sampleId, observedTerms, negatedTerms, age, sex, vcfPath);
    }

    private static Age mapToAge(org.phenopackets.schema.v1.core.Age age) {
        try {
            Period iso8601 = Period.parse(age.getAge());
            return Age.of(iso8601.getYears(), iso8601.getMonths(), iso8601.getDays());
        } catch (DateTimeParseException e) {
            LOGGER.warn("Ignoring unparasble age {}", age.getAge());
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
