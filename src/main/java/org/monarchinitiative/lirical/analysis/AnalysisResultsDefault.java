package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.lirical.likelihoodratio.TestResult;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

final class AnalysisResultsDefault implements AnalysisResults {
    private final List<TestResult> results;

    AnalysisResultsDefault(List<TestResult> results) {
        this.results = results;
    }

    @Override
    public Stream<TestResult> results() {
        return results.stream();
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
