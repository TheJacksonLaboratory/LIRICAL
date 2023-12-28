package org.monarchinitiative.lirical.cli.cmd.util;

import org.monarchinitiative.lirical.cli.cmd.ValidationPolicy;
import org.monarchinitiative.lirical.core.sanitize.SanitationResult;

public class Util {
    private Util(){}

    public static boolean phenopacketIsEligibleForAnalysis(SanitationResult result, ValidationPolicy validationPolicy) {
        return switch (validationPolicy) {
            case STRICT -> !result.hasErrorOrWarnings();
            case LENIENT, MINIMAL -> !result.hasErrors();
        };
    }
}
