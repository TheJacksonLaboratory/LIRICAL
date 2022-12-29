package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;

import java.io.IOException;

public class GeneIdentifierSerializer extends StdSerializer<GeneIdentifier> {

    public GeneIdentifierSerializer() {
        this(GeneIdentifier.class);
    }

    protected GeneIdentifierSerializer(Class<GeneIdentifier> t) {
        super(t);
    }

    @Override
    public void serialize(GeneIdentifier value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("id", value.id().getValue());
        gen.writeStringField("symbol", value.symbol());

        gen.writeEndObject();
    }
}
