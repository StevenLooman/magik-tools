package nl.ramsolutions.sw.magik.sessionwrapper;

import java.util.regex.Pattern;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

class MagikJlineHighlighter implements Highlighter {

  @Override
  public AttributedString highlight(final LineReader reader, final String buffer) {
    if (buffer.startsWith("_")) {
      return new AttributedString(buffer);
    }

    return new AttributedStringBuilder()
        .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
        .append(buffer)
        .toAttributedString();
  }

  @Override
  public void setErrorPattern(final Pattern errorPattern) {}

  @Override
  public void setErrorIndex(final int errorIndex) {}
}
