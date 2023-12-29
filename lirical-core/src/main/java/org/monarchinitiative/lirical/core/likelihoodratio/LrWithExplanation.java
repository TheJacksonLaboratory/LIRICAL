package org.monarchinitiative.lirical.core.likelihoodratio;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
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

    @JsonGetter(value = "query")
    public TermId queryTerm() {
        return queryTerm;
    }

    @JsonGetter(value = "match")
    public TermId matchingTerm() {
        return matchingTerm;
    }

    @JsonGetter
    public LrMatchType matchType() {
        return matchType;
    }

    @JsonGetter
    public double lr() {
        return lr;
    }

    @JsonGetter
    public String explanation() {
        return explanation;
    }

    /**
     * @return explanation text suitable for including in HTML documents
     */
    @JsonIgnore
    public String escapedExplanation() {
        return StringUtils.replaceEach(explanation, EXPLANATION_SEARCH_LIST, EXPLANATION_REPLACEMENT_LIST);
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
