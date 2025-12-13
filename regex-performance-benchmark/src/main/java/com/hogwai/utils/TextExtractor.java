package com.hogwai.utils;

public final class TextExtractor {
    private static final String EMPTY = "";

    private TextExtractor() {
    }

    /**
     * Retrieve the text part at the provided index
     * <p>
     * Example: get("google_drive_1234", '_', 2) -> "1234"
     *
     * @param text      text to inspect
     * @param separator separator
     * @return text part located at the provided index
     */
    public static String get(String text, char separator, int index) {
        if (text == null) return null;

        int start = 0;

        for (int i = 0; i < index; i++) {
            start = text.indexOf(separator, start);
            if (start == -1) {
                throw new IndexOutOfBoundsException("Index %d not found in %s".formatted(i, text));
            }
            start++;
        }

        int end = text.indexOf(separator, start);
        if (end == -1) {
            end = text.length();
        }

        return text.substring(start, end);
    }

    /**
     * Retrieve the substring after the last occurrence of a separator
     * <p>
     * Example: "facebook_1234" -> "1234"
     *
     * @param text      text to inspect
     * @param separator separator
     * @return the substring after the last occurrence of the specified string
     */
    public static String getAfterLastSeparator(String text, String separator) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }

        if (separator == null || separator.isBlank()) {
            return text;
        }

        final int pos = text.lastIndexOf(separator);

        if (pos == -1) {
            return EMPTY;
        }

        return text.substring(pos + 1);
    }
}