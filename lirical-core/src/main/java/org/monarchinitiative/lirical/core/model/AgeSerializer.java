package org.monarchinitiative.lirical.core.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Period;

class AgeSerializer extends StdSerializer<Age> {

    AgeSerializer() {
        super(Age.class);
    }

    AgeSerializer(Class<Age> t) {
        super(t);
    }

    @Override
    public void serialize(Age age, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Period p = Period.of(age.getYears(), age.getMonths(), age.getDays());
        gen.writeString(p.normalized().toString());
    }
}
