package org.monarchinitiative.lirical.core.util;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * Static utility class with the binary search implementation for arrays of items with custom key extractor function.
 */
public class BinarySearch {

    private BinarySearch() {
    }

    /**
     * Perform a binary search on an array of sorted {@link T}s using the {@code keyExtractor} function for extracting
     * the key for comparison.
     * <p>
     * The array must be sorted by the {@code keyExtractor} function. Otherwise, the behavior is undefined.
     *
     * @param haystack an array of items sorted by {@code keyExtractor} function.
     * @param keyExtractor a function for extracting a key with natural comparison order.
     * @param needle the item we are searching for.
     * @return an {@link Optional} with the found item or an empty optional if the item is not present in the array.
     * @param <T> type of the array items
     * @param <U> type of the comparison key
     */
    public static <T, U extends Comparable<U>> Optional<T> binarySearch(T[] haystack,
                                                                        Function<T, U> keyExtractor,
                                                                        U needle) {
        return binarySearch(haystack, keyExtractor, U::compareTo, needle);
    }

    /**
     * Perform a binary search on an array of sorted {@link T}s using the {@code keyExtractor} function for extracting
     * the key for comparison.
     * <p>
     * The array must be sorted by the {@code keyExtractor} and {@code comparator} functions.
     * Otherwise, the behavior is undefined.
     *
     * @param haystack an array of items sorted by {@code keyExtractor} function.
     * @param keyExtractor a function for extracting a key with natural comparison order.
     * @param comparator a function for comparing the key instances.
     * @param needle the item we are searching for.
     * @return an {@link Optional} with the found item or an empty optional if the item is not present in the array.
     * @param <T> type of the array items
     * @param <U> type of the comparison key
     */
    public static <T, U> Optional<T> binarySearch(T[] haystack,
                                                  Function<T, U> keyExtractor,
                                                  Comparator<? super U> comparator,
                                                  U needle) {
        if (haystack.length == 0)
            return Optional.empty();

        int low = 0, high = haystack.length;

        while (low <= high) {
            int mid = low + ((high - low) / 2);
            if (mid == haystack.length)
                break;
            T item = haystack[mid];
            int comparison = comparator.compare(needle, keyExtractor.apply(item));
            if (comparison == 0) {
                return Optional.ofNullable(item);
            } else if (comparison < 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        return Optional.empty();
    }
}
