package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.io.InputStream;

@Deprecated(forRemoval = true)
public interface AnalysisDataParser {

    /**
     * @deprecated use {@link #parse(InputStream, GenomeBuild, TranscriptDatabase)} instead.
     */
    // REMOVE(v2.0.0)
    @Deprecated(forRemoval = true)
    default AnalysisData parse(InputStream is) throws LiricalParseException {
        return parse(is, GenomeBuild.HG38, TranscriptDatabase.REFSEQ);
    }

    AnalysisData parse(InputStream is, GenomeBuild build, TranscriptDatabase transcriptDatabase) throws LiricalParseException;

}
