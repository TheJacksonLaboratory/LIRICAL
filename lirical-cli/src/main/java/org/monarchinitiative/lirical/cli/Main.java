package org.monarchinitiative.lirical.cli;



import org.monarchinitiative.lirical.cli.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;


/**
 * The CLI driver class.
 * @author Peter Robinson
 * @version 1.3.1 (2020-08-13)
 */

@CommandLine.Command(name = "java -jar lirical-cli.jar", mixinStandardHelpOptions = true,
        version = "1.3.4",
        description = "LIkelihood Ratio Interpretation of Clinical AbnormaLities")
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }

        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("prioritize", new PrioritizeCommand())
                .addSubcommand("phenopacket", new PhenopacketCommand())
                .addSubcommand("yaml", new YamlCommand())
                .addSubcommand("benchmark", new BenchmarkCommand());
        cline.setToggleBooleanFlags(false);
        System.exit(cline.execute(args));
    }

    @Override
    public Integer call() {
        // work done in subcommands
        return 0;
    }

}
