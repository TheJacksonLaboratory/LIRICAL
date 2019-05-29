package org.monarchinitiative.lirical.likelihoodratio;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

public class LrWithExplanation implements Comparable<LrWithExplanation> {


    enum MatchType {EXACT_MATCH,
        QUERY_TERM_SUBCLASS_OF_DISEASE_TERM,
        DISEASE_TERM_SUBCLASS_OF_QUERY,
        NON_ROOT_COMMON_ANCESTOR,
        NO_MATCH_BELOW_ROOT};



    private final TermId queryTerm;
    private final TermId matchingTerm;
    private final MatchType matchType;
    private final double LR;


    private LrWithExplanation(TermId q, TermId m, MatchType mt, double ratio){
        this.queryTerm=q;
        this.matchingTerm=m;
        matchType=mt;
        this.LR=ratio;
    }


    public static LrWithExplanation exactMatch(TermId tid, double ratio) {
        return new LrWithExplanation(tid,tid,MatchType.EXACT_MATCH, ratio);
    }


    public static LrWithExplanation queryTermSubTermOfDisease(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q,m,MatchType.QUERY_TERM_SUBCLASS_OF_DISEASE_TERM,ratio);
    }

    public static LrWithExplanation diseaseTermSubTermOfQuery(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q,m,MatchType.DISEASE_TERM_SUBCLASS_OF_QUERY,ratio);
    }

    public static LrWithExplanation nonRootCommonAncestor(TermId q, TermId m, double ratio) {
        return new LrWithExplanation(q,m,MatchType.NON_ROOT_COMMON_ANCESTOR,ratio);
    }

    public static LrWithExplanation noMatch(TermId q, double ratio) {
        return new LrWithExplanation(q,q,MatchType.NO_MATCH_BELOW_ROOT,ratio);
    }

    public boolean is_subclass() { return this.matchType.equals(MatchType.DISEASE_TERM_SUBCLASS_OF_QUERY) ||
    this.matchType.equals(MatchType.QUERY_TERM_SUBCLASS_OF_DISEASE_TERM);}


    public double getLR(){ return LR; }

    public String getExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]",ontology.getTermMap().get(this.queryTerm).getName(),queryTerm.getValue() );
        String mtermlabel = String.format("%s[%s]",ontology.getTermMap().get(this.matchingTerm).getName(),matchingTerm.getValue() );
        switch (this.matchType) {
            case EXACT_MATCH:
                return String.format("E:%s",qtermlabel);
            case QUERY_TERM_SUBCLASS_OF_DISEASE_TERM:
                return String.format("Q<D:%s<%s[%.3f]",qtermlabel,mtermlabel,LR );
            case DISEASE_TERM_SUBCLASS_OF_QUERY:
                return String.format("D<Q:%s<%s[%.3f]",mtermlabel,qtermlabel,LR );
            case NON_ROOT_COMMON_ANCESTOR:
                return String.format("Q~D:%s~%s[%.3f]",qtermlabel,mtermlabel,LR);
            case NO_MATCH_BELOW_ROOT:
                return String.format("NM:%s[%.3f]",qtermlabel,LR);
             default:
                 return String.format("NM:%s[%.3f]",qtermlabel,LR); // should never happen but needed for compiler
        }
    }

    /**
     * Get versions of the string that will work in HTML
     * @param ontology
     * @return
     */
    public String getEscapedExplanation(Ontology ontology) {
        String qtermlabel = String.format("%s[%s]",ontology.getTermMap().get(this.queryTerm).getName(),queryTerm.getValue() );
        String mtermlabel = String.format("%s[%s]",ontology.getTermMap().get(this.matchingTerm).getName(),matchingTerm.getValue() );
        switch (this.matchType) {
            case EXACT_MATCH:
                return String.format("<b>E</b>:%s[%.3f]",qtermlabel,LR);
            case QUERY_TERM_SUBCLASS_OF_DISEASE_TERM:
                return String.format("<b>Q&lt;D</b>:%s&lt;%s[%.3f]",qtermlabel,mtermlabel,LR );
            case DISEASE_TERM_SUBCLASS_OF_QUERY:
                return String.format("<b>D&lt;Q</b>:%s&lt;%s[%.3f]",mtermlabel,qtermlabel,LR );
            case NON_ROOT_COMMON_ANCESTOR:
                return String.format("<b>Q~D</b>:%s~%s[%.3f]",qtermlabel,mtermlabel,LR);
            case NO_MATCH_BELOW_ROOT:
                return String.format("<b>NM</b>:%s[%.3f]",qtermlabel,LR);
            default:
                return String.format("<b>NM</b>:%s[%.3f]",qtermlabel,LR); // should never happen but needed for compiler
        }
    }

    /**
     * Sort the {@link LrWithExplanation} objects in decreasing order according to likelihood ratio score.
     * This will match the order of the bars in the SVG plot.
     * @param that Other object
     * @return integer indicating sort order
     */
    @Override
    public int compareTo(LrWithExplanation that) {
        if (LR>that.LR) return -1;
        else if (LR<that.LR) return 1;
        else return 0;
    }



}
