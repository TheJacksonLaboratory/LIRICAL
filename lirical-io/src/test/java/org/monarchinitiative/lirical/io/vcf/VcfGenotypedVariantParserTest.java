package org.monarchinitiative.lirical.io.vcf;

import htsjdk.variant.vcf.VCFFileReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.model.AlleleCount;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.GenotypedVariant;
import org.monarchinitiative.lirical.io.TestResources;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VcfGenotypedVariantParserTest {

    private static final GenomicAssembly GENOMIC_ASSEMBLY = GenomicAssemblies.GRCh38p13();

    private static final Path VCF_PATH = TestResources.LIRICAL_TEST_BASE.resolve("vcf").resolve("multiSample.vcf");

    private VCFFileReader reader;

    @BeforeEach
    public void setUp() {
        reader = new VCFFileReader(VCF_PATH, false);
    }

    @AfterEach
    public void tearDown() {
        reader.close();
    }

    @Test
    public void variantStream() {
        VcfGenotypedVariantParser parser = new VcfGenotypedVariantParser(GENOMIC_ASSEMBLY, GenomeBuild.HG38, reader);

        List<GenotypedVariant> variants = parser.variantStream().toList();

        assertThat(variants, hasSize(4));
        GenotypedVariant a0 = variants.get(0);
        assertThat(a0.alleleCount("Holly").isPresent(), equalTo(true));
        assertThat(a0.alleleCount("Holly").get(), equalTo(AlleleCount.of(0, 1)));

        assertThat(a0.alleleCount("Walt").isPresent(), equalTo(true));
        assertThat(a0.alleleCount("Walt").get(), equalTo(AlleleCount.of(1, 1)));

        assertThat(a0.alleleCount("Skyler").isPresent(), equalTo(true));
        assertThat(a0.alleleCount("Skyler").get(), equalTo(AlleleCount.of(0, 0)));

        GenotypedVariant a1 = variants.get(1);
        assertThat(a1.alleleCount("Holly").isPresent(), equalTo(true));
        assertThat(a1.alleleCount("Holly").get(), equalTo(AlleleCount.of(0, 1)));

        assertThat(a1.alleleCount("Walt").isPresent(), equalTo(true));
        assertThat(a1.alleleCount("Walt").get(), equalTo(AlleleCount.of(1, 0)));

        assertThat(a1.alleleCount("Skyler").isPresent(), equalTo(true));
        assertThat(a1.alleleCount("Skyler").get(), equalTo(AlleleCount.of(0, 2)));

        GenotypedVariant c = variants.get(3);
        assertThat(c.alleleCount("Holly").isPresent(), equalTo(true));
        assertThat(c.alleleCount("Holly").get(), equalTo(AlleleCount.of(1, 1)));

        assertThat(c.alleleCount("Walt").isPresent(), equalTo(false));

        assertThat(c.alleleCount("Skyler").isPresent(), equalTo(true));
        assertThat(c.alleleCount("Skyler").get(), equalTo(AlleleCount.of(2, 0)));
    }
}