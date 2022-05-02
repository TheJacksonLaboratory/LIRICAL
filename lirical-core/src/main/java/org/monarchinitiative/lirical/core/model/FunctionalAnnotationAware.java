package org.monarchinitiative.lirical.core.model;

import java.util.List;

public interface FunctionalAnnotationAware {

    /**
     * @return list of functional variant annotations.
     */
    List<TranscriptAnnotation> annotations();

}
