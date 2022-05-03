package org.monarchinitiative.lirical.io.analysis;

import com.google.protobuf.util.JsonFormat;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.AlleleCount;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 *
 * @author Peter Robinson
 */
class PhenopacketV1Importer implements PhenopacketImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketV1Importer.class);
    private static final JsonFormat.Parser PARSER = JsonFormat.parser();
    private static final boolean ALWAYS_PASSING_VARIANT_FILTERS = true;

    private static final PhenopacketV1Importer INSTANCE = new PhenopacketV1Importer();

    static PhenopacketV1Importer instance() {
        return INSTANCE;
    }

    private final Map<GenomeBuild, GenomicAssembly> assemblyMap = Map.of(
            GenomeBuild.HG19, GenomicAssemblies.GRCh37p13(),
            GenomeBuild.HG38, GenomicAssemblies.GRCh38p13());

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
        String genomeBuild = firstVcf.map(HtsFile::getGenomeAssembly).orElse(null);

        // Disease IDs
        List<TermId> diseaseIds = phenopacket.getDiseasesList().stream()
                .map(Disease::getTerm)
                .map(OntologyClass::getId)
                .map(AnalysisIoUtils::createTermId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

        // Variants
        List<GenotypedVariant> variants = phenopacket.getVariantsList().stream()
                .map(toGenotypedVariant(sampleId))
                .flatMap(Optional::stream)
                .toList();

        Path vcfPath = firstVcf.flatMap(hts -> SanitizingAnalysisDataParser.toUri(hts.getUri()))
                .map(Path::of)
                .orElse(null);

        return new PhenopacketData(genomeBuild,
                sampleId,
                observedTerms,
                negatedTerms,
                age,
                sex,
                diseaseIds,
                variants,
                vcfPath);
    }

    private Function<Variant, Optional<GenotypedVariant>> toGenotypedVariant(String sampleId) {
        return v -> {
            if (v.hasVcfAllele()) {
                VcfAllele vcfAllele = v.getVcfAllele();

                // 0 - parse genome build.
                Optional<GenomeBuild> build = GenomeBuild.parse(vcfAllele.getGenomeAssembly());
                if (build.isEmpty()) {
                    LOGGER.warn("Unknown genome build {}", vcfAllele.getGenomeAssembly());
                    return Optional.empty();
                }
                GenomeBuild genomeBuild = build.get();
                GenomicAssembly assembly = assemblyMap.get(genomeBuild);
                if (assembly == null) { // In case we did not update the `assemblyMap`.
                    LOGGER.warn("We do not have genomic assembly data for genome build {}", genomeBuild);
                    return Optional.empty();
                }
                VcfConverter vcfConverter = new VcfConverter(assembly, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.retainingCommonBase()));

                // 1 - parse genome variant.
                Map<String, String> infoFields = parseVcfInfoFields(vcfAllele.getInfo());

                if (infoFields.containsKey("SVTYPE")) { // We cannot do non-sequence/non-literal variants right now.
                    // TODO - implement non-sequence variants.
                    LOGGER.warn("Parse of symbolic/breakend variants is not yet implemented!");
                    return Optional.empty();
                }
                // SEQUENCE/LITERAL
                Contig contig = vcfConverter.parseContig(vcfAllele.getChr());
                if (contig.isUnknown()) {
                    LOGGER.warn("Unknown contig {}", vcfAllele.getChr());
                    return Optional.empty();
                }

                GenomicVariant gv = vcfConverter.convert(contig, vcfAllele.getId(), vcfAllele.getPos(),
                        vcfAllele.getRef(), vcfAllele.getAlt());


                // 2 - parse genotype/zygosity.
                AlleleCount alleleCount;
                switch (v.getZygosity().getId()) {
                    case "GENO:0000134" -> alleleCount = AlleleCount.of(0, 1); // HEMIZYGOUS
                    case "GENO:0000135" -> alleleCount = AlleleCount.of(1, 1); // HETEROZYGOUS
                    case "GENO:0000136" -> alleleCount = AlleleCount.of(0, 2); // HOMOZYGOUS ALTERNATE
                    default -> {
                        LOGGER.warn("Unknown zygosity: {}", v.getZygosity().getId());
                        return Optional.empty();
                    }
                }
                Map<String, AlleleCount> alleles = Map.of(sampleId, alleleCount);

                // 3 - assemble the final variant. Uff..
                return Optional.of(GenotypedVariant.of(genomeBuild,
                        gv,
                        alleles,
                        ALWAYS_PASSING_VARIANT_FILTERS));
            } else {
                LOGGER.warn("Skipping non-VCF allele {}: {}", v.getAlleleCase(), v);
                return Optional.empty();
            }
        };
    }

    private static Map<String, String> parseVcfInfoFields(String info) {
        if (info.isEmpty()) {
            return Map.of();
        } else {
            return Arrays.stream(info.split(";"))
                        .map(p -> p.split("="))
                        .collect(Collectors.toMap(f -> f[0], f -> f.length == 2 ? f[1] : ""));
        }
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
