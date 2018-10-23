package org.monarchinitiative.lr2pg.vcf;

import com.google.common.collect.ImmutableList;
import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;

import java.util.List;

/**
 * This class encapsulates only as much data about a variant as we need to run the algoroithm and
 * display the result
 */
public class SimpleVariant {
    /*
     int chrom = veval.getChromosome();
                    int pos = veval.getPosition();
                    String ref = veval.getRef();
                    String alt = veval.getAlt();
                    List<TranscriptAnnotation> transcriptAnnotationList = veval.getTranscriptAnnotations();
                    String genotypeString = veval.getGenotypeString();
                    float freq;
                    float path;
                    boolean isClinVarPath=false;
                    ClinVarData.ClinSig clinvarSig=null;
     */

    private final int chromAsInt;
    private final int position;
    private final String ref;
    private final String alt;
    private final List<TranscriptAnnotation> annotationList;

    public SimpleVariant(int chrom, int pos, String ref, String alt, List<TranscriptAnnotation> annotlist, double path, double freq){
        this.chromAsInt=chrom;
        this.position=pos;
        this.ref=ref;
        this.alt=alt;
        this.annotationList=ImmutableList.copyOf(annotlist);
    }

}
