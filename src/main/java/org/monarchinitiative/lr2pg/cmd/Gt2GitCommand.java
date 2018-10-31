package org.monarchinitiative.lr2pg.cmd;



import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.data.JannovarData;
import org.h2.mvstore.MVStore;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.JannovarVariantAnnotator;
import org.monarchinitiative.exomiser.core.model.ChromosomalRegionIndex;
import org.monarchinitiative.exomiser.core.model.RegulatoryFeature;
import org.monarchinitiative.lr2pg.configuration.Lr2PgFactory;
import org.monarchinitiative.lr2pg.exception.Lr2pgException;
import org.monarchinitiative.lr2pg.gt2git.GenicIntoleranceCalculator;

import java.util.List;

/**
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Gt2GitCommand extends Lr2PgCommand {
    /** Location of data directory (by default: "data"), where we will write the background frequency. */
    private final String datadir;
    /** Todo -- pass as argument */
    private String mvstore="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_variants.mv.db";
    /** Todo -- pass as argument */
    private String jannovarFile="/Users/peterrobinson/Documents/data/exomiser/1802_hg19/1802_hg19_transcripts_refseq.ser";
    /** Todo -- pass as argument */
    private GenomeAssembly genomeAssembly = GenomeAssembly.HG19;

    public Gt2GitCommand(String data){
        this.datadir=data;
    }


    public void run() throws Lr2pgException  {
        Lr2PgFactory.Builder builder = new Lr2PgFactory.Builder()
                .jannovarFile(jannovarFile)
                .mvStore(mvstore);

        Lr2PgFactory factory = builder.build();

        MVStore alleleStore = factory.mvStore();
        JannovarData jannovarData = factory.jannovarData();
        List<RegulatoryFeature> emtpylist = ImmutableList.of();
        ChromosomalRegionIndex<RegulatoryFeature> emptyRegionIndex = ChromosomalRegionIndex.of(emtpylist);
        JannovarVariantAnnotator jannovarVariantAnnotator = new JannovarVariantAnnotator(genomeAssembly, jannovarData, emptyRegionIndex);

       GenicIntoleranceCalculator calculator = new GenicIntoleranceCalculator(jannovarVariantAnnotator,alleleStore);
       calculator.run();
    }

}
