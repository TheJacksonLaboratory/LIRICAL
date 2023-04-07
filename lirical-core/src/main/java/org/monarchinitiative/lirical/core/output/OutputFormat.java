package org.monarchinitiative.lirical.core.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LIRICAL supports writing results in these formats.
 */
public enum OutputFormat {

    HTML,
    TSV,
    JSON;

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputFormat.class);

    @Override
    public String toString() {
        return switch (this) {
            case HTML -> "html";
            case TSV -> "tsv";
            case JSON -> "json";
        };
    }

    public static OutputFormat parse(String value) {
        return switch (value.toLowerCase()) {
            case "html":
                yield HTML;
            case "tsv":
                yield TSV;
            case "json":
                yield JSON;
            default:
                LOGGER.warn("Unknown output format {}, falling back to html", value);
                yield HTML;
        };
    }
}
