package org.monarchinitiative.lirical.io.analysis;

import java.io.InputStream;

/**
 * Decode phenopacket data from an input stream.
 */
public interface PhenopacketImporter {

    PhenopacketData read(InputStream is) throws PhenopacketImportException;

}
