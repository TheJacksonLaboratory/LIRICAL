package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.*;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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
            description = "Path to phenopacket JSON file.")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF file. This path has priority over any VCF files described in phenopacket.")
    protected Path vcfPath;

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException {
        LOGGER.info("Reading phenopacket from {}.", phenopacketPath.toAbsolutePath());

        PhenopacketData data = null;
        try (InputStream is = Files.newInputStream(phenopacketPath)) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.info("Success!");
        } catch (IOException e) {
            LOGGER.info("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = Files.newInputStream(phenopacketPath)) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
            } catch (IOException e) {
                LOGGER.info("Unable to parser as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        List<TermId> presentTerms = data.getHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();
        List<TermId> excludedTerms = data.getNegatedHpoTerms().map(sanitizer::replaceIfObsolete).flatMap(Optional::stream).toList();

        // Read VCF file.
        GenesAndGenotypes genes;
        // Path to VCF set via CLI has priority.
        Path vcfPath = this.vcfPath != null
                ? this.vcfPath
                : data.getVcfPath().orElse(null);
        String sampleId = data.getSampleId();
        if (vcfPath == null || lirical.variantParserFactory().isEmpty()) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory().get(), lirical.phenotypeService().associationData());
        }
        return AnalysisData.of(sampleId,
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                presentTerms,
                excludedTerms,
                genes);
    }

}
