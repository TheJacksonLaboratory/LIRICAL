package org.monarchinitiative.lirical.output;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;
import org.monarchinitiative.lirical.analysis.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TsvDifferential extends BaseDifferential {

    public TsvDifferential(String sampleId,
                           TermId diseaseId,
                           String diseaseName,
                           TestResult result,
                           int rank,
                           List<VisualizableVariant> variants) {
        super(sampleId, diseaseId, diseaseName, result, rank, variants);
    }

    @Override
    protected String formatPostTestProbability(double postTestProbability) {
        if (postTestProbability >0.9999) {
            return String.format("%.5f%%",100* postTestProbability);
        } else if (postTestProbability >0.999) {
            return String.format("%.4f%%",100* postTestProbability);
        } else if (postTestProbability >0.99) {
            return String.format("%.3f%%",100* postTestProbability);
        } else {
            return String.format("%.2f%%",100* postTestProbability);
        }
    }

    @Override
    protected String formatPreTestProbability(double preTestProbability) {
        if (preTestProbability < 0.001) {
            return String.format("1/%d",Math.round(1.0/ preTestProbability));
        } else {
            return String.format("%.6f", preTestProbability);
        }
    }

    private Function<VisualizableVariant, String> formatVariant() {
        return v -> String.format("%s:%d%s>%s %s pathogenicity:%.1f [%s]", v.contigName(),v.pos(),v.ref(),v.alt(),annotation2string(v.getAnnotationList().get(0)),v.getPathogenicityScore(),v.getGenotype());
    }

    private static String annotation2string(TranscriptAnnotation annotation) {
        return String.format("%s:%s:%s",annotation.getAccession(),annotation.getHgvsCdna(),annotation.getHgvsProtein());
    }

    public String getVarString() {
        return variants.stream().map(formatVariant()).collect(Collectors.joining("; "));
    }

}
