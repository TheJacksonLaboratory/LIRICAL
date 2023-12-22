package org.monarchinitiative.lirical.core.sanitize;

/**
 * A sanitizer that does nothing.
 */
class NoOpInputSanitizer implements InputSanitizer {

    private static final NoOpInputSanitizer INSTANCE = new NoOpInputSanitizer();
    private NoOpInputSanitizer(){}

    static NoOpInputSanitizer getInstance() {
        return INSTANCE;
    }

    @Override
    public SanitationResult sanitize(SanitationInputs inputs) {
        return new SanitationResultNotRun(inputs);
    }
}
