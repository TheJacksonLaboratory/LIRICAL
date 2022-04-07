package org.monarchinitiative.lirical.io.analysis;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.exception.LiricalParseException;
import org.monarchinitiative.lirical.core.service.HpoTermSanitizer;
import org.monarchinitiative.lirical.io.VariantParserFactory;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

abstract class BasePhenopacketAnalysisDataParser<BUILDER extends Message.Builder> extends SanitizingAnalysisDataParser {

    protected static final JsonFormat.Parser PARSER = JsonFormat.parser();

    protected BasePhenopacketAnalysisDataParser(HpoTermSanitizer sanitizer,
                                                VariantParserFactory variantParserFactory,
                                                HpoAssociationData associationData) {
        super(sanitizer, variantParserFactory, associationData);
    }


    @Override
    public AnalysisData parse(InputStream is) throws LiricalParseException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            BUILDER builder = getBuilder();
            PARSER.merge(reader, builder);
            return mapToAnalysisData(builder);
        } catch (IOException e) {
            throw new LiricalParseException(e);
        }
    }

    protected abstract BUILDER getBuilder();

    protected abstract AnalysisData mapToAnalysisData(BUILDER builder) throws LiricalParseException;

}
