package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisDataParser;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.io.analysis.AnalysisDataFormat;
import org.monarchinitiative.lirical.io.analysis.AnalysisDataParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class coordinates the main analysis of a VCF file plus list of observed HPO terms. This
 * analysis is driven by a YAML file.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@CommandLine.Command(name = "yaml",
        aliases = {"Y"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from YAML file")
public class YamlCommand extends AnalysisDataParserAwareCommand {
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
    protected AnalysisData prepareAnalysisData(AnalysisDataParserFactory factory) throws LiricalParseException {
        AnalysisDataParser parser = factory.forFormat(AnalysisDataFormat.YAML);

        LOGGER.info("Parsing YAML input file at {}", yamlPath);
        try (InputStream is = Files.newInputStream(yamlPath)) {
            return parser.parse(is);
        } catch (IOException e) {
            throw new LiricalParseException(e);
        }
    }
}
