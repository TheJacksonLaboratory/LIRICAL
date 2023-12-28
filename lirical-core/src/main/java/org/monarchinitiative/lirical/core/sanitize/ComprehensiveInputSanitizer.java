package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class ComprehensiveInputSanitizer extends BaseInputSanitizer {

    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    public ComprehensiveInputSanitizer(MinimalOntology hpo){
        super(hpo);
    }

    @Override
    public SanitationResult sanitize(SanitationInputs inputs) {
        List<SanityIssue> issues = new ArrayList<>();
        // sampleId is nullable, nothing to be checked there at this point.
        SanitizedInputs sanitized = new SanitizedInputs(inputs.sampleId());

        // Check phenotypic features
        checkCuriesAreWellFormed(sanitized, inputs.presentHpoTerms(), inputs.excludedHpoTerms(), issues);
        checkPhenotypicFeatures(sanitized, issues);

        checkAge(inputs.age(), sanitized, issues);
        checkSex(inputs.sex(), sanitized, issues);

        checkVcf(inputs.vcf(), sanitized, issues);

        return new SanitationResultDefault(sanitized, issues);
    }

    private void checkPhenotypicFeatures(SanitizedInputs sanitized, List<SanityIssue> issues) {
        checkTermsAreUnique(sanitized.presentHpoTerms(), issues);
        checkTermsAreUnique(sanitized.excludedHpoTerms(), issues);

        checkCuriesArePresentInHpo(sanitized.presentHpoTerms(), issues);
        checkCuriesArePresentInHpo(sanitized.excludedHpoTerms(), issues);

        checkTermsUsePrimaryIdentifiers(sanitized.presentHpoTerms(), issues);
        checkTermsUsePrimaryIdentifiers(sanitized.excludedHpoTerms(), issues);

        checkTermsAreDescendantsOfPhenotypicAbnormality(sanitized.presentHpoTerms(), issues);
        checkTermsAreDescendantsOfPhenotypicAbnormality(sanitized.excludedHpoTerms(), issues);

        checkTermsAreLogicallyConsistent(sanitized, issues);
    }

    private void checkTermsAreUnique(List<TermId> termIds, List<SanityIssue> issues) {
        Map<TermId, Long> termCounts = termIds.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        List<TermId> toClean = new ArrayList<>();
        for (Map.Entry<TermId, Long> e : termCounts.entrySet()) {
            if (e.getValue() > 1) {
                issues.add(SanityIssue.warning(
                        "Term should be used at most once but %s is used %d times".formatted(e.getKey().getValue(), e.getValue()),
                        "Use a term at most once"));
                toClean.add(e.getKey());
            }
        }

        for (TermId termId : toClean) {
            // Find indices to for removal.
            boolean found = false;
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < termIds.size(); i++) {
                TermId t = termIds.get(i);
                if (t.equals(termId)) {
                    if (!found) {
                        found = true;
                    } else {
                        toRemove.add(i);
                    }
                }
            }

            // And then remove the terms
            removeElements(termIds, toRemove);
        }
    }

    private void checkTermsAreDescendantsOfPhenotypicAbnormality(List<TermId> termIds, List<SanityIssue> issues) {
        List<Integer> toRemove = new ArrayList<>();
        int i = 0;
        for (TermId termId : termIds) {
            if (!termId.equals(PHENOTYPIC_ABNORMALITY) && !hpo.graph().existsPath(termId, PHENOTYPIC_ABNORMALITY)) {
                Term term = hpo.termForTermId(termId)
                        .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(termId.getValue())));
                issues.add(SanityIssue.warning(
                        "Term %s is not a descendant of Phenotypic abnormality".formatted(
                                formatTerm(term)),
                        "Consider removing %s from the phenotypic features".formatted(formatTerm(term))));
                toRemove.add(i);
            }
            i++;
        }
        removeElements(termIds, toRemove);
    }

    private void checkTermsAreLogicallyConsistent(SanitizedInputs sanitized, List<SanityIssue> issues) {
        pruneExcludedHpoTerms(sanitized.excludedHpoTerms(), issues);
        prunePresentHpoTerms(sanitized.presentHpoTerms(), issues);

        checkNoPresentFeatureHasExcludedAncestor(sanitized, issues);
    }

    private void pruneExcludedHpoTerms(List<TermId> excludedTerms,
                                       List<SanityIssue> issues) {
        // Check the excluded features use the most general term.
        // All terms whose ancestor is among excluded term ids must be removed.
        List<Integer> toRemove = new ArrayList<>();
        int i = 0;
        for (TermId termId : excludedTerms) {
            for (TermId other : excludedTerms) {
                if (!termId.equals(other) && hpo.graph().existsPath(termId, other)) {
                    Term term = hpo.termForTermId(termId)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(termId.getValue())));
                    Term ancestor = hpo.termForTermId(other)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(other.getValue())));
                    issues.add(SanityIssue.warning(
                            "Sample should not be annotated with excluded %s and its excluded ancestor %s".formatted(formatTerm(term), formatTerm(ancestor)),
                            "Remove %s from the phenotype terms".formatted(formatTerm(term))));
                    toRemove.add(i);
                    break;
                }
            }
            i++;
        }

        removeElements(excludedTerms, toRemove);
    }

    private void prunePresentHpoTerms(List<TermId> presentTerms, List<SanityIssue> issues) {
        // Check the present features use the most specific term.
        // All ancestors of the present term ids must be removed.
        List<Integer> toRemove = new ArrayList<>();
        int i = 0;
        for (TermId termId : presentTerms) {
            for (TermId other : presentTerms) {
                if (!termId.equals(other) && hpo.graph().existsPath(other, termId)) {
                    Term term = hpo.termForTermId(other)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(other.getValue())));
                    Term ancestor = hpo.termForTermId(termId)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(termId.getValue())));
                    issues.add(SanityIssue.warning(
                            "Sample should not be annotated with %s and its ancestor %s".formatted(formatTerm(term), formatTerm(ancestor)),
                            "Remove %s from the phenotype terms".formatted(formatTerm(ancestor))));
                    toRemove.add(i);
                    break;
                }
            }
            i++;
        }

        removeElements(presentTerms, toRemove);
    }

    private void checkNoPresentFeatureHasExcludedAncestor(SanitizedInputs sanitized, List<SanityIssue> issues) {
        for (TermId present : sanitized.presentHpoTerms()) {
            for (TermId excluded : sanitized.excludedHpoTerms()) {
                if (present.equals(excluded)) {
                    // Term is both present and excluded.
                    Term term = hpo.termForTermId(present)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(present.getValue())));
                    issues.add(SanityIssue.error(
                            "Sample must not be annotated with %s in present and excluded state at the same time".formatted(formatTerm(term)),
                            "Make up your mind"));
                } else if (hpo.graph().getAncestorsStream(present).anyMatch(anc -> anc.equals(excluded))) {
                    // Term has an excluded ancestor.
                    Term presentTerm = hpo.termForTermId(present)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(present.getValue())));
                    Term excludedTerm = hpo.termForTermId(excluded)
                            .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(excluded.getValue())));
                    issues.add(SanityIssue.error(
                            "Sample must not be annotated with %s while its ancestor %s is excluded".formatted(
                                    formatTerm(presentTerm), formatTerm(excludedTerm)),
                            "Resolve the logical inconsistency by choosing one of the terms"));
                }
            }
        }
    }

    private static void checkAge(String age, SanitizedInputs sanitized, List<SanityIssue> issues) {
        if (age != null) {
            try {
                Period period = Period.parse(age);
                sanitized.setAge(Age.parse(period));
            } catch (DateTimeParseException e) {
                issues.add(SanityIssue.warning(
                        "Age %s could not be parsed: %s".formatted(age, e.getMessage()),
                        "Format age as a ISO8601 duration (e.g. `P22Y6M`)"));
            }
        }
    }

    private static void checkSex(String sex, SanitizedInputs sanitized, List<SanityIssue> issues) {
        if (sex != null) {
            try {
                sanitized.setSex(Sex.valueOf(sex.toUpperCase()));
            } catch (IllegalArgumentException e) {
                issues.add(SanityIssue.warning(
                        "Sex %s could not be parsed".formatted(sex),
                        "Use one of {'male', 'female', 'unknown'}"));
            }
        }
    }

    private static String formatTerm(Term term) {
        return "%s [%s]".formatted(term.getName(), term.id().getValue());
    }
}
