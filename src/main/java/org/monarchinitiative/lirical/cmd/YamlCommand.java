package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.analysis.AnalysisData;
import org.monarchinitiative.lirical.cmd.yaml.YamlConfig;
import org.monarchinitiative.lirical.cmd.yaml.YamlParser;
import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.model.Age;
import org.monarchinitiative.lirical.model.Sex;
import org.monarchinitiative.lirical.model.GenesAndGenotypes;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms. This
 * analysis is driven by a YAML file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(name = "yaml",
        aliases = {"Y"},
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from YAML file")
public class YamlCommand extends AbstractPrioritizeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(YamlCommand.class);

    @CommandLine.Option(names = {"-y","--yaml"},
            required = true,
            description = "Path to YAML configuration file.")
    private Path yamlPath;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    private String genomeBuild = "hg38";

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException {
        LOGGER.info("Parsing YAML input file at {}", yamlPath);
        YamlConfig config;
        try (InputStream is = Files.newInputStream(yamlPath)) {
            config = YamlParser.parse(is);
        } catch (IOException e) {
            throw new LiricalParseException(e);
        }

        String sampleId = config.getSampleId();
        Age age = parseAge(config.age());
        Sex sex = parseSex(config.sex());
        List<TermId> presentTerms, absentTerms;
        try {
            presentTerms = config.getHpoIds().stream().map(TermId::of).toList();
            absentTerms = config.getNegatedHpoIds().stream().map(TermId::of).toList();
        } catch (RuntimeException e) {
            LOGGER.error("Error parsing phenotype terms: {}", e.getMessage(), e);
            throw new LiricalParseException(e);
        }

        GenesAndGenotypes genes;
        Optional<Path> vcfPathOptional = config.vcfPath();
        if (vcfPathOptional.isPresent() && lirical.variantParserFactory().isPresent()) {
            genes = readVariantsFromVcfFile(sampleId, vcfPathOptional.get(), lirical.variantParserFactory().get(), lirical.phenotypeService().associationData());
        } else {
            genes = GenesAndGenotypes.empty();
        }

        return AnalysisData.of(sampleId, age, sex, presentTerms, absentTerms, genes);
    }

    private static Sex parseSex(String sex) {
        return switch (sex.toLowerCase()) {
            case "male" -> Sex.MALE;
            case "female" -> Sex.FEMALE;
            default -> {
                LOGGER.info("Unknown sex {}", sex);
                yield Sex.UNKNOWN;
            }
        };
    }

}
