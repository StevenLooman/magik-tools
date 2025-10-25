package nl.ramsolutions.sw.sslr.magik;

import org.sonar.colorizer.InlineDocTokenizer;

/** Magik doc tokenizer. */
class MagikDocTokenizer extends InlineDocTokenizer {

  /**
   * Constructor.
   *
   * @param tagBefore Tag before.
   * @param tagAfter Tag after.
   */
  MagikDocTokenizer(final String tagBefore, final String tagAfter) {
    super("##", tagBefore, tagAfter);
  }
}
