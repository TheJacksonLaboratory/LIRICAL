package org.monarchinitiative.lirical.cli.cmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class PrioritizeCommandTest extends BaseCommandTest {

    private PrioritizeCommand cmd;

    @BeforeEach
    public void setUp() {
        cmd = new PrioritizeCommand();
    }

    @Test
    public void phenotypeOnlyRun() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;

        List<String> checks = cmd.checkInput();

        assertThat(checks, is(empty()));
    }

    @Test
    public void genotypeAwareRunNeedsAssemblyAndExomiserDir() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;
        cmd.genomeBuild = "hg38";
        cmd.vcfPath = VCF_FILE;
        cmd.dataSection.exomiserHg38DataDirectory = EXOMISER_HG38;

        List<String> checks = cmd.checkInput();

        assertThat(checks, is(empty()));
    }

    @Test
    public void genotypeAwareRunNeedsAssemblyAndExomiserFiles() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;
        cmd.genomeBuild = "hg38";
        cmd.vcfPath = VCF_FILE;
        cmd.dataSection.exomiserHg38Database = EXOMISER_HG38.resolve("2406_hg38_variants.mv.db");
        cmd.dataSection.exomiserHg38ClinVarDatabase = EXOMISER_HG38.resolve("2406_hg38_clinvar.mv.db");

        List<String> checks = cmd.checkInput();

        assertThat(checks, is(empty()));
    }

    @Test
    public void complainsWhenMissingAssemblyOrVcf() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;
        cmd.dataSection.exomiserHg38DataDirectory = EXOMISER_HG38;

        cmd.genomeBuild = "hg38";
        assertThat(cmd.checkInput(), hasSize(1));
        assertThat(
                cmd.checkInput(), hasItems(
                        "The --assembly is set but --vcf is not specified. Proceed either with genotype-aware analysis with both --vcf and --assembly options, or run a phenotype-only analysis without the --vcf and --assembly options."
                )
        );

        cmd.genomeBuild = null;
        cmd.vcfPath = VCF_FILE;
        assertThat(cmd.checkInput(), hasSize(1));
        assertThat(
                cmd.checkInput(), hasItems(
                        "The --vcf is set but --assembly is not specified. Proceed either with genotype-aware analysis with both --vcf and --assembly options, or run a phenotype-only analysis without the --vcf and --assembly options."
                )
        );
    }

    @Test
    public void complainsWhenMissingExomiserDataDir() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;
        cmd.genomeBuild = "hg38";
        cmd.vcfPath = VCF_FILE;

        assertThat(cmd.checkInput(), hasSize(1));
        assertThat(
                cmd.checkInput(), hasItems(
                        "Genome build set to HG38 but Exomiser resources are unset"
                )
        );
    }

    @Test
    public void complainsWhenMissingExomiserFiles() {
        cmd.dataSection.liricalDataDirectory = DATA_DIR;
        cmd.genomeBuild = "hg38";
        cmd.vcfPath = VCF_FILE;

        cmd.dataSection.exomiserHg38Database = EXOMISER_HG38.resolve("2406_hg38_variants.mv.db");
        assertThat(cmd.checkInput(), hasSize(1));
        assertThat(
                cmd.checkInput(), hasItems(
                        "Genome build set to HG38 but Exomiser resources are unset"
                )
        );

        cmd.dataSection.exomiserHg38Database = null;
        cmd.dataSection.exomiserHg38ClinVarDatabase = EXOMISER_HG38.resolve("2406_hg38_clinvar.mv.db");
        assertThat(cmd.checkInput(), hasSize(1));
        assertThat(
                cmd.checkInput(), hasItems(
                        "Genome build set to HG38 but Exomiser resources are unset"
                )
        );
    }
}