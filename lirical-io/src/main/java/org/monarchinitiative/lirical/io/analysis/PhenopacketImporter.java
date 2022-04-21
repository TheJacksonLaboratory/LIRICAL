package org.monarchinitiative.lirical.io.analysis;

import java.io.InputStream;

public interface PhenopacketImporter {

    PhenopacketData read(InputStream is) throws PhenopacketImportException;

}
