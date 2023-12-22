package org.monarchinitiative.lirical.cli.cmd;

/**
 * What to do in case of errors or imperfections in the input data.
 */
public enum ValidationPolicy {
    /**
     * Only run the analysis if no errors or warnings are found.
     */
    STRICT,

    /**
     * Run the analysis if the user input contains non-controversial issues that can be fixed automatically.
     */
    LENIENT,

    /**
     * Get out of my way! I'll run the analysis, or I'll die trying!
     */
    NONE
}
