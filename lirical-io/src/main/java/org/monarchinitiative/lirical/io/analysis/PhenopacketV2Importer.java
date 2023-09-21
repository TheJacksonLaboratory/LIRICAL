package org.monarchinitiative.lirical.io.analysis;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

class PhenopacketV2Importer implements PhenopacketImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketV2Importer.class);
    private static final PhenopacketV2Importer INSTANCE = new PhenopacketV2Importer();

    static PhenopacketV2Importer instance() {
        return INSTANCE;
    }

    @Override
    public PhenopacketData read(InputStream is) throws PhenopacketImportException {
        Phenopacket phenopacket = PhenopacketImportUtil.readPhenopacket(is, Phenopacket.class);

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
        Age age = parseAge(subject.getTimeAtLastEncounter(), subject.getId());

        // Sex
        org.monarchinitiative.lirical.core.model.Sex sex = toSex(subject.getSex());

        // Disease IDs
        TermId diseaseIds = getDisease(phenopacket);

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

    private static Sex toSex(org.phenopackets.schema.v2.core.Sex sex) {
        return switch (sex) {
            case FEMALE -> Sex.FEMALE;
            case MALE -> Sex.MALE;
            case UNKNOWN_SEX, OTHER_SEX, UNRECOGNIZED -> Sex.UNKNOWN;
        };
    }

    private static Age parseAge(TimeElement timeAtLastEncounter, String subjectId) {
        return switch (timeAtLastEncounter.getElementCase()) {
            case GESTATIONAL_AGE -> {
                GestationalAge ga = timeAtLastEncounter.getGestationalAge();
                LOGGER.debug("Parsing gestational age {}w {}d of subject {}", ga.getWeeks(), ga.getDays(), subjectId);
                yield Age.gestationalAge(ga.getWeeks(), ga.getDays());
            }
            case AGE -> {
                org.phenopackets.schema.v2.core.Age a = timeAtLastEncounter.getAge();
                LOGGER.debug("Parsing age {} of subject {}", a.getIso8601Duration(), subjectId);
                yield Age.parse(Period.parse(a.getIso8601Duration()));
            }
            case AGE_RANGE, ONTOLOGY_CLASS, TIMESTAMP, INTERVAL -> {
                LOGGER.warn("Ignoring unsupported age format {} for subject {}", timeAtLastEncounter.getElementCase(), subjectId);
                yield Age.ageNotKnown();
            }
            case ELEMENT_NOT_SET -> {
                LOGGER.warn("Time at last encounter was not set for subject {}", subjectId);
                yield Age.ageNotKnown();
            }
        };
    }

    private TermId getDisease(Phenopacket phenopacket) throws PhenopacketImportException {
        List<Interpretation> interpretationsList = phenopacket.getInterpretationsList();
        if (interpretationsList.size() == 1) {
            Interpretation interp = interpretationsList.get(0);
            if (interp.hasDiagnosis()) {
                return TermId.of(interp.getDiagnosis().getDisease().getId());
            }
        }
        List<TermId> diseaseIds = phenopacket.getDiseasesList().stream()
                .map(Disease::getTerm)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        if (diseaseIds.size() == 1) {
            return diseaseIds.get(0);
        }
        throw new PhenopacketImportException("Invalid Number of Diseases.");
    }
}
