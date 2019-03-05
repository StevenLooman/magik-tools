package org.stevenlooman.sw.magik.toolkit;

import org.sonar.colorizer.InlineDocTokenizer;

class MagikCommentTokenizer extends InlineDocTokenizer {
  public MagikCommentTokenizer(String tagBefore, String tagAfter) {
    super("#", tagBefore, tagAfter);
  }
}

