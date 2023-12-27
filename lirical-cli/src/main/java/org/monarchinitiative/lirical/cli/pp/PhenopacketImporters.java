package org.monarchinitiative.lirical.cli.pp;

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
