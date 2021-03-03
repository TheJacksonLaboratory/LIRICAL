package org.monarchinitiative.lirical.likelihoodratio;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * There are five possible ways that a query term can match a disease term. The likelihood ratio
 * is calculated differently for each of these match types (see {@link MatchType}).This class
 * captures sufficient information about each match to provide an explanation for the HTML output.
 *
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class LrWithExplanation implements Comparable<LrWithExplanation> {


    enum MatchType {
        EXACT_MATCH, QUERY_TERM_SUBCLASS_OF_DISEASE_TERM, QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE, DISEASE_TERM_SUBCLASS_OF_QUERY, NON_ROOT_COMMON_ANCESTOR, UNUSUAL_BACKGROUND_FREQUENCY, EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE, EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE, EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE, NO_MATCH_BELOW_ROOT
    }


    private final TermId queryTerm;
    private final TermId matchingTerm;
    private final MatchType matchType;
    private final double LR;


    private LrWithExplanation(TermId q, TermId m, MatchType mt, double ratio) {
        this.queryTerm = q;
        this.matchingTerm = m;
        matchType = mt;
        this.LR = ratio;
    }


    static LrWithExplanation exactMatch(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.EXACT_MATCH, ratio);
    }

    static LrWithExplanation unusualBackgroundFrequency(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.UNUSUAL_BACKGROUND_FREQUENCY, ratio);
    }

    static LrWithExplanation queryTermExcluded(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE, ratio);
    }

    static LrWithExplanation excludedQueryTermNotPresentInDisease(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE, ratio);
    }

    static LrWithExplanation excludedQueryTermPresentInDisease(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE, ratio);
    }

    static LrWithExplanation excludedQueryTermEcludedInDisease(TermId tid, double ratio) {
        return new LrWithExplanation(tid, tid, MatchType.EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE, ratio);
    }


    static LrWithExplanation queryTermSubTermOfDisease(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q, m, MatchType.QUERY_TERM_SUBCLASS_OF_DISEASE_TERM, ratio);
    }

    static LrWithExplanation diseaseTermSubTermOfQuery(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q, m, MatchType.DISEASE_TERM_SUBCLASS_OF_QUERY, ratio);
    }

    static LrWithExplanation nonRootCommonAncestor(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q, m, MatchType.NON_ROOT_COMMON_ANCESTOR, ratio);
    }

    static LrWithExplanation noMatch(TermId q, double ratio) {
        return new LrWithExplanation(q, q, MatchType.NO_MATCH_BELOW_ROOT, ratio);
    }


    public double getLR() {
        return LR;
    }

    public String getExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.queryTerm).getName(), queryTerm.getValue());
        String mtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.matchingTerm).getName(), matchingTerm.getValue());
        double log10LR = Math.log10(LR);
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
     */
    String getEscapedExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.queryTerm).getName(), queryTerm.getValue());
        String mtermlabel = String.format("%s[%s]", ontology.getTermMap().get(this.matchingTerm).getName(), matchingTerm.getValue());
        double log10LR = Math.log10(LR);
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
        return Double.compare(LR, that.LR);
    }


}
