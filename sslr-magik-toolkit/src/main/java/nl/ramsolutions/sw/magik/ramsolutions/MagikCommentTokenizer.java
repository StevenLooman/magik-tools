package nl.ramsolutions.sw.magik.ramsolutions;

import org.sonar.colorizer.InlineDocTokenizer;

/** Magik comment tokenizer. */
class MagikCommentTokenizer extends InlineDocTokenizer {

  /**
   * Constructor.
   *
   * @param tagBefore Tag before.
   * @param tagAfter Tag after.
   */
  MagikCommentTokenizer(String tagBefore, String tagAfter) {
    super("#", tagBefore, tagAfter);
  }
}
