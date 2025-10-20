package nl.ramsolutions.sw.sslr;

import org.sonar.colorizer.InlineDocTokenizer;

/** Comment tokenizer. */
public class CommentTokenizer extends InlineDocTokenizer {

  /**
   * Constructor.
   *
   * @param tagBefore Tag before.
   * @param tagAfter Tag after.
   */
  public CommentTokenizer(final String tagBefore, final String tagAfter) {
    super("#", tagBefore, tagAfter);
  }
}
