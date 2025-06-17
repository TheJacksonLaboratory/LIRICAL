package org.monarchinitiative.lirical.cli.cmd;

import java.nio.file.Path;

public class BaseCommandTest {

    // `lirical-cli` directory.
    protected static final Path LIRICAL_CLI_DIR = Path.of("").toAbsolutePath();
    // Folder with example VCF, Phenopacket, and YAML inputs.
    protected static final Path EXAMPLES_DIR = LIRICAL_CLI_DIR.resolve("src").resolve("examples");
    protected static final Path VCF_FILE = EXAMPLES_DIR.resolve("LDS2.vcf.gz");

    // Fake data directory.
    protected static final Path DATA_DIR = LIRICAL_CLI_DIR.resolve("src").resolve("test").resolve("resources").resolve("data");
    protected static final Path EXOMISER_HG19 = DATA_DIR.resolve("2406_hg19");
    protected static final Path EXOMISER_HG38 = DATA_DIR.resolve("2406_hg38");

}
