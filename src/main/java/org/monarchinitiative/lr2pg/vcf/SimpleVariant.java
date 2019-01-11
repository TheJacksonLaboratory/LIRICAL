package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;

import java.util.List;
import java.util.Set;

import static org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData.ClinSig.NOT_PROVIDED;

/**
 * This class encapsulates only as much data about a variant as we need to run the algoroithm and
 * display the result
 * @author <a href="mailto:peter.robinson@jax.org">Peter Robinson</a>
 */
public class SimpleVariant implements Comparable<SimpleVariant> {
    /** A set of interpretation classes from ClinVar that we will regard as pathogenic. */
    private static final Set<ClinVarData.ClinSig> PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS =
            Sets.immutableEnumSet(ClinVarData.ClinSig.PATHOGENIC,
                    ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC,
                    ClinVarData.ClinSig.LIKELY_PATHOGENIC);
    /** The threshold predicted pathogenicity score for being in the pathogenic bin. */
    private static final float PATHOGENICITY_THRESHOLD=0.80f;
    /** Must be either hg19 or hg38 -- we are using this for the UCSC URL. */
    private static String genomeBuild=null;



    private final String chromosome;
    private final int position;
    private final String ref;
    private final String alt;
    private final List<TranscriptAnnotation> annotationList;
    private final float pathogenicity;
    private final float frequency;
    /** This is the exomiser-style pathogenicity score: the predicted pathogenicity multiplied by a frequency factor.*/
    private final float pathogenicityScore;
    private final ClinVarData.ClinSig clinvar;
    private final SimpleGenotype gtype;


    public SimpleVariant(int chrom, int pos, String ref, String alt, List<TranscriptAnnotation> annotlist,
                         float path, float freq, String genotypeString){
        this(chrom,pos,ref,alt,annotlist,path,freq,genotypeString,NOT_PROVIDED);
    }

    public SimpleVariant(int chrom, int pos, String ref, String alt, List<TranscriptAnnotation> annotlist,
                         float path, float freq, String genotypeString,ClinVarData.ClinSig clinv){
        this.position=pos;
        this.ref=ref;
        this.alt=alt;
        this.annotationList=ImmutableList.copyOf(annotlist);
        this.pathogenicity=path;
        this.frequency=freq;
        this.pathogenicityScore=(float)pathogenicityScore();
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


        switch (chrom) {
            case 25: this.chromosome ="chrM";break;
            case 24: this.chromosome ="chrY";break;
            case 23: this.chromosome ="chrX";break;
            default: this.chromosome =String.format("chr%d",chrom);
        }

    }

    /** This can be set so that we will correctly build the URL to view the location of mutation in UCSC. */
    public static void setGenomeBuildForUrl(GenomeAssembly assembly) {
        if (assembly.equals(GenomeAssembly.HG19)) {
            genomeBuild="hg19";
        } else if (assembly.equals(GenomeAssembly.HG38)) {
            genomeBuild="hg38";
        }
    }



    /**
     * @return true if the predicted pathogenicity of this variant is above {@link #PATHOGENICITY_THRESHOLD}.
     */
    public boolean isInPathogenicBin() {
        return this.pathogenicityScore >= PATHOGENICITY_THRESHOLD;
    }

    /**@return chromosome on which this variant is located. Returns a String such as chr1 or chrY */
    public String getChromosome() {
        return chromosome;
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

    /** This function sorts variants in descending order of pathogenicity. */
    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") SimpleVariant other){
        return Float.compare(other.pathogenicity,pathogenicity);

    }

    /** @return a string such as NM_000141.4:c.1694A>C:p.(Glu565Ala).*/
    private String annotation2string(TranscriptAnnotation ta) {
        return String.format("%s:%s:%s",ta.getAccession(),ta.getHgvsCdna(),ta.getHgvsProtein());
    }


    @Override
    public String toString() {
        return String.format("%s:%d%s>%s %s pathogenicity:%.1f [%s]", chromosome,position,ref,alt,annotation2string(annotationList.get(0)),pathogenicity,gtype);
    }

    public float getPathogenicity() {
        return pathogenicity;
    }

    public float getFrequency() {
        return frequency;
    }

    /** @return Exomiser like pathogenicity score that multiplies the predicted pathogenicity by the frequency factor. */
    private double pathogenicityScore() {
        double freqScore = Math.max(0,1-0.13533*Math.exp(100*this.frequency));
        return this.pathogenicity * freqScore;
    }



    public boolean isClinVarPathogenic() {
        return PATHOGENIC_CLINVAR_PRIMARY_INTERPRETATIONS.contains(this.clinvar);
    }

    public String getClinvar() {
        if (NOT_PROVIDED.equals(clinvar))
            return "n/a";
        else return clinvar.toString();
    }

    public SimpleGenotype getGtype() {
        return gtype;
    }

    /**
     * This method generates an HTML link to the UCSC genome browser that shows the general neighborhood of this variant
     * @return A String with an HTML link to the UCSC Genome browser.
     */
    public String getUcsc() {
        int delta=10;
        String display= String.format("%s:%d%s&gt;%s",chromosome,position,ref,alt );
        if (genomeBuild==null) {
            return display;
        } else {
            int start = position-delta;
            int end = position + ref.length() + delta;
            return String.format("<a href=\"http://genome.ucsc.edu/cgi-bin/hgTracks?db=%s&position=%s:%d-%d\" target=\"__blank\">%s</a>", genomeBuild,chromosome,start,end,display );
        }
    }
}
