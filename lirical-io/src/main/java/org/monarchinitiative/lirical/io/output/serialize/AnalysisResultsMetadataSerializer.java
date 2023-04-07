package org.monarchinitiative.lirical.io.output.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;

import java.io.IOException;

public class AnalysisResultsMetadataSerializer extends StdSerializer<AnalysisResultsMetadata> {


    public AnalysisResultsMetadataSerializer() {
        this(AnalysisResultsMetadata.class);
    }

    protected AnalysisResultsMetadataSerializer(Class<AnalysisResultsMetadata> t) {
        super(t);
    }

    @Override
    public void serialize(AnalysisResultsMetadata value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("liricalVersion", value.getLiricalVersion());
        gen.writeStringField("hpoVersion", value.getHpoVersion());
        gen.writeStringField("transcriptDatabase", value.getTranscriptDatabase());
        gen.writeStringField("analysisDate", value.getAnalysisDate());
        gen.writeStringField("sampleName", value.getSampleName());
        gen.writeBooleanField("isGlobalAnalysisMode", value.getGlobalMode());

        gen.writeEndObject();
    }
}
