package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.PhenopacketImporter;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.Age;
import org.phenopackets.schema.v1.core.Sex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Period;
import java.util.*;

/**
 * Run LIRICAL from a Phenopacket -- with or without accompanying VCF file.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter N Robinson</a>
 */

@CommandLine.Command(name = "phenopacket",
        aliases = {"P"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from a Phenopacket")
public class PhenopacketCommand extends AbstractPrioritizeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketCommand.class);

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            required = true,
            description = "path to phenopacket file")
    protected Path phenopacketPath;

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException {
        // Read the Phenopacket
        PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath);

        // Parse & sanitize HPO terms
        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> observedTerms = importer.getHpoTerms().stream()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();
        List<TermId> negatedTerms = importer.getNegatedHpoTerms().stream()
                .map(sanitizer::replaceIfObsolete)
                .flatMap(Optional::stream)
                .toList();

        // Parse sample attributes
        String sampleId = importer.getSampleId();
        org.monarchinitiative.lirical.model.Age age = importer.getAge().filter(a -> Age.getDefaultInstance().equals(a))
                .map(Age::getAge)
                .map(Period::parse)
                .map(org.monarchinitiative.lirical.model.Age::parse)
                .orElse(org.monarchinitiative.lirical.model.Age.ageNotKnown());

        org.monarchinitiative.lirical.model.Sex sex = importer.getSex()
                .map(this::toSex)
                .orElse(org.monarchinitiative.lirical.model.Sex.UNKNOWN);

        // Go through VCF file (if present)
        GenesAndGenotypes genes;
        Optional<Path> vcfPathOpt = importer.getVcfPath();
        if (vcfPathOpt.isEmpty() || lirical.variantParserFactory().isEmpty()) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcfPathOpt.get(), lirical.variantParserFactory().get(), lirical.phenotypeService().associationData());
        }

        return AnalysisData.of(sampleId, age, sex, observedTerms, negatedTerms, genes);
    }

    private org.monarchinitiative.lirical.model.Sex toSex(Sex sex) {
        return switch (sex) {
            case MALE -> org.monarchinitiative.lirical.model.Sex.MALE;
            case FEMALE -> org.monarchinitiative.lirical.model.Sex.FEMALE;
            case OTHER_SEX, UNKNOWN_SEX, UNRECOGNIZED -> org.monarchinitiative.lirical.model.Sex.UNKNOWN;
        };
    }

}
