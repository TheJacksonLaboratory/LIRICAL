package org.monarchinitiative.lirical.beta;

import org.monarchinitiative.lirical.beta.cmd.PhenopacketCommand;
import org.monarchinitiative.lirical.beta.cmd.PrioritizeWithSquirlsCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static picocli.CommandLine.Help.Ansi.Style.*;

@CommandLine.Command(name = "java -jar lirical-beta.jar",
        header = "LIkelihood Ratio Interpretation of Clinical AbnormaLities (Beta features)",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class Main implements Callable<Integer> {

    public static final String VERSION = "v2.0.0";

    public static final int WIDTH = 120;

    public static final String FOOTER = "See the full documentation at https://lirical.readthedocs.io/en/master";

    private static final CommandLine.Help.ColorScheme COLOR_SCHEME = new CommandLine.Help.ColorScheme.Builder()
            .commands(bold, fg_blue, underline)
            .options(fg_yellow)
            .parameters(fg_yellow)
            .optionParams(italic)
            .build();

    public static void main(String[] args) {
        // if the user doesn't pass any command or option, add -h to show help
        if (args.length == 0)
            args = new String[]{"-h"};

        CommandLine cline = new CommandLine(new Main())
                .setColorScheme(COLOR_SCHEME)
                .addSubcommand("prioritize-squirls", new PrioritizeWithSquirlsCommand())
                .addSubcommand("phenopacket-squirls", new PhenopacketCommand());
        System.exit(cline.execute(args));

    }

    @Override
    public Integer call() throws Exception {
        // work done in subcommands
        return 0;
    }
}

