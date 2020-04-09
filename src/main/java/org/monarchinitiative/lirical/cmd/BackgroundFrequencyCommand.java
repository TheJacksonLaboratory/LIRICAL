package org.monarchinitiative.lirical.cmd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalException;
import org.monarchinitiative.lirical.backgroundfrequency.GenicIntoleranceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
@Parameters(commandDescription = "Calculation of background variant frequency", hidden = true)
public class BackgroundFrequencyCommand extends LiricalCommand {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundFrequencyCommand.class);
    /** One of HG38 (default) or HG19. */
    private GenomeAssembly genomeAssembly;
    /** Name of the output file (e.g., background-hg19.tsv). Determined automatically based on genome build..*/
    private String outputFileName;
    /** Path of the Jannovar file. Note this can be taken from the Exomiser distribution, e.g.,
     * {@code exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser}. */
    @Parameter(names={"-e","--exomiser"}, description = "path to Exomiser database directory", required = true)
    private String exomiser;
    /** Should be one of hg19 or hg38. */
    @Parameter(names={"-g", "--genome"}, description = "string representing the genome assembly (hg19,hg38)")
    private String genomeAssemblyString="hg38";
    @Parameter(names={"--transcriptdb"}, description = "Jannovar transcript database (UCSC, RefSeq)")
    private String transcriptdatabase="UCSC";
    /** If true, calculate the distribution of ClinVar pathogenicity scores. */
    @Parameter(names="--clinvar", description = "determine distribution of ClinVar pathogenicity scores")
    private boolean doClinvar;
    /** Directory that contains {@code hp.obo} and {@code phenotype.hpoa} files. In the current implementation this
     * is required to initialize the {@link LiricalFactory} object, but the data in this directory is not actually
     * needed for this analysis.*/
    @Parameter(names={"-d","--data"}, description ="directory to download data" )
    private String datadir="data";


    public BackgroundFrequencyCommand(){
    }

    @Override
    public void run() throws LiricalException {
        if (genomeAssemblyString.toLowerCase().contains("hg19")) {
            this.genomeAssembly=GenomeAssembly.HG19;
            outputFileName="background-hg19.tsv";
        } else if (genomeAssemblyString.toLowerCase().contains("hg38")) {
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.tsv";
        } else {
            logger.warn("Could not determine genome assembly from argument: \""+
                    genomeAssemblyString +"\". We will use the default of hg38");
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.tsv";
        }
        if (this.exomiser ==null) {
            throw new LiricalException("Need to specify the Exomiser data directory: -e <path> to run gt2git command!");
        }

        LiricalFactory.Builder builder = new LiricalFactory.Builder()
                .exomiser(exomiser)
                .datadir(this.datadir)
                .transcriptdatabase(transcriptdatabase)
                .genomeAssembly(this.genomeAssemblyString);

        LiricalFactory factory = builder.buildForGt2Git();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();
        logger.trace("Will output background frequency file to " + outputFileName);

        MVStore alleleStore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);
        String outputpath=this.outputFileName;
        GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator, alleleStore, outputpath, this.doClinvar);
        calculator.run();
    }

}
