package org.monarchinitiative.lirical.beta.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
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

@CommandLine.Command(name = "phenopacket-squirls",
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Prioritize diseases for phenopacket using Exomiser and Squirls")
public class PhenopacketCommand extends BaseSquirlsAwareCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketCommand.class);

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            required = true,
            description = "Path to phenopacket JSON file.")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF file. This path has priority over any VCF files described in phenopacket.")
    protected Path vcfPath;

    @Override
    protected int checkInput() {
        return 0;
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
        if (vcfPath == null) {
            genes = GenesAndGenotypes.empty();
        } else {
            genes = readVariantsFromVcfFile(sampleId, vcfPath, lirical.variantParserFactory().orElse(null));
        }
        return AnalysisData.of(sampleId,
                data.getAge().orElse(null),
                data.getSex().orElse(null),
                presentTerms,
                excludedTerms,
                genes);
    }

}
