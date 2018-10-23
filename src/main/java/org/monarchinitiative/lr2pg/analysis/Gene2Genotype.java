package org.monarchinitiative.lr2pg.analysis;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.lr2pg.vcf.SimpleVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class collects and organizes the variants found to be present in a given gene.
 * It provides functions that can be used to calculate the genotype likelihood ratio.
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class Gene2Genotype {
    private final TermId geneId;
    private final String symbol;
    /** List of all of the variants found in this gene. */
    private List<SimpleVariant> varList;


    public Gene2Genotype(TermId id, String sym) {
        this.geneId=id;
        this.symbol=sym;
        this.varList=new ArrayList<>();
    }


    public void addVariant(int chrom, int pos, String ref, String alt,
                           List<TranscriptAnnotation> annotList, String genotypeString, float path, float freq){
        SimpleVariant simplevar = new SimpleVariant(chrom, pos, ref, alt,  annotList, path,  freq, genotypeString);
        this.varList.add(simplevar);
        System.err.println("##### " + genotypeString +" ########");

    }

    public void addVariant(int chrom, int pos, String ref, String alt,
                           List<TranscriptAnnotation> annotList, String genotypeString, float path, float freq,ClinVarData.ClinSig clinv){
        SimpleVariant simplevar = new SimpleVariant(chrom, pos, ref, alt,  annotList, path,  freq, genotypeString,clinv);
        this.varList.add(simplevar);
        System.err.println("##### " + genotypeString +" ########");
    }


    public void sortVariants() {
        Collections.sort(varList);
    }

    @Override
    public String toString() {
        String varString = varList.stream().map(SimpleVariant::toString).collect(Collectors.joining("; "));
        return String.format("%s[%s]: %s",this.symbol,this.geneId.getIdWithPrefix(),varString);
    }

}
