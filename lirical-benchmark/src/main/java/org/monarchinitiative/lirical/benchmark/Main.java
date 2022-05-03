package org.monarchinitiative.lirical.benchmark;

import org.monarchinitiative.lirical.benchmark.cmd.BenchmarkCommand;
import org.monarchinitiative.lirical.benchmark.cmd.GridSearchCommand;
import org.monarchinitiative.lirical.benchmark.cmd.SimulatePhenopacketWithVcfCommand;
import org.monarchinitiative.lirical.benchmark.cmd.SimulatePhenotypeOnlyCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "java -jar lirical-benchmark.jar",
        mixinStandardHelpOptions = true,
        version = "2.0.0",
        sortOptions = false,
        description = "LIRICAL benchmark")
public class Main implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Override
    public Integer call() throws Exception {
        // The work is done in subcommands.
        return 0;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }

        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("benchmark", new BenchmarkCommand())
                .addSubcommand("grid", new GridSearchCommand())
                .addSubcommand("simulate", new SimulatePhenotypeOnlyCommand())
                .addSubcommand("simulate-vcf", new SimulatePhenopacketWithVcfCommand());

        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }
}
