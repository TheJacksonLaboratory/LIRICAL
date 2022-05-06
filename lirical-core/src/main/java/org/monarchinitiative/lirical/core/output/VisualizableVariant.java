package org.monarchinitiative.lirical.core.output;

import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;

import java.util.List;

/**
 * The variant data for displaying in SVG widget.
 */
public interface VisualizableVariant {
    // Note: the interface MUST be public, otherwise FreeMarker will not work.

    String contigName();

    int pos();

    String ref();

    String alt();

    String getUcsc();

    boolean isInPathogenicBin();

    float getPathogenicityScore();

    float getFrequency();

    String getGenotype();

    String getClinvar();

    boolean isPassingFrequency();

    List<TranscriptAnnotation> getAnnotationList();
}
