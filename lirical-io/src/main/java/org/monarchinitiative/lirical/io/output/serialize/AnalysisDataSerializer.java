package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.time.Period;

public class AnalysisDataSerializer extends StdSerializer<AnalysisData> {

    public AnalysisDataSerializer() {
        this(AnalysisData.class);
    }

    protected AnalysisDataSerializer(Class<AnalysisData> t) {
        super(t);
    }

    @Override
    public void serialize(AnalysisData value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("sampleId", value.sampleId());
        Age age = value.age();
        if (age != null && !age.equals(Age.ageNotKnown())) {
            Period p = Period.of(age.getYears(), age.getMonths(), age.getDays());
            gen.writeObjectField("age", p.normalized().toString());
        }

        gen.writeObjectField("sex", value.sex());

        gen.writeArrayFieldStart("observedPhenotypicFeatures");
        for (TermId termId : value.presentPhenotypeTerms())
            gen.writeString(termId.getValue());
        gen.writeEndArray();

        gen.writeArrayFieldStart("excludedPhenotypicFeatures");
        for (TermId termId : value.negatedPhenotypeTerms())
            gen.writeString(termId.getValue());
        gen.writeEndArray();

        gen.writeEndObject();
    }
}
