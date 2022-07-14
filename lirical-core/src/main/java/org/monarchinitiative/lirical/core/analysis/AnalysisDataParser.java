package org.monarchinitiative.lirical.core.analysis;

import java.io.InputStream;

public interface AnalysisDataParser {

    AnalysisData parse(InputStream is) throws LiricalParseException;

}
