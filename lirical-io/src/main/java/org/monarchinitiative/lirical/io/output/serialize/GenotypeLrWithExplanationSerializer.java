package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;

import java.io.IOException;

public class GenotypeLrWithExplanationSerializer extends StdSerializer<GenotypeLrWithExplanation> {

    public GenotypeLrWithExplanationSerializer() {
        this(GenotypeLrWithExplanation.class);
    }

    protected GenotypeLrWithExplanationSerializer(Class<GenotypeLrWithExplanation> t) {
        super(t);
    }

    @Override
    public void serialize(GenotypeLrWithExplanation value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("geneId", value.geneId());
        gen.writeObjectField("matchType", value.matchType());
        gen.writeNumberField("lr", value.lr());
        gen.writeStringField("explanation", value.explanation());

        gen.writeEndObject();
    }
}
