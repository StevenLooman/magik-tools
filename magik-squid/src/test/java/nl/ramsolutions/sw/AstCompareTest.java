package nl.ramsolutions.sw;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.junit.jupiter.api.Test;

/** Test {@link AstCompare}. */
class AstCompareTest {

  private AstNode parseMagik(final String code) {
    final MagikParser parser = new MagikParser();
    return parser.parseSafe(code);
  }

  @Test
  void testCompareEqualsRecursive() {
    final String code1 = "_true";
    final String code2 = "_true";
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    assertThat(equals).isTrue();
  }

  @Test
  void testCompareEqualsRecursivenIgnoringTrivia() {
    final String code1 =
        """
        _block
        _endblock
        """;
    final String code2 =
        """
        _block
          # comment
        _endblock
        """;
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    assertThat(equals).isTrue();
  }

  @Test
  void testCompareEqualsRecursivenComparingLocation() {
    final String code1 =
        """
        _block
        _endblock
        """;
    final String code2 =
        """
        _block

        _endblock
        """;
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals =
        AstCompare.astNodeEqualsRecursive(left, right, AstCompare.Flags.COMPARE_LOCATION);
    assertThat(equals).isFalse();
  }

  @Test
  void testCompareNotEqualsRecursiveTrueFalse() {
    final String code1 = "_true";
    final String code2 = "_false";
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    assertThat(equals).isFalse();
  }

  @Test
  void testCompareNotEqualsWithParenthesisRecursive() {
    final String code1 = "(_true)";
    final String code2 = "_true";
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals = AstCompare.astNodeEqualsRecursive(left, right);
    assertThat(equals).isFalse();
  }

  @Test
  void testCompareEqualsRecursiveTrivia() {
    final String code1 =
        """
        _block
          # comment 1
          # comment 2
        _endblock
        """;
    final String code2 =
        """
        _block
          # comment 1
          # comment 2
        _endblock
        """;
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals =
        AstCompare.astNodeEqualsRecursive(
            left, right, AstCompare.Flags.COMPARE_TRIVIA, AstCompare.Flags.COMPARE_LOCATION);
    assertThat(equals).isTrue();
  }

  @Test
  void testCompareEqualsRecursiveTrivia2() {
    final String code1 =
        """
        _block
          # comment 1
          # comment 2
        _endblock
        """;
    final String code2 =
        """
        _block

          # comment 1
          # comment 2
        _endblock
        """;
    final AstNode left = this.parseMagik(code1);
    final AstNode right = this.parseMagik(code2);

    final boolean equals =
        AstCompare.astNodeEqualsRecursive(left, right, AstCompare.Flags.COMPARE_TRIVIA);
    assertThat(equals).isFalse();
  }

  @Test
  void testCompareEqualsRecursiveTriviaCloned() {
    final String code =
        """
        _block
          # comment 1
          # comment 2
        _endblock
        """;
    final AstNode left = this.parseMagik(code);
    final AstNode right = AstNodeHelper.clone(left);

    final boolean equals =
        AstCompare.astNodeEqualsRecursive(
            left, right, AstCompare.Flags.COMPARE_TRIVIA, AstCompare.Flags.COMPARE_LOCATION);
    assertThat(equals).isTrue();
  }
}
