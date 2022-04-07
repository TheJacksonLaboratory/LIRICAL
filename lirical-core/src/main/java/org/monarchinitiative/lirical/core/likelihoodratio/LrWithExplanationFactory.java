package org.monarchinitiative.lirical.core.likelihoodratio;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

public class LrWithExplanationFactory {

    private final Ontology ontology;

    public LrWithExplanationFactory(Ontology ontology) {
        this.ontology = ontology;
    }

    public LrWithExplanation create(TermId term, LrMatchType matchType, double lr) {
        return create(term, term, matchType, lr);
    }
    public LrWithExplanation create(TermId queryTerm, TermId matchingTerm, LrMatchType matchType, double lr) {
        String explanation = getExplanation(queryTerm, matchingTerm, matchType, lr);
        return LrWithExplanation.of(queryTerm, matchingTerm, matchType, lr, explanation);
    }

    private String getExplanation(TermId queryTerm, TermId matchingTerm, LrMatchType matchType, double lr) {
        String queryTermLabel = String.format("%s[%s]", ontology.getTermMap().get(queryTerm).getName(), queryTerm.getValue());
        String matchTermLabel = String.format("%s[%s]", ontology.getTermMap().get(matchingTerm).getName(), matchingTerm.getValue());
        double log10LR = Math.log10(lr);
        return switch (matchType) {
            case EXACT_MATCH -> String.format("E:%s[%.3f]", queryTermLabel, log10LR);
            case QUERY_TERM_SUBCLASS_OF_DISEASE_TERM -> String.format("Q<D:%s<%s[%.3f]", queryTermLabel, matchTermLabel, log10LR);
            case DISEASE_TERM_SUBCLASS_OF_QUERY -> String.format("D<Q:%s<%s[%.3f]", matchTermLabel, queryTermLabel, log10LR);
            case NON_ROOT_COMMON_ANCESTOR -> String.format("Q~D:%s~%s[%.3f]", queryTermLabel, matchTermLabel, log10LR);
            case UNUSUAL_BACKGROUND_FREQUENCY -> String.format("U:%s[%.3f]", queryTermLabel, log10LR);
            case EXCLUDED_QUERY_TERM_EXCLUDED_IN_DISEASE -> String.format("XX:%s[%.3f]", queryTermLabel, log10LR);
            case EXCLUDED_QUERY_TERM_NOT_PRESENT_IN_DISEASE -> String.format("XA:%s[%.3f]", queryTermLabel, log10LR);
            case EXCLUDED_QUERY_TERM_PRESENT_IN_DISEASE -> String.format("XP:%s[%.3f]", queryTermLabel, log10LR);
            case QUERY_TERM_PRESENT_BUT_EXCLUDED_IN_DISEASE, NO_MATCH_BELOW_ROOT -> String.format("NM:%s[%.3f]", queryTermLabel, log10LR);
        };
    }
}
