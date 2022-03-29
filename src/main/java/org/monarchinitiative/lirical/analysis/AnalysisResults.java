package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.lirical.likelihoodratio.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface AnalysisResults {

    static AnalysisResults of(List<TestResult> results) {
        return new AnalysisResultsDefault(results);
    }

    Stream<TestResult> results();

    default Stream<TestResult> prioritizedResults() {
        return results().sorted(Comparator.comparingDouble(TestResult::getCompositeLR).reversed());
    }

    default Map<TermId, TestResult> resultsByDiseaseId() {
        return results().collect(Collectors.toMap(TestResult::diseaseId, Function.identity()));
    }

}
