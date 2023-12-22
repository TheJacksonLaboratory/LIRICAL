package org.monarchinitiative.lirical.cli.pp;

import com.google.protobuf.Message;
import org.phenopackets.phenopackettools.core.PhenopacketElement;
import org.phenopackets.phenopackettools.core.PhenopacketSchemaVersion;
import org.phenopackets.phenopackettools.io.PhenopacketParser;
import org.phenopackets.phenopackettools.io.PhenopacketParserFactory;
import org.phenopackets.phenopackettools.util.format.SniffException;

import java.io.IOException;
import java.io.InputStream;

class PhenopacketImportUtil {

    private PhenopacketImportUtil() {
    }

    static <T> T readPhenopacket(InputStream is,
                                 Class<T> clz) throws PhenopacketImportException {
        PhenopacketParserFactory factory = PhenopacketParserFactory.getInstance();
        PhenopacketSchemaVersion schemaVersion = parseSchemaVersion(clz);
        PhenopacketParser parser = factory.forFormat(schemaVersion);
        Message message;
        try {
            message = parser.parse(PhenopacketElement.PHENOPACKET, is);
        } catch (IOException | SniffException e) {
            throw new PhenopacketImportException(e);
        }

        if (clz.isInstance(message)) {
            return clz.cast(message);
        } else {
            throw new PhenopacketImportException("Expected a %s but got %s [%s]".formatted(
                    clz.getName(),
                    message.getClass().getSimpleName(),
                    message.getClass().getName()));
        }
    }

    private static PhenopacketSchemaVersion parseSchemaVersion(Class<?> clz) throws PhenopacketImportException {
        if ("org.phenopackets.schema.v1".equals(clz.getPackageName())) return PhenopacketSchemaVersion.V1;
        else if ("org.phenopackets.schema.v2".equals(clz.getPackageName())) return PhenopacketSchemaVersion.V2;
        else throw new PhenopacketImportException("Cannot determine phenopacket schema version of a class %s".formatted(clz.getName()));
    }

}
