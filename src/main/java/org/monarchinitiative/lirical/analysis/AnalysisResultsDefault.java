package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

final class AnalysisResultsDefault implements AnalysisResults {

    private static final AnalysisResultsDefault EMPTY = new AnalysisResultsDefault(List.of());

    static AnalysisResultsDefault empty() {
        return EMPTY;
    }

    private final List<TestResult> results;

    private final Map<TermId, TestResult> resultByDiseaseId;

    AnalysisResultsDefault(List<TestResult> results) {
        this.results = Objects.requireNonNull(results);
        this.resultByDiseaseId = results.stream()
                .collect(Collectors.toMap(TestResult::diseaseId, Function.identity()));
    }

    @Override
    public Iterator<TestResult> iterator() {
        return results.iterator();
    }

    @Override
    public int size() {
        return results.size();
    }

    @Override
    public Optional<TestResult> resultByDiseaseId(TermId diseaseId) {
        return Optional.ofNullable(resultByDiseaseId.get(diseaseId));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AnalysisResultsDefault) obj;
        return Objects.equals(this.results, that.results);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results);
    }

    @Override
    public String toString() {
        return "AnalysisResultsDefault[" +
                "results=" + results + ']';
    }

}
