package org.monarchinitiative.lirical.cli.cmd;

import com.google.protobuf.InvalidProtocolBufferException;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisDataParser;
import org.monarchinitiative.lirical.core.exception.LiricalParseException;
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
 * Run LIRICAL from a Phenopacket -- with or without accompanying VCF file.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter N Robinson</a>
 */

@CommandLine.Command(name = "phenopacket",
        aliases = {"P"},
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run LIRICAL from a Phenopacket")
public class PhenopacketCommand extends AnalysisDataParserAwareCommand {

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
    protected AnalysisData prepareAnalysisData(AnalysisDataParserFactory factory) throws LiricalParseException {
        LOGGER.info("Reading phenopacket from {}", phenopacketPath.toAbsolutePath());
        AnalysisData data = null;

        // Try v2 first
        LOGGER.debug("Trying to decode using Phenopacket v2 format");
        try (InputStream is = Files.newInputStream(phenopacketPath)) {
            AnalysisDataParser parser = factory.forFormat(AnalysisDataFormat.PHENOPACKET_v2);
            data = parser.parse(is);
            LOGGER.debug("Success!");
        } catch (IOException e) {
            if (!(e instanceof InvalidProtocolBufferException))
                throw new LiricalParseException(e);
        }

        // Try v1 if v2 failed
        if (data == null) {
            LOGGER.debug("That did not work. Trying to decode using Phenopacket v1 format");
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
                AnalysisDataParser parser = factory.forFormat(AnalysisDataFormat.PHENOPACKET_v1);
                data = parser.parse(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("That failed too");
                throw new LiricalParseException(e);
            }
        }

        return data;
    }

}
