package nl.ramsolutions.sw.magik;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Myers diff algorithm for computing minimal differences between two strings.
 *
 * <p>This algorithm finds the shortest edit sequence (SES) to transform one string into another,
 * producing a list of {@link DiffEdit}s representing insertions, deletions, and unchanged regions.
 */
public class MyersDiff {

  /** Single edit operation in the diff. */
  public static class DiffEdit {
    public enum Type {
      EQUAL,
      DELETE,
      INSERT
    }

    private final Type type;
    private final String text;
    private final int startA; // Position in string A.
    private final int startB; // Position in string B.

    public DiffEdit(final Type type, final String text, final int startA, final int startB) {
      this.type = type;
      this.text = text;
      this.startA = startA;
      this.startB = startB;
    }

    public Type getType() {
      return this.type;
    }

    public String getText() {
      return this.text;
    }

    public int getStartA() {
      return this.startA;
    }

    public int getStartB() {
      return this.startB;
    }

    @Override
    public String toString() {
      return "%s@%s(%s, '%s', %d, %d)"
          .formatted(
              this.getClass().getName(),
              Integer.toHexString(this.hashCode()),
              this.type,
              this.text,
              this.startA,
              this.startB);
    }
  }

  /**
   * Compute the diff between two strings.
   *
   * @param textA The original text.
   * @param textB The modified text.
   * @return A list of {@link DiffEdit}s representing the differences.
   */
  public List<DiffEdit> diff(final String textA, final String textB) {
    final List<DiffEdit> edits = new ArrayList<>();

    // Handle empty strings.
    if (textA.isEmpty() && textB.isEmpty()) {
      return edits;
    }

    if (textA.isEmpty()) {
      edits.add(new DiffEdit(DiffEdit.Type.INSERT, textB, 0, 0));
      return edits;
    }

    if (textB.isEmpty()) {
      edits.add(new DiffEdit(DiffEdit.Type.DELETE, textA, 0, 0));
      return edits;
    }

    // Find common prefix.
    int commonPrefix = 0;
    final int minLength = Math.min(textA.length(), textB.length());
    while (commonPrefix < minLength && textA.charAt(commonPrefix) == textB.charAt(commonPrefix)) {
      commonPrefix++;
    }

    // Find common suffix.
    int commonSuffix = 0;
    final int maxSuffix = minLength - commonPrefix;
    while (commonSuffix < maxSuffix
        && textA.charAt(textA.length() - 1 - commonSuffix)
            == textB.charAt(textB.length() - 1 - commonSuffix)) {
      commonSuffix++;
    }

    // Add common prefix as EQUAL.
    if (commonPrefix > 0) {
      final DiffEdit edit =
          new DiffEdit(DiffEdit.Type.EQUAL, textA.substring(0, commonPrefix), 0, 0);
      edits.add(edit);
    }

    // Extract the middle parts that differ.
    final String middleA = textA.substring(commonPrefix, textA.length() - commonSuffix);
    final String middleB = textB.substring(commonPrefix, textB.length() - commonSuffix);

    // If middle parts exist, compute their diff using Myers algorithm.
    if (!middleA.isEmpty() || !middleB.isEmpty()) {
      final List<DiffEdit> middleEdits = this.computeMyersDiff(middleA, middleB, commonPrefix);
      edits.addAll(middleEdits);
    }

    // Add common suffix as EQUAL.
    if (commonSuffix > 0) {
      final int suffixStartA = textA.length() - commonSuffix;
      final int suffixStartB = textB.length() - commonSuffix;
      edits.add(
          new DiffEdit(
              DiffEdit.Type.EQUAL, textA.substring(suffixStartA), suffixStartA, suffixStartB));
    }

    return edits;
  }

