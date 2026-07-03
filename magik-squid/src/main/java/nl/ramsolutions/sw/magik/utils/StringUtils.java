package nl.ramsolutions.sw.magik.utils;

/** Utils for {@link String}s. */
public final class StringUtils {

  private StringUtils() {}

  /**
   * Compute the Levenshtein (edit) distance between two strings: the minimum number of
   * single-character insertions, deletions or substitutions needed to turn one into the other.
   *
   * @param lhs Left-hand side string.
   * @param rhs Right-hand side string.
   * @return The edit distance.
   */
  public static int levenshteinDistance(final String lhs, final String rhs) {
    int[] previous = new int[rhs.length() + 1];
    for (int j = 0; j <= rhs.length(); j++) {
      previous[j] = j;
    }

    for (int i = 1; i <= lhs.length(); i++) {
      final int[] current = new int[rhs.length() + 1];
      current[0] = i;
      for (int j = 1; j <= rhs.length(); j++) {
        final int cost = lhs.charAt(i - 1) == rhs.charAt(j - 1) ? 0 : 1;
        current[j] =
            Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
      }
      previous = current;
    }

    return previous[rhs.length()];
  }
}
