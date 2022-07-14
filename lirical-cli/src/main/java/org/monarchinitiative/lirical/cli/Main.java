package org.monarchinitiative.lirical.cli;

import org.monarchinitiative.lirical.cli.cmd.*;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Help.Ansi.Style.*;

@CommandLine.Command(name = "java -jar lirical-cli.jar",
        header = "LIkelihood Ratio Interpretation of Clinical AbnormaLities\n",
        mixinStandardHelpOptions = true,
        usageHelpWidth = Main.WIDTH,
        version = Main.VERSION,
        footer = Main.FOOTER)
public class Main implements Callable<Integer> {

    public static final String VERSION = "lirical-cli v2.0.0-SNAPSHOT";
    public static final int WIDTH = 120;
    public static final String FOOTER = "\nSee the full documentation at https://lirical.readthedocs.io/en/master";

    private static final CommandLine.Help.ColorScheme COLOR_SCHEME = new CommandLine.Help.ColorScheme.Builder()
            .commands(bold, fg_blue, underline)
            .options(fg_yellow)
            .parameters(fg_yellow)
            .optionParams(italic)
            .build();

    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }

        CommandLine cline = new CommandLine(new Main())
                .setColorScheme(COLOR_SCHEME)
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
