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
import org.monarchinitiative.lirical.configuration.Lr2PgFactory;
import org.monarchinitiative.lirical.exception.Lr2pgException;
import org.monarchinitiative.lirical.gt2git.GenicIntoleranceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
@Parameters(commandDescription = "Calculation of background variant frequency", hidden = true)
public class Gt2GitCommand extends Lr2PgCommand {
    private static final Logger logger = LoggerFactory.getLogger(Gt2GitCommand.class);
//    @Parameter(names={"-d","--data"}, description ="directory to download data (default: data)" )
//    private String datadir="data";
    /** One of HG38 (default) or HG19. */
    private GenomeAssembly genomeAssembly;
    /** Name of the output file (e.g., background-hg19.txt). Determined automatically based on genome build..*/
    private String outputFileName;
    /** (e.g., ="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_variants.mv.db") */
   // @Parameter(names={"-m","--mvstore"}, description = "path to Exomiser MVStore file")
    private String mvStorePath;
    private String jannovarFile;

    /** Path of the Jannovar file. Note this can be taken from the Exomiser distribution, e.g.,
     * {@code exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser}. */
    @Parameter(names={"-e","--exomiser"}, description = "path to Exomiser database directory", required = true)
    private String exomiser;
    /** SHould be one of hg19 or hg38. */
    @Parameter(names={"-g", "--genome"}, description = "string representing the genome assembly (hg19,hg38)")
    private String genomeAssemblyString="hg38";
    @Parameter(names={"--transcriptdb"}, description = "Jannovar transcript database (UCSC, Ensembl, RefSeq)")
    private String transcriptdatabase="UCSC";
    /** If true, calculate the distribution of ClinVar pathogenicity scores. */
    @Parameter(names="--clinvar", description = "determine distribution of ClinVar pathogenicity scores")
    private boolean doClinvar;

    /**
     *
     */
    public Gt2GitCommand(){
    }

    @Override
    public void run() throws Lr2pgException  {
        if (genomeAssemblyString.toLowerCase().contains("hg19")) {
            this.genomeAssembly=GenomeAssembly.HG19;
            outputFileName="background-hg19.txt";
        } else if (genomeAssemblyString.toLowerCase().contains("hg38")) {
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.txt";
        } else {
            logger.warn("Could not determine genome assembly from argument: \""+
                    genomeAssemblyString +"\". We will use the default of hg38");
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.txt";
        }
        if (this.exomiser ==null) {
            throw new Lr2pgException("Need to specify the Exomiser data directory: -e <path> to run gt2git command!");
        }



        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder()
                .exomiser(exomiser)
                .transcriptdatabase(transcriptdatabase)
                .genomeAssembly(this.genomeAssemblyString);

        Lr2PgFactory factory = builder.build();
        factory.qcExomiserFiles();
        factory.qcGenomeBuild();
        logger.trace("Will output background frequency file to " + outputFileName);

        MVStore alleleStore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);
        String outputpath=this.outputFileName;
       GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator,alleleStore,outputpath,this.doClinvar);
       calculator.run();
    }




}
