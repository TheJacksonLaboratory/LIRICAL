package org.monarchinitiative.lirical.core.io;

import org.monarchinitiative.phenol.annotations.base.temporal.Age;
import org.monarchinitiative.phenol.annotations.base.temporal.ConfidenceRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static utility class for parsing ISO8601-like duration string into {@link Age}.
 * <p>
 * See {@link #parse(String)} for more information.
 */
public class AgeParser {

    // TODO - ISO standards in fact allows fractions in the last temporal field.

    private static final Logger LOGGER = LoggerFactory.getLogger(AgeParser.class);
    private static final Pattern GESTATIONAL_PT = Pattern.compile("^P(\\d+W)(\\d+D)?.*$");
    private static final Pattern PERIOD_PT = Pattern.compile("^P(\\d+[YMWD])+.*");
    private static final Pattern TEMPORAL_TOKEN_PT = Pattern.compile("(?<value>\\d+)(?<token>[YMWD])");

    private AgeParser() {
    }

    private enum TemporalToken {
        YEAR,
        MONTH,
        WEEK,
        DAY;

        private static final Set<TemporalToken> GESTATIONAL = Set.of(WEEK, DAY);
        private static final Set<TemporalToken> POSTNATAL = Set.of(YEAR, MONTH, DAY);
        private static TemporalToken parse(String value) {
            return switch (value) {
                case "Y" -> YEAR;
                case "M" -> MONTH;
                case "W" -> WEEK;
                case "D" -> DAY;
                default -> throw new IllegalArgumentException("Unknown token '%s'".formatted(value));
            };
        }
    }

    /**
     * Parse ISO8601 {@code duration} into {@link Age}.
     * <p>
     * If the duration uses weeks, the duration is converted into a gestational age. In that case, weeks ({@code W})
     * and days ({@code D}) are the only allowed time elements.
     * If weeks are not used, the duration is converted into a postnatal age. If this is the case, weeks are not allowed.
     * Any data located past the time designator {@code T} is ignored.
     * <p>
     * The precision of resulting {@link Age} depends on the input. For instance, duration {@code P1Y0D}
     * is more informative than a mere {@code P1Y}. The former is mapped into an imprecise {@link Age} that represents
     * 365 days. The latter is mapped into an imprecise {@link Age} too. However, the {@link ConfidenceRange} includes
     * the entire 2<sup>nd</sup> year (note that the first year has index 0).
     *
     * @param duration ISO8601 duration string
     * @return age
     * @throws AgeParseException if the {@code duration} is not a proper ISO8601 duration string
     * @throws NullPointerException if {@code duration} is {@code null}
     */
    public static Age parse(String duration) throws AgeParseException {
        Matcher periodMatcher = PERIOD_PT.matcher(Objects.requireNonNull(duration));
        if (!periodMatcher.matches())
            throw new AgeParseException("Invalid age format %s".formatted(duration));

        boolean isGestational = sniffGestational(duration);

        Map<TemporalToken, Integer> tokens = partitionTemporalTokens(duration, isGestational);

        TemporalToken mostPrecise = findTheMostPreciseTemporalToken(tokens);

        return isGestational
                ? createGestationalAge(tokens, mostPrecise)
                : createPostnatalAge(tokens, mostPrecise);
    }

    private static boolean sniffGestational(String payload) {
        boolean isGestational;
        Matcher gestationalMatcher = GESTATIONAL_PT.matcher(payload);
        if (gestationalMatcher.matches()) {
            LOGGER.debug("The age {} seems to be gestational", payload);
            isGestational = true;
        } else {
            isGestational = false;
        }
        return isGestational;
    }

    private static Map<TemporalToken, Integer> partitionTemporalTokens(String duration, boolean isGestational) throws AgeParseException {
        int tIdx = duration.indexOf("T");
        if (tIdx >= 0)
            duration = duration.substring(0, tIdx);
        Matcher tokenMatcher = TEMPORAL_TOKEN_PT.matcher(duration);
        Map<TemporalToken, Integer> tokens = new HashMap<>(TemporalToken.values().length);
        while (tokenMatcher.find()) {
            TemporalToken token = TemporalToken.parse(tokenMatcher.group("token"));
            tokens.put(token, Integer.parseInt(tokenMatcher.group("value")));
        }
        if (isGestational) {
            if (tokens.keySet().stream().anyMatch(token -> !TemporalToken.GESTATIONAL.contains(token)))
                throw new AgeParseException("Duration %s contains time elements that are not allowed for gestational age!".formatted(duration));
        } else {
            if (tokens.keySet().stream().anyMatch(token -> !TemporalToken.POSTNATAL.contains(token)))
                throw new AgeParseException("Duration %s contains time elements that are not allowed for postnatal age!".formatted(duration));
        }

        return tokens;
    }

    private static TemporalToken findTheMostPreciseTemporalToken(Map<TemporalToken, Integer> tokens) {
        TemporalToken last = null;
        for (TemporalToken token : TemporalToken.values()) {
            // We rely on the sort order of the enum.
            if (tokens.containsKey(token))
                last = token;
        }
        return last;
    }

    private static Age createGestationalAge(Map<TemporalToken, Integer> tokens, TemporalToken last) {
        return switch (last) {
            case WEEK -> Age.gestational(
                    tokens.get(TemporalToken.WEEK),
                    0,
                    ConfidenceRange.of(0, 7));
            case DAY -> Age.gestational(
                    tokens.getOrDefault(TemporalToken.WEEK, 0),
                    tokens.get(TemporalToken.DAY),
                    ConfidenceRange.of(0, 1));
            default ->
                    throw new IllegalStateException("The last token should have been WEEK or DAY, but was %s".formatted(last));
        };
    }

    private static Age createPostnatalAge(Map<TemporalToken, Integer> tokens, TemporalToken last) {
        return switch (last) {
            case YEAR -> Age.postnatal(
                    tokens.get(TemporalToken.YEAR),
                    0,
                    0,
                    ConfidenceRange.of(0, (int) Age.DAYS_IN_JULIAN_YEAR));
            case MONTH -> Age.postnatal(
                    tokens.getOrDefault(TemporalToken.YEAR, 0),
                    tokens.get(TemporalToken.MONTH),
                    0,
                    ConfidenceRange.of(0, (int) Age.DAYS_IN_MONTH));
            case DAY -> Age.postnatal(
                    tokens.getOrDefault(TemporalToken.YEAR, 0),
                    tokens.getOrDefault(TemporalToken.MONTH, 0),
                    tokens.get(TemporalToken.DAY),
                    ConfidenceRange.of(0, 1));
            default ->
                    throw new IllegalStateException("The last token should have been YEAR, MONTH, or DAY, but was %s".formatted(last));
        };
    }

}
