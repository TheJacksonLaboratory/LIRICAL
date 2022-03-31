package org.monarchinitiative.lirical.output;

import org.monarchinitiative.exomiser.core.model.TranscriptAnnotation;

import java.util.List;

/**
 * The variant data for displaying in SVG widget.
 */
interface VisualizableVariant {

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

    List<TranscriptAnnotation> getAnnotationList();
}
