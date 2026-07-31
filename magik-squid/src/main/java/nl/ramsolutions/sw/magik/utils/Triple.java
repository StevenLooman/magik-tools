package nl.ramsolutions.sw.magik.utils;

/** A triple of objects. */
public class Triple<L, M, R> {

  private final L left;
  private final M middle;
  private final R right;

  /**
   * Constructor.
   *
   * @param left Left element.
   * @param middle Middle element.
   * @param right Right element.
   */
  public Triple(final L left, final M middle, final R right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  /**
   * Get the left element.
   *
   * @return The left element.
   */
  public L getLeft() {
    return this.left;
  }

  /**
   * Get the middle element.
   *
   * @return The middle element.
   */
  public M getMiddle() {
    return this.middle;
  }

  /**
   * Get the right element.
   *
   * @return The right element.
   */
  public R getRight() {
    return this.right;
  }
}
