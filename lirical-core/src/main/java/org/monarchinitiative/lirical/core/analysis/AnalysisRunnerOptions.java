package org.monarchinitiative.lirical.core.analysis;

public record AnalysisRunnerOptions(RunnerType runnerType, OnsetAwareRunnerOptions onsetAwareRunnerOptions) {

    public static AnalysisRunnerOptions DEFAULT = new AnalysisRunnerOptions(RunnerType.DEFAULT, OnsetAwareRunnerOptions.DEFAULT);

    public enum RunnerType {
        DEFAULT,
        ONSET
    }

    public record OnsetAwareRunnerOptions(boolean strict) {
        public static OnsetAwareRunnerOptions DEFAULT = new OnsetAwareRunnerOptions(false);
    }
}
