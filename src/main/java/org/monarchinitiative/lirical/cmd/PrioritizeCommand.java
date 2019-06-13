package org.monarchinitiative.lirical.cmd;

import com.beust.jcommander.Parameter;
import org.monarchinitiative.lirical.configuration.LiricalFactory;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

/**
 * This is a common superclass for {@link YamlCommand} and {@link PhenopacketCommand}.
 * Its purpose is to provide command line parameters and variables that are used
 * in the same way by both of the subclasses.
 */
public abstract class PrioritizeCommand extends LiricalCommand {
    /** Directory where various files are downloaded/created. */
    @Parameter(names={"-d","--data"}, description ="directory to download data" )
    protected String datadir="data";
    /** Discard any candidate disease with no known disease gene or for which no predicted pathogenic variant
     * was found in the corresponding disease gene. */
    @Parameter(names={"-k","--keep"}, description = "retain candidates even if no candidate variant is found")
    protected boolean keepIfNoCandidateVariant=false;
    @Parameter(names={"-m","--mindiff"}, description = "minimal number of differential diagnoses to show")
    protected int minDifferentialsToShow=10;
    @Parameter(names={"--strict"},description="use strict genotype matching for likelihood ratio calculation")
    boolean strict=false;
    @Parameter(names={"-o","--output-directory"}, description = "directory into which to write output file(s).")
    protected String outdir=null;
    /** The threshold for showing a differential diagnosis in the main section (posterior probability of 1%).*/
    @Parameter(names= {"-t","--threshold"}, description = "minimum post-test prob. to show diagnosis in HTML output")
    protected double LR_THRESHOLD=0.01;
    /** If true, the program will not output an HTML file but will output a Tab Separated Values file instead.*/
    @Parameter(names="--tsv",description = "Use TSV instead of HTML output")
    protected boolean outputTSV=false;
    /** Prefix of the output file. For instance, if the user enters {@code -x sample1} and an HTML file is output,
     * the name of the HTML file will be {@code sample1.html}. If a TSV file is output, the name of the file will
     * be {@code sample1.tsv}. */
    @Parameter(names={"-x", "--prefix"},description = "prefix of outfile")
    protected String outfilePrefix="lirical";
    /** An object that contains parameters from the YAML file for configuration. */
    protected LiricalFactory factory;
    /** Key: an EntrezGene id; value: corresponding gene symbol. */
    protected Map<TermId,String> geneId2symbol;
    /** Various metadata that will be used for the HTML org.monarchinitiative.lirical.output. */
    protected Map<String,String> metadata;

}
