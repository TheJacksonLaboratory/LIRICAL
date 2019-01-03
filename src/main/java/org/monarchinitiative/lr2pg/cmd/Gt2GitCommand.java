package org.monarchinitiative.lr2pg.cmd;



import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.data.JannovarData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.gt2git.GenicIntoleranceCalculator;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Gt2GitCommand extends Lr2PgCommand {
    private static final Logger logger = LogManager.getLogger();
    /** Location of data directory (by default: "data"), where we will write the background frequency. */
    private final String datadir;
    /** (e.g., ="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_variants.mv.db") */
    private final String mvstore;
    /** (e.g., ="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser") */
    private final String jannovarFile;
    /** One of HG38 (default) or HG19. */
    private final GenomeAssembly genomeAssembly;
    /** Name of org.monarchinitiative.lr2pg.output file.*/
    private final String outputFileName;

    private final boolean doClinvar;

    /**
     *
     * @param data Path to directory where the background file will be written.
     * @param mv Path to the Exomiser mvstore data resource
     * @param jann Path to the Jannovar transcript file
     * @param genome String representing genome build (hg19 or hg38)
     */
    public Gt2GitCommand(String data, String mv, String jann, String genome, boolean doClinvar){
        this.datadir=data;
        this.mvstore=mv;
        this.jannovarFile=jann;
        this.doClinvar=doClinvar;
        if (genome.toLowerCase().contains("hg19")) {
            this.genomeAssembly=GenomeAssembly.HG19;
            outputFileName="background-hg19.txt";
        } else if (genome.toLowerCase().contains("hg38")) {
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg19.txt";
        } else {
            logger.warn("Could not determine genome assembly from argument: \""+
            genome +"\". We will use the default of hg38");
            this.genomeAssembly=GenomeAssembly.HG38;
            outputFileName="background-hg38.txt";
        }
    }


    public void run() throws Lr2pgException  {
        if (this.mvstore==null) {
            throw new Lr2pgException("Need to specify the MVStore file: -m <mvstore> to run gt2git command!");
        }
        if (this.jannovarFile==null) {
            throw new Lr2pgException("Need to specify the jannovar transcript file: -j <jannovar> to run gt2git command!");
        }


        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder()
                .jannovarFile(jannovarFile)
                .mvStore(mvstore);

        Lr2PgFactory factory = builder.build();

        MVStore alleleStore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);
        createOutputDirectoryIfNecessary();
        String outputpath=String.format("%s%s%s",this.datadir,File.separator,this.outputFileName);
       GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator,alleleStore,outputpath,this.doClinvar);
       calculator.run();
    }


    private void createOutputDirectoryIfNecessary(){
        File dir = new File(this.datadir);
        if (! dir.exists() ) {
            boolean b = dir.mkdir();
            if (b) {
                logger.info("Successfully created directory at " + this.datadir);
            } else {
                logger.fatal("Unable to create directory at {}. Terminating program",this.datadir);
                System.exit(1);
            }
        }
    }

}
