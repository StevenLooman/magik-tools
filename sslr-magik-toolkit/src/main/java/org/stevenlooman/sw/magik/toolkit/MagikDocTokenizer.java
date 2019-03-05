package org.stevenlooman.sw.magik.toolkit;

import org.sonar.colorizer.InlineDocTokenizer;

class MagikDocTokenizer extends InlineDocTokenizer {
  public MagikDocTokenizer(String tagBefore, String tagAfter) {
    super("##", tagBefore, tagAfter);
  }
}

