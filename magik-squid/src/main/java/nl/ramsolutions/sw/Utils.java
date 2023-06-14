package nl.ramsolutions.sw;

import java.util.stream.Collectors;

/**
 * Utility functions.
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Untabify a string.
     * @param text Text to untabify.
     * @param tabSize Tab size.
     * @return Untabified text.
     */
    public static String untabify(final String text, final int tabSize) {
        // From: https://stackoverflow.com/questions/11722855/expand-the-tabs-in-java/40834203#40834203
        final int[] col = new int[1];
        return text.chars()
            .mapToObj(c -> {
                switch (c) {
                    case '\t':
                        int expandBy = tabSize - col[0] % tabSize;
                        col[0] += expandBy;
                        return " ".repeat(expandBy);

                    case '\n':
                        col[0] = 0;
                        break;

                    default:
                        col[0]++;
                        break;
                }

                return String.valueOf((char) c);
            })
            .collect(Collectors.joining());
    }

}
