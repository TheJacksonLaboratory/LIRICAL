package org.monarchinitiative.lirical.analysis;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.lirical.vcf.SimpleGenotype;
import org.monarchinitiative.lirical.vcf.SimpleVariant;
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
@Deprecated(forRemoval = true)
public class Gene2Genotype {


    /** The NCBI Entrez Gene ID of this gene. */
    private final TermId geneId;
    /** The symbol of this gene. */
    private final String symbol;
    /** List of all of the variants found in this gene. */
    private final List<SimpleVariant> varList;
    /** Sum of variants in the pathogenic bin, weighted by their predicted pathogenicity. */
    private double sumOfPathBinScores;
    /** It simplifies the use of this class to have an object that indicates that NO VARIANT
     * was found in the gene (no variant in the gene was present in teh VCF file).    */
    public static final Gene2Genotype NO_IDENTIFIED_VARIANT = new Gene2Genotype(TermId.of("n/a:n/a"),"n/a");

    /**
     *
     * @param id gene ID of this gene
     * @param sym symbol of this gene
     */
    public Gene2Genotype(TermId id, String sym) {
        this.geneId=id;
        this.symbol=sym;
        this.varList=new ArrayList<>();
        this.sumOfPathBinScores=0d;
    }

    public TermId getGeneId() {
        return geneId;
    }

    public String getSymbol() {
        return symbol;
    }

    public List<SimpleVariant> getVarList() {
        return varList;
    }

    public double getSumOfPathBinScores() {
        return sumOfPathBinScores;
    }


    public void addVariant(int chrom, int pos, String ref, String alt,
                           List<TranscriptAnnotation> annotList, String genotypeString, float path, float freq,ClinVarData.ClinSig clinv){
        SimpleVariant simplevar = new SimpleVariant(chrom, pos, ref, alt,  annotList, path,  freq, genotypeString,clinv);
        this.varList.add(simplevar);
        if (simplevar.isInPathogenicBin()) {
            SimpleGenotype sgenotype=simplevar.getGtype();
            if (sgenotype.equals(SimpleGenotype.HOMOZYGOUS_ALT)) {
                this.sumOfPathBinScores += 2*simplevar.getPathogenicityScore();
            } else  { // assume het
                this.sumOfPathBinScores+=simplevar.getPathogenicityScore();
            }
        }
        Collections.sort(varList); // keep variant list sorted
    }


    public boolean hasPredictedPathogenicVar() {
        return this.varList.stream().anyMatch(SimpleVariant::isInPathogenicBin);
    }

    /** @return true iff there is a variant with a pathogenic ClinVar interpretation. */
   public boolean hasPathogenicClinvarVar() {
        return this.varList.stream().anyMatch(SimpleVariant::isClinVarPathogenic);
   }

   public int pathogenicClinVarCount() {
       return this.varList.stream().map(SimpleVariant::pathogenicClinVarAlleleCount).reduce(0,Integer::sum);
   }

   public int pathogenicAlleleCount() {
       return this.varList.stream().map(SimpleVariant::pathogenicAlleleCount).reduce(0,Integer::sum);
   }

    @Override
    public String toString() {
        String varString = varList.stream().filter(SimpleVariant::isInPathogenicBin).map(SimpleVariant::toString).collect(Collectors.joining("; "));
        return String.format("%s[%s]: %s",this.symbol,this.geneId.getValue(),varString);
    }

}
