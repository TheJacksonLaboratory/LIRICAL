package org.monarchinitiative.lirical.cli.cmd;

import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.core.service.TranscriptDatabase;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.likelihoodratio.backgroundfrequency.GenicIntoleranceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * This command is used to generate the background frequency files. It is not needed to run LIRICAL on exome/genome
 * data, but may be interesting for those who desire to use a population frequency data source other than gnomAD. The
 * heavy lifting is done by {@link GenicIntoleranceCalculator}.
 * To run the command enter
 * <pre>
 *     java -jar LIRICAL.jar gt2git -e <path to Exomiser database> -g <hg19 or hg38>
 * </pre>
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */

@CommandLine.Command(name = "background",
        aliases = {"B"},
        mixinStandardHelpOptions = true,
        description = "Calculation of background variant frequency",
        hidden = true)
public class BackgroundFrequencyCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundFrequencyCommand.class);

    @CommandLine.Option(names={"-e","--exomiser"},
            required = true,
            description = "path to Exomiser database directory")
    private Path exomiserDataDirectory;
    /** Should be one of hg19 or hg38. */
    @CommandLine.Option(names={"-g", "--genome"},
            paramLabel = "{hg19,hg38}",
            description = "string representing the genome assembly (default: ${DEFAULT-VALUE})")
    private String genomeAssemblyString="hg38";
    @CommandLine.Option(names={"--transcriptdb"},
            paramLabel = "{REFSEQ,UCSC}",
            description = "transcript database (default: ${DEFAULT-VALUE})")
    protected TranscriptDatabase transcriptDb = TranscriptDatabase.UCSC;
    /** If true, calculate the distribution of ClinVar pathogenicity scores. */
    @CommandLine.Option(names="--clinvar", description = "determine distribution of ClinVar pathogenicity scores")
    private boolean doClinvar;
    /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. In the current implementation this
     * is required to initialize the {@link LiricalFactory} object, but the data in this directory is not actually
     * needed for this analysis.*/
    @CommandLine.Option(names={"-d","--data"},
            description ="directory to download data (default: ${DEFAULT-VALUE})" )
    private Path datadir = Path.of("data");


    public BackgroundFrequencyCommand(){
    }

    @Override
    public Integer call() throws LiricalException {
        String outputFileName;
        if (genomeAssemblyString.toLowerCase().contains("hg19")) {

            outputFileName ="background-hg19.tsv";
        } else if (genomeAssemblyString.toLowerCase().contains("hg38")) {
            outputFileName ="background-hg38.tsv";
        } else {
            logger.warn("Could not determine genome assembly from argument: \""+
                    genomeAssemblyString +"\". We will use the default of hg38");
            outputFileName ="background-hg38.tsv";
        }
        if (this.exomiserDataDirectory ==null) {
            throw new LiricalException("Need to specify the Exomiser data directory: -e <path> to run gt2git command!");
        }

        LiricalFactory.Builder builder = LiricalFactory.builder()
                .exomiser(exomiserDataDirectory)
//                .datadir(this.datadir)
                .transcriptdatabase(transcriptDb)
                .genomeAssembly(genomeAssemblyString);

        LiricalFactory factory = builder.build();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();
        logger.trace("Will output background frequency file to " + outputFileName);

        MVStore alleleStore = factory.mvStore().get();
        JannovarData jannovarData = factory.jannovarData().get();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(factory.getAssembly(), jannovarData, emptyRegionIndex);
        String outputpath= outputFileName;
        GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator, alleleStore, outputpath, this.doClinvar);
        calculator.run();
        return 0;
    }

}
