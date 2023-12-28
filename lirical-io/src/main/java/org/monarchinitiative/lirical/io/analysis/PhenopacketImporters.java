package org.monarchinitiative.lirical.io.analysis;

/**
 * Get {@link PhenopacketImporter}s to ingest v1 or v2 phenopackets.
 */
public class PhenopacketImporters {

    private PhenopacketImporters() {
    }

    public static PhenopacketImporter v1() {
        return PhenopacketV1Importer.instance();
    }

    public static PhenopacketImporter v2() {
        return PhenopacketV2Importer.instance();
    }
}
