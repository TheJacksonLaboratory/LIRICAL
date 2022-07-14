package org.monarchinitiative.lirical.core.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum GenomeBuild {

    HG19,
    HG38;

    // Matches GRCh37.p13, GRCh38.p13, ..
    private static final Pattern GRCH = Pattern.compile("GRCh(?<release>\\d{2})(?<patch>\\.p\\d{2})?");
    private static final Pattern HG = Pattern.compile("hg(?<release>\\d{2})");

    public static Optional<GenomeBuild> parse(String payload) {
        Matcher grchMatcher = GRCH.matcher(payload);
        if (grchMatcher.matches()) {
            return switch (grchMatcher.group("release")) {
                case "37" -> Optional.of(HG19);
                case "38" -> Optional.of(HG38);
                default -> Optional.empty();
            };
        }

        Matcher hgMatcher = HG.matcher(payload);
        if (hgMatcher.matches()) {
            return switch (hgMatcher.group("release")) {
                case "19", "37" -> Optional.of(HG19);
                case "38" -> Optional.of(HG38);
                default -> Optional.empty();
            };
        }

        return Optional.empty();
    }
}
