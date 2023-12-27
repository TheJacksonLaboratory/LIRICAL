package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Shared functions for {@link InputSanitizer}s.
 *
 * @author Daniel Danis
 */
abstract class BaseInputSanitizer implements InputSanitizer {

    protected final MinimalOntology hpo;

    BaseInputSanitizer(MinimalOntology hpo) {
        this.hpo = Objects.requireNonNull(hpo);
    }

    protected void checkCuriesArePresentInHpo(List<TermId> termIds, List<SanityIssue> issues) {
        List<Integer> toRemove = new ArrayList<>();
        int i = 0;
        for (TermId termId : termIds) {
            if (!hpo.containsTermId(termId)) {
                issues.add(SanityIssue.warning(
                        "Term %s does not exist in HPO version %s".formatted(termId.getValue(), hpo.version().orElse("UNKNOWN")),
                        "Consider updating HPO or explore the HPO browser to choose alternative term"));
                toRemove.add(i);
            }
            i++;
        }
        BaseInputSanitizer.removeElements(termIds, toRemove);
    }

    protected static void removeElements(List<TermId> termIds, Collection<Integer> toRemove) {
        toRemove.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .mapToInt(idx -> idx)
                .forEachOrdered(termIds::remove);
    }

    protected void checkTermsUsePrimaryIdentifiers(List<TermId> termIds, List<SanityIssue> issues) {
        List<TermId> replacements = new ArrayList<>(termIds.size());
        for (TermId termId : termIds) {
            Term term = hpo.termForTermId(termId)
                    .orElseThrow(() -> new RuntimeException("%s should be a term from HPO at this point".formatted(termId.getValue())));

            TermId primary = term.id();
            if (termId.equals(primary)) {
                replacements.add(null);
            } else {
                issues.add(SanityIssue.warning(
                        "%s is an obsolete id of %s".formatted(termId.getValue(), term.getName()),
                        "Use %s instead".formatted(primary.getValue())));
                replacements.add(primary);
            }
        }

        for (int i = 0; i < replacements.size(); i++) {
            TermId replacement = replacements.get(i);
            if (replacement != null)
                termIds.set(i, replacement);
        }
    }

    protected void checkVcf(String vcf, SanitizedInputs sanitized, List<SanityIssue> issues) {
        if (vcf != null) {
            Path path = Path.of(vcf);
            if (Files.isRegularFile(path) && Files.isReadable(path)) {
                sanitized.setVcf(path);
            } else {
                issues.add(SanityIssue.error(
                        "VCF path is set but %s does not point to a readable file".formatted(path.toAbsolutePath()),
                        "Update the path or the file permissions"));
            }

        }
    }

    protected static void checkCuriesAreWellFormed(SanitizedInputs sanitized,
                                                   List<String> inputPresentTermIds,
                                                   List<String> inputExcludedTermIds,
                                                   List<SanityIssue> issues) {
        if (inputPresentTermIds.isEmpty() && inputExcludedTermIds.isEmpty()) {
            issues.add(SanityIssue.error("No HPO terms were provided", "Add at least 1 HPO term to start"));
        } else {
            // We can check if the present terms are valid.
            for (String curie : inputPresentTermIds) {
                checkCurieIsValid(curie, sanitized.presentHpoTerms(), issues);
            }

            // We can check if the excluded term IDs are valid.
            for (String curie : inputExcludedTermIds) {
                checkCurieIsValid(curie, sanitized.excludedHpoTerms(), issues);
            }
        }
    }

    private static void checkCurieIsValid(String curie,
                                          List<TermId> termIds,
                                          List<SanityIssue> issues) {
        try {
            termIds.add(TermId.of(curie));
        } catch (PhenolRuntimeException e) {
            issues.add(SanityIssue.warning(
                    "The term ID %s is invalid: %s".formatted(curie, e.getMessage()),
                    "Ensure the term ID consists of a valid prefix (e.g. `HP`) and id (e.g. `0001250`) " +
                            "joined by colon `:` or underscore `_`."));
        }
    }
}
