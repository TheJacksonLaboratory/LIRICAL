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

        assertThat(resolver.homoSapiensGeneInfo(), equalTo(dataDirectory.resolve("Homo_sapiens.gene_info.gz")));
        assertThat(resolver.hpoJson(), equalTo(dataDirectory.resolve("hp.json")));
        assertThat(resolver.mim2geneMedgen(), equalTo(dataDirectory.resolve("mim2gene_medgen")));
        assertThat(resolver.phenotypeAnnotations(), equalTo(dataDirectory.resolve("phenotype.hpoa")));
    }

    @Test
    public void missingMvStoreFileThrowsException() {
        LiricalDataException e = assertThrows(LiricalDataException.class, () -> LiricalDataResolver.of(TestResources.TEST_BASE));

        assertThat(e.getMessage(), containsString("Missing one or more resource files in Lirical data directory"));
    }

}