package nl.ramsolutions.sw.magik.sessionwrapper;

import com.sonar.sslr.api.AstNode;
import java.util.regex.Pattern;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;

class MagikJlineHighlighter implements Highlighter {

  private final MagikParser parser = new MagikParser();
  private Pattern errorPattern;
  private int errorIndex;

  @Override
  public AttributedString highlight(final LineReader reader, final String buffer) {
    final AstNode astNode = this.parser.parseSafe(buffer);
    final MagikJlineHighlighterWalker walker = new MagikJlineHighlighterWalker();
    walker.walkAst(astNode);
    return walker.getAttributedString();
  }

  @Override
  public void setErrorPattern(final Pattern errorPattern) {
    this.errorPattern = errorPattern;
  }

  @Override
  public void setErrorIndex(final int errorIndex) {
    this.errorIndex = errorIndex;
  }
}
