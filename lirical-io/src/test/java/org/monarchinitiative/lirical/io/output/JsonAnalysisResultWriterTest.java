package org.monarchinitiative.lirical.io.output;

import org.junit.jupiter.api.*;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrMatchType;
import org.monarchinitiative.lirical.core.likelihoodratio.GenotypeLrWithExplanation;
import org.monarchinitiative.lirical.core.likelihoodratio.LrMatchType;
import org.monarchinitiative.lirical.core.likelihoodratio.LrWithExplanation;
import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.LrThreshold;
import org.monarchinitiative.lirical.core.output.MinDiagnosisCount;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.TestResources;
import org.monarchinitiative.phenol.annotations.formats.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class JsonAnalysisResultWriterTest {

    private JsonAnalysisResultWriter writer;

    private static final TermId a = TermId.of("HP:0000001");
    private static final TermId b = TermId.of("HP:0000002");
    private static final TermId c = TermId.of("HP:0000003");

    private static String EXAMPLE_ANALYSIS_OUTPUT;
    // We expect to find the JSON file with the analysis output here.
    private static final Path TEST_OUTPUT = Path.of("test.json");

    @BeforeAll
    public static void beforeAll() throws Exception {
        EXAMPLE_ANALYSIS_OUTPUT = readFileIntoString(TestResources.LIRICAL_TEST_BASE.resolve("output").resolve("example-analysis-output.json"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(TEST_OUTPUT);
    }

    @BeforeEach
    public void setUp() {
        writer = JsonAnalysisResultWriter.of();
    }

    @Test
    public void writeExampleAnalysisOutput() throws Exception {
        AnalysisData analysisData = createTestAnalysisData();
        AnalysisResults results = createTestAnalysisResults();
        AnalysisResultsMetadata metadata = createTestMetadata();
        Path current = Path.of(".");
        OutputOptions oo = new OutputOptions(LrThreshold.notInitialized(), MinDiagnosisCount.setToUserDefinedMinCount(2), 1.f, true, current, "test");
        writer.process(analysisData, results, metadata, oo);

        String output = readFileIntoString(TEST_OUTPUT);

        assertThat(output, equalTo(EXAMPLE_ANALYSIS_OUTPUT));
    }

    private static AnalysisData createTestAnalysisData() {
        return AnalysisData.of("sampleId", Age.of(1, 2, 3), Sex.MALE, List.of(a, b), List.of(c), GenesAndGenotypes.empty());
    }

    private static AnalysisResultsMetadata createTestMetadata() {
        return AnalysisResultsMetadata.builder()
                .setLiricalVersion("liricalVersion")
                .setHpoVersion("hpoVersion")
                .setTranscriptDatabase("transcriptDatabase")
                .setLiricalPath("liricalPath")
                .setExomiserPath("exomiserPath")
                .setAnalysisDate(LocalDateTime.of(2023, 11, 28, 20, 37, 14, 123456).toString())
                .setSampleName("sampleId")
                .setnPassingVariants(2)
                .setnFilteredVariants(4)
                .setGenesWithVar(6)
                .setGlobalMode(true)
                .build();
    }

    private static AnalysisResults createTestAnalysisResults() {
        List<LrWithExplanation> observed = List.of(LrWithExplanation.of(a, b, LrMatchType.EXACT_MATCH, 1.34, "EXPLANATION"));
        List<LrWithExplanation> excluded = List.of(LrWithExplanation.of(a, c, LrMatchType.EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE, 1.23, "EXCLUDED_EXPLANATION"));
        GenotypeLrWithExplanation genotypeLr = GenotypeLrWithExplanation.of(
                GeneIdentifier.of(TermId.of("NCBIGene:1234"), "GENE_SYMBOL"),
                GenotypeLrMatchType.LIRICAL_GT_MODEL,
                1.23,
                "GENE_EXPLANATION");
        return AnalysisResults.of(
                List.of(
                        TestResult.of(
                                TermId.of("OMIM:1234567"),
                                1.2,
                                observed,
                                excluded,
                                genotypeLr)
                )
        );
    }

    private static String readFileIntoString(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }
}