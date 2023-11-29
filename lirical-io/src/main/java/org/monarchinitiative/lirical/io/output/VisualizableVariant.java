package org.monarchinitiative.lirical.io.output;

import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;

import java.util.List;

/**
 * The variant data for displaying in SVG widget.
 */
public interface VisualizableVariant {
    // Note: the interface MUST be public, otherwise FreeMarker will not work.

    static VisualizableVariant of(String sampleId, LiricalVariant lv, boolean isPassingPathogenicityThreshold) {
        return new VisualizableVariantDefault(sampleId, lv, isPassingPathogenicityThreshold);
    }

    String contigName();

    int pos();

    String ref();

    String alt();

    String getUcsc();

    boolean isPassingPathogenicThreshold();

    float getPathogenicityScore();

    float getFrequency();

    String getGenotype();

    String getClinvar();

    List<TranscriptAnnotation> getAnnotationList();
}
