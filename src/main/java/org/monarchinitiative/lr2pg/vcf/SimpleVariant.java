package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
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
    /** The threshold predicted pathogenicity score for being in the pathogenic bin. */
    private static final float PATHOGENICITY_THRESHOLD=0.80f;


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


    public int getChromAsInt() {
        return chromAsInt;
    }

    public int getPosition() {
        return position;
    }

    public String getRef() {
        return ref;
    }

    public String getAlt() {
        return alt;
    }

    public List<TranscriptAnnotation> getAnnotationList() {
        return annotationList;
    }

    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") SimpleVariant other){
        return Float.compare(pathogenicity,other.pathogenicity);

    }

    /** @return a string such as NM_000141.4:c.1694A>C:p.(Glu565Ala).*/
    private String annotation2string(TranscriptAnnotation ta) {
        return String.format("%s:%s:%s",ta.getAccession(),ta.getHgvsCdna(),ta.getHgvsProtein());
    }


    @Override
    public String toString() {
        return String.format("chr%d:%d%s>%s %s pathogenicity:%.1f [%s]",chromAsInt,position,ref,alt,annotation2string(annotationList.get(0)),pathogenicity,gtype);
    }

    public float getPathogenicity() {
        return pathogenicity;
    }

    public float getFrequency() {
        return frequency;
    }

    public boolean isClinVarPathogenic() {
        return PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS.contains(this.clinvar);
    }

    public ClinVarData.ClinSig getClinvar() {
        return clinvar;
    }

    public SimpleGenotype getGtype() {
        return gtype;
    }
}