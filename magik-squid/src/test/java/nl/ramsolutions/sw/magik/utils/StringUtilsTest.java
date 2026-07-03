package nl.ramsolutions.sw.magik.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Test {@link StringUtils}. */
class StringUtilsTest {

  @Test
  void testEqualStringsHaveZeroDistance() {
    assertThat(StringUtils.levenshteinDistance("visual", "visual")).isZero();
  }

  @Test
  void testEmptyStringDistanceIsOtherLength() {
    assertThat(StringUtils.levenshteinDistance("", "visual")).isEqualTo(6);
    assertThat(StringUtils.levenshteinDistance("block", "")).isEqualTo(5);
  }

  @Test
  void testSingleEditDistances() {
    assertThat(StringUtils.levenshteinDistance("visua", "visual")).isEqualTo(1); // insertion
    assertThat(StringUtils.levenshteinDistance("visualz", "visual")).isEqualTo(1); // deletion
    assertThat(StringUtils.levenshteinDistance("visurl", "visual")).isEqualTo(1); // substitution
  }

  @Test
  void testDistanceIsSymmetric() {
    assertThat(StringUtils.levenshteinDistance("kitten", "sitting"))
        .isEqualTo(StringUtils.levenshteinDistance("sitting", "kitten"))
        .isEqualTo(3);
  }
}
