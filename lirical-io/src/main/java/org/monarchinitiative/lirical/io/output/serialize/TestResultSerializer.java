package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;

import java.io.IOException;

public class TestResultSerializer extends StdSerializer<TestResult> {

    public TestResultSerializer() {
        this(TestResult.class);
    }

    protected TestResultSerializer(Class<TestResult> t) {
        super(t);
    }

    @Override
    public void serialize(TestResult value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("diseaseId", value.diseaseId().getValue());
        gen.writeNumberField("pretestProbability", value.pretestProbability());

        // observed phenotypic features
        gen.writeArrayFieldStart("observedPhenotypicFeatures");
        for (LrWithExplanation lre : value.observedResults())
            gen.writeObject(lre);
        gen.writeEndArray();

        // excluded phenotypic features
        gen.writeArrayFieldStart("excludedPhenotypicFeatures");
        for (LrWithExplanation lre : value.excludedResults())
            gen.writeObject(lre);
        gen.writeEndArray();


        // genotypeLR
        if (value.genotypeLr().isPresent())
            gen.writeObjectField("genotypeLR", value.genotypeLr().get());


        gen.writeNumberField("compositeLR", value.getCompositeLR());
        gen.writeNumberField("posttestProbability", value.posttestProbability());

        gen.writeEndObject();
    }
}
