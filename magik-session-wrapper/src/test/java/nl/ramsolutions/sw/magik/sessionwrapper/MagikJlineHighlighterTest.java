package nl.ramsolutions.sw.magik.sessionwrapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;

/** Tests for {@link MagikJlineHighlighter}. */
class MagikJlineHighlighterTest {

  AttributedString highlight(final String code) {
    final MagikJlineHighlighter highlighter = new MagikJlineHighlighter();
    return highlighter.highlight(null, code);
  }

  @Test
  void testHighlightPrompt1() {
    final String code =
        """
        write(10)
        """;
    final AttributedString attributedString = this.highlight(code);
    final AttributedString expected =
        new AttributedStringBuilder()
            .append("write")
            .append("(", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("10", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(")", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("\n")
            .toAttributedString();
    assertThat(attributedString).isEqualTo(expected);
  }

  @Test
  void testHighlightPrompt2() {
    final String code =
        """
        _for y _over 1.upto(10) # test
        _loop@abc
            show(y, (y), y + y, |y|, "abc", 'abc', 16r10, 10, @user:object, {10}, _true)
        _endloop@abc
        """;
    final AttributedString attributedString = this.highlight(code);
    final AttributedString expected =
        new AttributedStringBuilder()
            .append("_for", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .append(" y ")
            .append("_over", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .append(" ")
            .append("1", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(".", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("upto")
            .append("(", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("10", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(")", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("# test", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
            .append("\n")
            .append("_loop", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .append("@", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("abc")
            .append("\n")
            .append("    show")
            .append("(", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("y")
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("(", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("y")
            .append(")", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("y")
            .append(" ")
            .append("+", AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
            .append(" ")
            .append("y")
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("|y|")
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("\"abc\"", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("'abc'", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("16r10", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("10", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("@", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("user:object")
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("{", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("10", AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
            .append("}", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(",", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append(" ")
            .append("_true", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .append(")", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("\n")
            .append("_endloop", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
            .append("@", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .append("abc")
            .append("\n")
            .toAttributedString();
    assertThat(attributedString.toAnsi()).hasToString(expected.toAnsi());
    assertThat(attributedString).isEqualTo(expected);
  }
}
