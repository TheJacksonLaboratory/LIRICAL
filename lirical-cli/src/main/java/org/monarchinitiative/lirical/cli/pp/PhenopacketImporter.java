package org.monarchinitiative.lirical.cli.pp;

import java.io.InputStream;

public interface PhenopacketImporter {

    PhenopacketData read(InputStream is) throws PhenopacketImportException;

}
