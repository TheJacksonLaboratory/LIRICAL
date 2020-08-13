package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.analysis.Gene2Genotype;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.util.Map;

/**
 * This is a common superclass for {@link YamlCommand} and {@link PhenopacketCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by both of the subclasses.
 * @author Peter N Robinson
 */
public abstract class AbstractPrioritizeCommand {
    /** Directory where various files are downloaded/created. */
    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    protected String datadir="data";
    /** If global is set to true, then LIRICAL will not discard candidate diseases with no known disease gene or
     * candidates for which no predicted pathogenic variant was found in the VCF. */
    @CommandLine.Option(names={"-g","--global"}, description = "global analysis (default: ${DEFAULT-VALUE})")
    protected boolean globalAnalysisMode = false;
    @CommandLine.Option(names={"-m","--mindiff"}, description = "minimal number of differential diagnoses to show")
    protected Integer minDifferentialsToShow = null;
    @CommandLine.Option(names={"-o","--output-directory"}, description = "directory into which to write output (default: ${DEFAULT-VALUE}).")
    protected String outdir=".";
    /** The threshold for showing a differential diagnosis in the main section (posterior probability of 5%).*/
    @CommandLine.Option(names= {"-t","--threshold"}, description = "minimum post-test prob. to show diagnosis in HTML output")
    protected Double LR_THRESHOLD = null;
    /** If true, the program will not output an HTML file but will output a Tab Separated Values file instead.*/
    @CommandLine.Option(names="--tsv",description = "Use TSV instead of HTML output (default: ${DEFAULT-VALUE})")
    protected boolean outputTSV=false;
    /** Prefix of the output file. For instance, if the user enters {@code -x sample1} and an HTML file is output,
     * the name of the HTML file will be {@code sample1.html}. If a TSV file is output, the name of the file will
     * be {@code sample1.tsv}. */
    @CommandLine.Option(names={"-x", "--prefix"},description = "prefix of outfile (default: ${DEFAULT-VALUE})")
    protected String outfilePrefix="lirical";
    @CommandLine.Option(names={"--orpha"},description = "use Orphanet annotation data (default: ${DEFAULT-VALUE})")
    boolean useOrphanet = false;
    /** An object that contains parameters from the YAML file for configuration. */
    protected LiricalFactory factory;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected Map<TermId,String> geneId2symbol;
    protected Map<TermId, Gene2Genotype> genotypeMap;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    protected Map<String,String> metadata;

    protected void checkThresholds() {
        if (LR_THRESHOLD != null && minDifferentialsToShow != null) {
            System.err.println("[ERROR] Only one of the options -t/--threshold and -m/--mindiff can be used at once.");
            throw new LiricalRuntimeException("Only one of the options -t/--threshold and -m/--mindiff can be used at once.");
        }
        if (LR_THRESHOLD != null ) {
            if (LR_THRESHOLD < 0.0 || LR_THRESHOLD > 1.0) {
                System.err.println("[ERROR] Post-test probability (-t/--threshold) must be between 0.0 and 1.0.");
                throw new LiricalRuntimeException("Post-test probability (-t/--threshold) must be between 0.0 and 1.0.");
            }
        }
    }

}
