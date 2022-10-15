package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;

import java.io.IOException;

public class AnalysisResultsSerializer extends StdSerializer<AnalysisResults> {

    public AnalysisResultsSerializer() {
        this(AnalysisResults.class);
    }

    protected AnalysisResultsSerializer(Class<AnalysisResults> t) {
        super(t);
    }

    @Override
    public void serialize(AnalysisResults value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartArray();

        for (TestResult result : value.resultsWithDescendingPostTestProbability().toList())
            gen.writeObject(result);

        gen.writeEndArray();
    }
}
