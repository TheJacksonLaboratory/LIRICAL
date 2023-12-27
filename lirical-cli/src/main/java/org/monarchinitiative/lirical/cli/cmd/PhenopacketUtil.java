package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.core.analysis.LiricalParseException;
import org.monarchinitiative.lirical.cli.pp.PhenopacketData;
import org.monarchinitiative.lirical.cli.pp.PhenopacketImportException;
import org.monarchinitiative.lirical.cli.pp.PhenopacketImporter;
import org.monarchinitiative.lirical.cli.pp.PhenopacketImporters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhenopacketUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketUtil.class);

    private PhenopacketUtil() {
    }

    public static PhenopacketData readPhenopacketData(Path phenopacket) throws LiricalParseException {
        LOGGER.trace("Reading phenopacket from {}", phenopacket.toAbsolutePath());
        PhenopacketData data = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacket))) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.trace("Success!");
        } catch (PhenopacketImportException | IOException e) {
            LOGGER.trace("Unable to parse as v2 phenopacket, trying v1");
        }

        if (data == null) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacket))) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
            } catch (PhenopacketImportException | IOException e) {
                LOGGER.trace("Unable to parse as v1 phenopacket");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacket.toAbsolutePath());
            }
        }
        return data;
    }

}
