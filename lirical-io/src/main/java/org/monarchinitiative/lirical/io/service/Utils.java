package org.monarchinitiative.lirical.io.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

class Utils {

    private Utils() {
    }

    static <T, U extends Comparable<U>> Optional<T> binarySearch(List<T> items,
                                                                 Function<T, U> extractor,
                                                                 U key) {
        if (items.isEmpty())
            return Optional.empty();

        int low = 0, high = items.size();

        while (low <= high) {
            int mid = low + ((high - low) / 2);
            if (mid == items.size())
                break;
            T item = items.get(mid);
            int comparison = key.compareTo(extractor.apply(item));
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

    static <T, U extends Comparable<U>> Optional<T> binarySearch(T[] items,
                                                                 Function<T, U> extractor,
                                                                 U key) {
        if (items.length == 0)
            return Optional.empty();

        int low = 0, high = items.length;

        while (low <= high) {
            int mid = low + ((high - low) / 2);
            if (mid == items.length)
                break;
            T item = items[mid];
            int comparison = key.compareTo(extractor.apply(item));
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
