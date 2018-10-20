package org.monarchinitiative.lr2pg.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class Genotype {
    private final TermId geneId;
    private final String symbol;


    public Genotype(TermId id, String sym) {
        this.geneId=id;
        this.symbol=sym;
       // System.err.println(sym +"["+geneId.getIdWithPrefix()+"]");
    }



}
