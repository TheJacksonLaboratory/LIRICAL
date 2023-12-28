package org.monarchinitiative.lirical.core.sanitize;

/**
 * Represents the severity of an issue found during input data sanitation.
 *
 * @author Daniel Danis
 */
public enum SanityLevel {

    /**
     * Serious issues in the input data and the analysis cannot be carried on.
     */
    ERROR,

    /**
     * Something is not right, and you probably should not proceed. However, the analysis will likely complete.
     */
    WARNING,
}
