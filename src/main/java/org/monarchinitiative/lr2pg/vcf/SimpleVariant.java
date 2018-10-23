package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import de.charite.compbio.jannovar.mendel.Genotype;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;

import java.util.List;
import java.util.Set;

/**
 * This class encapsulates only as much data about a variant as we need to run the algoroithm and
 * display the result
 */
public class SimpleVariant implements Comparable<SimpleVariant> {


    /** A set of interpretation classes from ClinVar that we will regard as pathogenic. */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);

    private static final float PATHOGENICITY_THRESHOLD=0.8f;


    private final int chromAsInt;
    private final int position;
    private final String ref;
    private final String alt;
    private final List<TranscriptAnnotation> annotationList;
    private final float pathogenicity;
    private final float frequency;
    private final ClinVarData.ClinSig clinvar;
    private final SimpleGenotype gtype;

    public SimpleVariant(int chrom, int pos, String ref, String alt, List<TranscriptAnnotation> annotlist,
                         float path, float freq, String genotypeString){
        this(chrom,pos,ref,alt,annotlist,path,freq,genotypeString,ClinVarData.ClinSig.NOT_PROVIDED);
    }

    public SimpleVariant(int chrom, int pos, String ref, String alt, List<TranscriptAnnotation> annotlist,
                         float path, float freq, String genotypeString,ClinVarData.ClinSig clinv){
        this.chromAsInt=chrom;
        this.position=pos;
        this.ref=ref;
        this.alt=alt;
        this.annotationList=ImmutableList.copyOf(annotlist);
        this.pathogenicity=path;
        this.frequency=freq;
        this.clinvar=clinv;
        switch (genotypeString) {
            case "0/1":
            case "0|1":
                this.gtype=SimpleGenotype.HETEROZYGOUS;
                break;
            case "1/1":
            case "1|1":
                this.gtype=SimpleGenotype.HOMOZYGOUS_ALT;
                break;
            case "0/0":
            case "0|0":
                this.gtype=SimpleGenotype.HOMOZYGOUS_REF;
                break;
            default:
                this.gtype=SimpleGenotype.NOT_OBSERVED;
        }
    }

    /**
     * @return true if the predicted pathogenicity of this variant is above {@link #PATHOGENICITY_THRESHOLD}.
     */
    public boolean isInPathogenicBin() {
        return this.pathogenicity >= PATHOGENICITY_THRESHOLD;
    }



    @Override
    public int compareTo(SimpleVariant other){
        if (pathogenicity>other.pathogenicity) return 1;
        else if (pathogenicity<other.pathogenicity) return -1;
        else return 0;
    }


    @Override
    public String toString() {
        return String.format("chr-%d:%d%s>%s %s %.1f %s",chromAsInt,position,ref,alt,annotationList.get(0),pathogenicity,gtype);
    }

}
