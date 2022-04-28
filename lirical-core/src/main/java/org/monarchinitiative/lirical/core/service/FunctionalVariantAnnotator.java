package org.monarchinitiative.lirical.core.service;

import org.monarchinitiative.lirical.core.model.TranscriptAnnotation;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;

public interface FunctionalVariantAnnotator {

    List<TranscriptAnnotation> annotate(GenomicVariant variant);

}