  /**
   * Compute the diff between two strings using the Myers algorithm.
   *
   * @param textA The original text (middle part without common prefix/suffix).
   * @param textB The modified text (middle part without common prefix/suffix).
   * @param offset The offset to add to positions (for common prefix).
   * @return A list of {@link DiffEdit}s representing the differences.
   */
  private List<DiffEdit> computeMyersDiff(
      final String textA, final String textB, final int offset) {
    final List<DiffEdit> edits = new ArrayList<>();

    // Handle simple cases.
    if (textA.isEmpty() && !textB.isEmpty()) {
      final DiffEdit edit = new DiffEdit(DiffEdit.Type.INSERT, textB, offset, offset);
      edits.add(edit);
      return edits;
    }

    if (!textA.isEmpty() && textB.isEmpty()) {
      final DiffEdit edit = new DiffEdit(DiffEdit.Type.DELETE, textA, offset, offset);
      edits.add(edit);
      return edits;
    }

    if (textA.isEmpty() && textB.isEmpty()) {
      return edits;
    }

    // Use dynamic programming to compute the shortest edit script.
    final int n = textA.length();
    final int m = textB.length();

    // V array for the algorithm - stores the furthest reaching x for each diagonal.
    final int maxD = n + m;
    final int[] v = new int[2 * maxD + 1];

    // Trace to reconstruct the path.
    final List<int[]> trace = new ArrayList<>();

    // Myers algorithm main loop.
    for (int d = 0; d <= maxD; d++) {
      // Save current V for backtracking.
      trace.add(v.clone());

      for (int k = -d; k <= d; k += 2) {
        // Determine if we came from above (insert) or from the left (delete).
        final boolean goDown = (k == -d || (k != d && v[k - 1 + maxD] < v[k + 1 + maxD]));

        final int xStart = goDown ? v[k + 1 + maxD] : v[k - 1 + maxD];

        int x = goDown ? xStart : xStart + 1;
        int y = x - k;

        // Follow diagonal (matching characters).
        while (x < n && y < m && textA.charAt(x) == textB.charAt(y)) {
          x++;
          y++;
        }

        v[k + maxD] = x;

        // Check if we've reached the end.
        if (x >= n && y >= m) {
          // Backtrack to reconstruct the edit sequence.
          return this.backtrack(textA, textB, trace, maxD, offset);
        }
      }
    }

    throw new IllegalStateException("Myers algorithm failed to find a path");
  }

  /**
   * Backtrack through the trace to reconstruct the edit sequence.
   *
   * @param textA The original text.
   * @param textB The modified text.
   * @param trace The trace of V arrays from the forward pass.
   * @param maxD The maximum diagonal offset.
   * @param offset The offset to add to positions.
   * @return A list of {@link DiffEdit}s representing the differences.
   */
  private List<DiffEdit> backtrack(
      final String textA,
      final String textB,
      final List<int[]> trace,
      final int maxD,
      final int offset) {
    int x = textA.length();
    int y = textB.length();

    // Build the path in reverse
    final List<DiffEdit> edits = new ArrayList<>();

    for (int d = trace.size() - 1; d >= 0; d--) {
      final int[] v = trace.get(d);
      final int k = x - y;

      final boolean goDown = (k == -d || (k != d && v[k - 1 + maxD] < v[k + 1 + maxD]));

      final int xStart = goDown ? v[k + 1 + maxD] : v[k - 1 + maxD];
      final int yStart = xStart - (goDown ? k + 1 : k - 1);

      final int xMid = goDown ? xStart : xStart + 1;
      final int yMid = xMid - k;

      // Add diagonal (equal) segment if any.
      if (x > xMid && y > yMid) {
        final String equalText = textA.substring(xMid, x);
        edits.add(0, new DiffEdit(DiffEdit.Type.EQUAL, equalText, offset + xMid, offset + yMid));
      }

      // Add the edit operation.
      if (d > 0) {
        if (goDown) {
          // Vertical move = insert from B.
          if (yStart < yMid) {
            final String insertText = textB.substring(yStart, yMid);
            final DiffEdit edit =
                new DiffEdit(DiffEdit.Type.INSERT, insertText, offset + xStart, offset + yStart);
            edits.add(0, edit);
          }
        } else {
          // Horizontal move = delete from A.
          if (xStart < xMid) {
            final String deleteText = textA.substring(xStart, xMid);
            final DiffEdit edit =
                new DiffEdit(DiffEdit.Type.DELETE, deleteText, offset + xStart, offset + yStart);
            edits.add(0, edit);
          }
        }
      }

      x = xStart;
      y = yStart;
    }

    return edits;
  }
}
