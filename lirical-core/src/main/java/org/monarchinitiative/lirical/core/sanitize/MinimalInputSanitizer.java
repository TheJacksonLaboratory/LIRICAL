package org.monarchinitiative.lirical.core.sanitize;

import org.monarchinitiative.lirical.core.model.Age;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal sanitizer performs as few checks as possible.
 * <p>
 * The HPO terms are checked if they are well-formed CURIEs that exist in given HPO. Obsolete term IDs are replaced
 * with the current term IDs.
 * <p>
 * If path to VCF is set, then it must point to a readable file.
 *
 * @author Daniel Danis
 */
class MinimalInputSanitizer extends BaseInputSanitizer {

    MinimalInputSanitizer(MinimalOntology hpo) {
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

        // Convert the age and sex if possible, or ignore.
        sanitized.setAge(parseAgeOrNull(inputs.age()));
        sanitized.setSex(parseSexOrNull(inputs.sex()));

        //
        checkVcf(inputs.vcf(), sanitized, issues);


        return new SanitationResultDefault(sanitized, issues);
    }

    private static Age parseAgeOrNull(String age) {
        try {
            return Age.parse(Period.parse(age));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static Sex parseSexOrNull(String sex) {
        try {
            return Sex.valueOf(sex.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Check that CURIEs are present in HPO and upgrade to primary identifier if the obsolete term is being used.
     */
    private void checkPhenotypicFeatures(SanitizedInputs sanitized, List<SanityIssue> issues) {
        checkCuriesArePresentInHpo(sanitized.presentHpoTerms(), issues);
        checkCuriesArePresentInHpo(sanitized.excludedHpoTerms(), issues);

        checkTermsUsePrimaryIdentifiers(sanitized.presentHpoTerms(), issues);
        checkTermsUsePrimaryIdentifiers(sanitized.excludedHpoTerms(), issues);
    }
}
