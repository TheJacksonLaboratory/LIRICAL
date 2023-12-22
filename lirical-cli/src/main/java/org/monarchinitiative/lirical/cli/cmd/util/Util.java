package org.monarchinitiative.lirical.cli.cmd.util;

import org.monarchinitiative.lirical.cli.cmd.FailurePolicy;
import org.monarchinitiative.lirical.core.sanitize.SanitationResult;

public class Util {
    private Util(){}

    public static boolean phenopacketIsEligibleForAnalysis(SanitationResult result, FailurePolicy failurePolicy) {
        return switch (failurePolicy) {
            case STRICT -> !result.hasErrorOrWarnings();
            case LENIENT -> !result.hasErrors();
            case KAMIKAZE -> true; // Yeeeeaaah..
        };
    }
}
