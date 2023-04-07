package org.monarchinitiative.lirical.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * The command that sets up logging and then runs the downstream command.
 */
abstract class BaseCommand implements Callable<Integer> {

    @CommandLine.Option(names = {"-v"},
            description = {"Specify multiple -v options to increase verbosity.",
            "For example, `-v -v -v` or `-vvv`"})
    public boolean[] verbosity = {};

    @Override
    public Integer call() {
        // (0) Setup verbosity and print banner.
        setupLoggingAndPrintBanner();

        // (1) Run the command functionality.
        return execute();
    }

    protected abstract Integer execute();

    private void setupLoggingAndPrintBanner() {
        Level level = parseVerbosityLevel();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(level);

        printBanner();
    }

    private static String readBanner() {
        try (InputStream is = new BufferedInputStream(Objects.requireNonNull(LiricalConfigurationCommand.class.getResourceAsStream("/banner.txt")))) {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            // swallow
            return "";
        }
    }

    private Level parseVerbosityLevel() {
        int verbosity = 0;
        for (boolean a : this.verbosity) {
            if (a) verbosity++;
        }

        return switch (verbosity) {
            case 0 -> Level.INFO;
            case 1 -> Level.DEBUG;
            case 2 -> Level.TRACE;
            default -> Level.ALL;
        };
    }

    private static void printBanner() {
        System.err.println(readBanner());
    }

}
