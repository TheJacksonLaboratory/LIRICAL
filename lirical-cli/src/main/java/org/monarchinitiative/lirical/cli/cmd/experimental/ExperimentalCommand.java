package org.monarchinitiative.lirical.cli.cmd.experimental;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "experimental",
        hidden = true,
        subcommands = {
                PhenopacketsCommand.class,
        },
        sortOptions = false,
        mixinStandardHelpOptions = true,
        description = "Run experimental LIRICAL commands.")
public class ExperimentalCommand implements Callable<Integer> {

        @Override
        public Integer call() {
                // Work is done in subcommands.
                return 0;
        }
}
