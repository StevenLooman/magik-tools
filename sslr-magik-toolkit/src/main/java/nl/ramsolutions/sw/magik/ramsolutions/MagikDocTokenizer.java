package nl.ramsolutions.sw.magik.ramsolutions;

import org.sonar.colorizer.InlineDocTokenizer;

/** Magik doc tokenizer. */
class MagikDocTokenizer extends InlineDocTokenizer {

  /**
   * Constructor.
   *
   * @param tagBefore Tag before.
   * @param tagAfter Tag after.
   */
  MagikDocTokenizer(String tagBefore, String tagAfter) {
    super("##", tagBefore, tagAfter);
  }
}
