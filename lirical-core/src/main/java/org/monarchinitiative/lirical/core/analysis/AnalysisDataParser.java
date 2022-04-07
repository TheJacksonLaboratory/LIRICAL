package org.monarchinitiative.lirical.core.analysis;

import org.monarchinitiative.lirical.core.exception.LiricalParseException;

import java.io.InputStream;

public interface AnalysisDataParser {

    AnalysisData parse(InputStream is) throws LiricalParseException;

}
