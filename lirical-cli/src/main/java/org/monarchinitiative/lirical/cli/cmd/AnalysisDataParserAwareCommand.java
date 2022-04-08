package org.monarchinitiative.lirical.cli.cmd;

import org.monarchinitiative.lirical.configuration.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.exception.LiricalParseException;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.analysis.AnalysisDataParserFactory;

abstract class AnalysisDataParserAwareCommand extends AbstractPrioritizeCommand {

    @Override
    protected AnalysisData prepareAnalysisData(Lirical lirical) throws LiricalParseException {
        HpoTermSanitizer sanitizer = new HpoTermSanitizer(lirical.phenotypeService().hpo());
        AnalysisDataParserFactory parserFactory = new AnalysisDataParserFactory(sanitizer, lirical.variantParserFactory().orElse(null), lirical.phenotypeService().associationData());
        return prepareAnalysisData(parserFactory);
    }

    protected abstract AnalysisData prepareAnalysisData(AnalysisDataParserFactory factory) throws LiricalParseException;
}
