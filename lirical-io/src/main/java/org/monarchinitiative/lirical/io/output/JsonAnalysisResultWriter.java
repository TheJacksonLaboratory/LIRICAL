package org.monarchinitiative.lirical.io.output;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.output.serialize.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonAnalysisResultWriter implements AnalysisResultsWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAnalysisResultWriter.class);

    private static final Version VERSION = new Version(0, 1, 0, null, null, null);
    private static final JsonAnalysisResultWriter INSTANCE = new JsonAnalysisResultWriter();
    private final ObjectMapper objectMapper;

    public static JsonAnalysisResultWriter of() {
        return INSTANCE;
    }

    private JsonAnalysisResultWriter() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.registerModule(prepareModule());
        objectMapper.registerModule(new Jdk8Module());
    }

    @Override
    public void process(AnalysisData analysisData,
                        AnalysisResults analysisResults,
                        AnalysisResultsMetadata metadata,
                        OutputOptions outputOptions) throws IOException {
        Path output = outputOptions.outputDirectory().resolve(outputOptions.prefix() + ".json");
        LOGGER.info("Writing JSON file to {}", output.toAbsolutePath());
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output));
             JsonGenerator generator = objectMapper.createGenerator(os)) {

            generator.writeStartObject();

            // We write all but the variants here
            generator.writeObjectField("analysisData", analysisData);
            generator.writeObjectField("analysisMetadata", metadata);
            generator.writeObjectField("analysisResults", analysisResults);

            generator.writeEndObject();
        }
    }

    private static SimpleModule prepareModule() {
        SimpleModule module = new SimpleModule("JsonAnalysisResultSerializer", VERSION);

        // serializers
        // TODO - remove when using phenol >=2.0.5
        module.addSerializer(new GeneIdentifierSerializer());

        return module;
    }

}
