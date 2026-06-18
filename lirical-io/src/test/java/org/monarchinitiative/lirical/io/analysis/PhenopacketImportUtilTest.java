package org.monarchinitiative.lirical.io.analysis;

import org.junit.jupiter.api.Test;
import org.phenopackets.schema.v2.Phenopacket;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PhenopacketImportUtilTest {

    @Test
    public void readPhenopacket_protobuf() throws IOException, PhenopacketImportException {
        Phenopacket phenopacket;
        try (InputStream is = PhenopacketImportUtilTest.class.getResourceAsStream("pfeiffer.v2.pb")) {
            phenopacket = PhenopacketImportUtil.readPhenopacket(is, Phenopacket.class);
        }
        assertThat(phenopacket.getId(), equalTo("pfeiffer-phenopacket"));
        assertThat(phenopacket.getSubject().getId(), equalTo("II:3/Family 2"));
    }

}