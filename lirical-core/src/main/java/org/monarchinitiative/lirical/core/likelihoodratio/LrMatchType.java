package org.monarchinitiative.lirical.core.likelihoodratio;

/**
 * The likelihood ratio rules to match query term (<em>h<sub>i</sub></em>) to a disease (<em>D</em>).
 */
public enum LrMatchType {
    /**
     * <em>h<sub>i</sub></em> is identical to one of the terms to which <em>D</em> is annotated.
     */
    EXACT_MATCH,

    /**
     * <em>h<sub>i</sub></em> is a child term of one or more of the terms to which <em>D</em> is annotated.
     */
    QUERY_TERM_SUBCLASS_OF_DISEASE_TERM,

    /**
     * The proband has a phenotypic abnormality <em>h<sub>i</sub></em> that is explicitly excluded from disease <em>D</em>.
     */
    QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE,

    /**
     * <em>h<sub>i</sub></em> is an ancestor of one or more of the terms to which <em>D</em> is annotated.
     */
    DISEASE_TERM_SUBCLASS_OF_QUERY,

    /**
     * <em>h<sub>i</sub></em> and some term to which <em>D</em> is annotated have a non-root common ancestor.
     */
    NON_ROOT_COMMON_ANCESTOR,

    /**
     * Should never happen, but protect against divide by zero if there is some error.
     */
    UNUSUAL_BACKGROUND_FREQUENCY,

    /**
     * Unlike {@link #EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE} here the disease is neither annotated to the query
     * nor is the query excluded in the disease.
     */
    EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE,

    /**
     * The proband was shown not to have a phenotypic abnormality <em>h<sub>i</sub></em> that is explicitly excluded
     * from disease <em>D</em>.
     */
    EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE,

    /**
     * If the disease <em>D</em> is actually not annotated to the query term <em>h<sub>i</sub></em>,
     * but <em>h<sub>i</sub></em> was ruled out in the individual, we model this as the <em>1-f</em>,
     * where <em>f</em> is the frequency of <em>h<sub>i</sub></em> in <em>D</em>.
     */
    EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE,

    /**
     * <em>h<sub>i</sub></></em> does not have any non-root common ancestor with any term to which <em>D</em> is annotated.
     */
    NO_MATCH_BELOW_ROOT
}
