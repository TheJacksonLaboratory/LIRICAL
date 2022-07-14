package org.monarchinitiative.lirical.core.likelihoodratio;

import org.apache.commons.lang.StringUtils;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * There are five possible ways that a query term can match a disease term. The likelihood ratio
 * is calculated differently for each of these match types (see {@link LrMatchType}).This class
 * captures sufficient information about each match to provide an explanation for the HTML output.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class LrWithExplanation implements Comparable<LrWithExplanation> {


    private static final String[] EXPLANATION_SEARCH_LIST = {"&", "\"", "<", ">"};
    private static final String[] EXPLANATION_REPLACEMENT_LIST = {"&amp;", "&quot;", "&lt;", "&gt;"};


    private final TermId queryTerm;
    private final TermId matchingTerm;
    private final LrMatchType matchType;
    private final double lr;
    private final String explanation;

    public static LrWithExplanation of(TermId q, TermId m, LrMatchType mt, double lr, String explanation) {
        return new LrWithExplanation(q, m, mt, lr, explanation);
    }

    private LrWithExplanation(TermId q, TermId m, LrMatchType mt, double lr, String explanation) {
        this.queryTerm = q;
        this.matchingTerm = m;
        this.matchType = mt;
        this.lr = lr;
        this.explanation = explanation;
    }

    public TermId queryTerm() {
        return queryTerm;
    }

    public TermId matchingTerm() {
        return matchingTerm;
    }

    public LrMatchType matchType() {
        return matchType;
    }

    public double lr() {
        return lr;
    }

    public String explanation() {
        return explanation;
    }

    /**
     * @return explanation text suitable for including in HTML documents
     */
    public String escapedExplanation() {
        return StringUtils.replaceEach(explanation, EXPLANATION_SEARCH_LIST, EXPLANATION_REPLACEMENT_LIST);
    }

    /**
     * @deprecated use {@link #explanation()}
     */
    @Deprecated(forRemoval = true)
    public String getExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.queryTerm).getName(), queryTerm.getValue());
        String mtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.matchingTerm).getName(), matchingTerm.getValue());
        double log10LR = Math.log10(lr);
        switch (this.matchType) {
            case EXACT_MATCH:
                return String.format("E:%s[%.3f]", qtermlabel, log10LR);
            case QUERY_TERM_SUBCLASS_OF_DISEASE_TERM:
                return String.format("Q<D:%s<%s[%.3f]", qtermlabel, mtermlabel, log10LR);
            case DISEASE_TERM_SUBCLASS_OF_QUERY:
                return String.format("D<Q:%s<%s[%.3f]", mtermlabel, qtermlabel, log10LR);
            case NON_ROOT_COMMON_ANCESTOR:
                return String.format("Q~D:%s~%s[%.3f]", qtermlabel, mtermlabel, log10LR);
            case UNUSUAL_BACKGROUND_FREQUENCY:
                return String.format("U:%s[%.3f]", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE:
                return String.format("XX:%s[%.3f]", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE:
                return String.format("XA:%s[%.3f]", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE:
                return String.format("XP:%s[%.3f]", qtermlabel, log10LR);
            case QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE:
            case NO_MATCH_BELOW_ROOT:
            default:
                return String.format("NM:%s[%.3f]", qtermlabel, log10LR);
        }
    }

    /**
     * Get versions of the string that will work in HTML
     *
     * @param ontology reference to HPO ontology
     * @return an HTML string that explains this phenotypic LR result.
     * @deprecated use {@link #escapedExplanation()}
     */
    @Deprecated(forRemoval = true)
    String getEscapedExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.queryTerm).getName(), queryTerm.getValue());
        String mtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.matchingTerm).getName(), matchingTerm.getValue());
        double log10LR = Math.log10(lr);
        switch (this.matchType) {
            case EXACT_MATCH:
                return String.format("E: %s LR: %.3f", qtermlabel, log10LR);
            case QUERY_TERM_SUBCLASS_OF_DISEASE_TERM:
                return String.format("Q&lt;D: %s&lt;%s LR: %.3f", qtermlabel, mtermlabel, log10LR);
            case DISEASE_TERM_SUBCLASS_OF_QUERY:
                return String.format("D&lt;Q: %s&lt;%s LR: %.3f", mtermlabel, qtermlabel, log10LR);
            case NON_ROOT_COMMON_ANCESTOR:
                return String.format("Q~D:%s~%s LR: %.3f", qtermlabel, mtermlabel, log10LR);
            case NO_MATCH_BELOW_ROOT:
                return String.format("NM:%s LR: %.3f", qtermlabel, log10LR);
            case QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE:
                return String.format("X:%s LR: %.3f", qtermlabel, log10LR);
            case UNUSUAL_BACKGROUND_FREQUENCY:
                return String.format("U:%s LR: %.3f", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE:
                return String.format("XX:%s LR: %.3f", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE:
                return String.format("XA:%s LR: %.3f", qtermlabel, log10LR);
            case EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE:
                return String.format("XP:%s LR: %.3f", qtermlabel, log10LR);
            default:
                return String.format("NM:%s LR: %.3f", qtermlabel, log10LR); // should never happen but needed for compiler
        }
    }

    /**
     * Sort the {@link LrWithExplanation} objects in decreasing order according to likelihood ratio score.
     * This will match the order of the bars in the SVG plot.
     *
     * @param that Other object
     * @return integer indicating sort order
     */
    @Override
    public int compareTo(LrWithExplanation that) {
        return Double.compare(lr, that.lr);
    }


}
