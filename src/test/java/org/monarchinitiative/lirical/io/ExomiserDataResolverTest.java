package org.monarchinitiative.lirical.io;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.lirical.TestResources;
import org.monarchinitiative.lirical.exception.LiricalRuntimeException;
import org.monarchinitiative.lirical.model.GenomeBuild;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class ExomiserDataResolverTest {


    @Test
    public void create() throws Exception {
        Path dataDirectory = TestResources.TEST_BASE.resolve("exomiser_data");

        ExomiserDataResolver resolver = ExomiserDataResolver.of(dataDirectory);

        assertThat(resolver.version(), equalTo("1710"));
        assertThat(resolver.assembly(), equalTo("hg38"));
        assertThat(resolver.genomeBuild().isPresent(), equalTo(true));
        assertThat(resolver.genomeBuild().get(), equalTo(GenomeBuild.HG38));

        assertThat(resolver.mvStorePath(), equalTo(dataDirectory.resolve("1710_hg38_variants.mv.db")));
        assertThat(resolver.refseqTranscriptCache(), equalTo(dataDirectory.resolve("1710_hg38_transcripts_refseq.ser")));
        assertThat(resolver.ucscTranscriptCache(), equalTo(dataDirectory.resolve("1710_hg38_transcripts_ucsc.ser")));
    }

    @Test
    public void missingMvStoreFileThrowsException() {
        LiricalDataException e = assertThrows(LiricalDataException.class, () -> ExomiserDataResolver.of(TestResources.TEST_BASE));

        assertThat(e.getMessage(), containsString("Did not find Exomiser MV store file in"));
    }
}