package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;

import java.io.IOException;

public class LrWithExplanationSerializer extends StdSerializer<LrWithExplanation> {

    public LrWithExplanationSerializer() {
        this(LrWithExplanation.class);
    }

    protected LrWithExplanationSerializer(Class<LrWithExplanation> t) {
        super(t);
    }

    @Override
    public void serialize(LrWithExplanation value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("query", value.queryTerm().getValue());
        gen.writeStringField("match", value.matchingTerm().getValue());
        gen.writeObjectField("matchType", value.matchType());
        gen.writeNumberField("lr", value.lr());
        gen.writeStringField("explanation", value.explanation());

        gen.writeEndObject();
    }
}
