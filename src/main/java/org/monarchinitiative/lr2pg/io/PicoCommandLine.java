package org.monarchinitiative.lr2pg.io;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "java -jar Lr2pg.jar",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage|@:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description|@:%n%n",
        parameterListHeading = "%n@|bold,underline Parameters|@:%n",
        optionListHeading = "%n@|bold,underline Options|@:%n",
        header = "likelihood-ratio analysis of phenotypes and genotypes.",
        description = "Stores the current contents of the index in a new commit " +
                "along with a log message from the user describing the changes.")
public class PicoCommandLine {

    /** Path to YAML configuration file*/
    @CommandLine.Option(names = {"-y","--yaml"}, description = "path to yaml configuration file")
    private String yamlPath = null;

    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default \"data\")" )
    private File datadir;
    @CommandLine.Option(names={"-o","--overwrite"},description="overwrite previously downloaded files?" )
    private boolean overwriteDownload = false;


    /*
     private Options constructOptions() {
        final Options options = new Options();
        options.addOption("d", "data", true, "directory to download data (default \"data\")")
                .addOption(null, "clinvar", false, "determine distribution of ClinVar pathogenicity scores")
                .addOption("g", "genome", true, "string representing the genome assembly (hg19,hg38)")
                .addOption("h", "help", false, "show help")
                .addOption("j", "jannovar", true, "path to Jannovar transcript file")
                .addOption("m", "mvstore", true, "path to Exomiser MVStore file")
                .addOption("o", "overwrite", false, "overwrite downloaded files")
                .addOption("y", "yaml", true, "");
        return options;
    }
     */
}
