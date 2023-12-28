package org.monarchinitiative.lirical.core.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A container for the likelihood ratio test results.
 */
public interface AnalysisResults extends Iterable<TestResult> {

    static AnalysisResults empty() {
        return AnalysisResultsDefault.empty();
    }

    static AnalysisResults of(List<TestResult> results) {
        if (results.isEmpty())
            return AnalysisResultsDefault.empty();
        return new AnalysisResultsDefault(results);
    }

    /**
     * @return test result count
     */
    @JsonIgnore
    int size();

    @JsonIgnore
    default boolean isEmpty() {
        return size() == 0;
    }

    @JsonIgnore
    Optional<TestResult> resultByDiseaseId(TermId diseaseId);

    @JsonIgnore
    default Stream<TestResult> results() {
        return StreamSupport.stream(spliterator(), false);
    }

    @JsonIgnore
    default Stream<TestResult> resultsWithDescendingPostTestProbability() {
        return results().sorted(Comparator.comparingDouble(TestResult::posttestProbability).reversed());
    }

}
