package org.monarchinitiative.lirical.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LiricalDataResolverTest {

    @Test
    public void create() throws Exception {
        Path dataDirectory = TestResources.TEST_BASE.resolve("lirical_data");
        LiricalDataResolver resolver = LiricalDataResolver.of(dataDirectory);

        assertThat(resolver.hgncCompleteSet(), equalTo(dataDirectory.resolve("hgnc_complete_set.txt")));
        assertThat(resolver.hpoJson(), equalTo(dataDirectory.resolve("hp.json")));
        assertThat(resolver.mim2geneMedgen(), equalTo(dataDirectory.resolve("mim2gene_medgen")));
        assertThat(resolver.phenotypeAnnotations(), equalTo(dataDirectory.resolve("phenotype.hpoa")));

        assertThat(resolver.hg19UcscTxDatabase(), equalTo(dataDirectory.resolve("hg19_ucsc.ser")));
        assertThat(resolver.hg19EnsemblTxDatabase(), equalTo(dataDirectory.resolve("hg19_ensembl.ser")));
        assertThat(resolver.hg19RefseqTxDatabase(), equalTo(dataDirectory.resolve("hg19_refseq.ser")));
        assertThat(resolver.hg19RefseqCuratedTxDatabase(), equalTo(dataDirectory.resolve("hg19_refseq_curated.ser")));

        assertThat(resolver.hg38UcscTxDatabase(), equalTo(dataDirectory.resolve("hg38_ucsc.ser")));
        assertThat(resolver.hg38EnsemblTxDatabase(), equalTo(dataDirectory.resolve("hg38_ensembl.ser")));
        assertThat(resolver.hg38RefseqTxDatabase(), equalTo(dataDirectory.resolve("hg38_refseq.ser")));
        assertThat(resolver.hg38RefseqCuratedTxDatabase(), equalTo(dataDirectory.resolve("hg38_refseq_curated.ser")));
    }

    @Test
    public void missingMvStoreFileThrowsException() {
        LiricalDataException e = assertThrows(LiricalDataException.class, () -> LiricalDataResolver.of(TestResources.TEST_BASE));

        assertThat(e.getMessage(), containsString("Missing one or more resource files in Lirical data directory"));
    }

}