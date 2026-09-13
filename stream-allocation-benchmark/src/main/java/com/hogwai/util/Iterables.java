package com.hogwai.util;

import java.util.Optional;
import java.util.function.Predicate;

public final class Iterables {

    private Iterables() {
    }

    public static <T> boolean anyMatch(Iterable<T> items, Predicate<? super T> predicate) {
        for (T item : items) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean allMatch(Iterable<T> items, Predicate<? super T> predicate) {
        for (T item : items) {
            if (!predicate.test(item)) {
                return false;
            }
        }
        return true;
    }

    public static <T> boolean noneMatch(Iterable<T> items, Predicate<? super T> predicate) {
        for (T item : items) {
            if (predicate.test(item)) {
                return false;
            }
        }
        return true;
    }

    public static <T> Optional<T> findFirst(Iterable<T> items, Predicate<? super T> predicate) {
        for (T item : items) {
            if (predicate.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public static <T> T detect(Iterable<T> items, Predicate<? super T> predicate) {
        for (T item : items) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    public static <T> long count(Iterable<T> items, Predicate<? super T> predicate) {
        long count = 0L;
        for (T item : items) {
            if (predicate.test(item)) {
                count++;
            }
        }
        return count;
    }
}
