package org.monarchinitiative.lr2pg.analysis;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lr2pg.vcf.SimpleVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class Genotype {
    private final TermId geneId;
    private final String symbol;


    public Genotype(TermId id, String sym) {
        this.geneId=id;
        this.symbol=sym;
       // System.err.println(sym +"["+geneId.getIdWithPrefix()+"]");
    }


    public void addVariant(int chrom, int pos, String ref, String alt,
                           List<TranscriptAnnotation> transcriptAnnotationList, String genotypeString){
        SimpleVariant simplevar = new SimpleVariant(chrom, pos, ref, alt,  annotlist, path,  freq);


    }




}
