package org.monarchinitiative.lirical.output;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.model.AlleleCount;
import org.monarchinitiative.lirical.model.ClinvarClnSig;
import org.monarchinitiative.lirical.model.GenomeBuild;
import org.monarchinitiative.lirical.model.LiricalVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Objects;

/**
 * An adapter between Freemarker and {@link LiricalVariant}.
 */
class VisualizableVariantDefault implements VisualizableVariant {

    private final String sampleId;
    private final LiricalVariant variant;
    private final boolean isInPathogenicBin;

    VisualizableVariantDefault(String sampleId,
                               LiricalVariant variant,
                               boolean isInPathogenicBin) {
        this.sampleId = Objects.requireNonNull(sampleId);
        this.variant = Objects.requireNonNull(variant);
        this.isInPathogenicBin = isInPathogenicBin;
    }

    @Override
    public String contigName() {
        return variant.variant().contigName();
    }

    @Override
    public int pos() {
        return variant.variant().startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased());
    }

    @Override
    public String ref() {
        return variant.variant().ref();
    }

    @Override
    public String alt() {
        return variant.variant().alt();
    }

    @Override
    public String getUcsc() {
        GenomicVariant gv = variant.variant();
        int delta=10;
        String chromosome = gv.contigName();
        String display= String.format("%s:%d%s&gt;%s", chromosome, pos(),gv.ref(),gv.alt() );
        GenomeBuild genomeBuild = variant.genomeBuild();
        if (genomeBuild==null) {
            return display;
        } else {
            int start = pos()-delta;
            int end = pos() + gv.length() + delta;
            return String.format("<a href=\"https://genome.ucsc.edu/cgi-bin/hgTracks?db=%s&position=%s:%d-%d\" target=\"__blank\">%s</a>", ucscGenomeAssemblyToken(genomeBuild), chromosome, start, end, display);
        }
    }

    private static String ucscGenomeAssemblyToken(GenomeBuild genomeBuild) {
        return switch (genomeBuild) {
            case HG19 -> "hg19";
            case HG38 -> "hg38";
        };
    }

    @Override
    public boolean isInPathogenicBin() {
        return isInPathogenicBin;
    }

    @Override
    public float getPathogenicityScore() {
        return variant.pathogenicityScore().orElse(1.f);
    }

    @Override
    public float getFrequency() {
        return variant.frequency().orElse(0f);
    }

    @Override
    public String getGenotype() {
        return variant.alleleCount(sampleId)
                .map(VisualizableVariantDefault::genotypeFromAlleleCount)
                .orElse("./.");
    }

    private static String genotypeFromAlleleCount(AlleleCount ac) {
        if (ac.ref() == 2 && ac.alt() == 0) {
            // HOM_REF
            return "0/0";
        } else if (ac.ref() == 1 && ac.alt() == 1) {
            // HET
            return "0/1";
        } else if (ac.ref() == 0 && ac.alt() == 2) {
            // HOM_ALT
            return "1/1";
        } else {
            // unusual situations
            if (ac.ref() == 0 && ac.alt() == 1) {
                return "./1";
            } else if (ac.ref() == 1 && ac.alt() == 0) {
                return "./0";
            } else {
                return "./.";
            }
        }
    }

    @Override
    public String getClinvar() {
        ClinvarClnSig clnsig = variant.clinvarClnSig();
        return (clnsig.equals(ClinvarClnSig.NOT_PROVIDED))
                ? "n/a"
                : clnsig.toString();
    }

    @Override
    public List<TranscriptAnnotation> getAnnotationList() {
        return variant.annotations();
    }
}
