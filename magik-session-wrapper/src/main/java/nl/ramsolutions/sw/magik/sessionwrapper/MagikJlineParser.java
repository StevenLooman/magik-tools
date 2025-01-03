package nl.ramsolutions.sw.magik.sessionwrapper;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.SyntaxError;

/** JLine parser for Magik. */
class MagikJlineParser implements Parser {

  final MagikParser magikParser = new MagikParser();

  @Override
  public ParsedLine parse(final String line, final int cursor, final ParseContext context)
      throws SyntaxError {
    final AstNode topNode = magikParser.parse(line);

    switch (context) {
      case UNSPECIFIED:
        return this.parseUnspecified(line, cursor, topNode);

      case ACCEPT_LINE:
        return this.parseAcceptLine(line, cursor, topNode);

      case SPLIT_LINE:
        return this.parseSplitLine(line, cursor, topNode);

      case COMPLETE:
        return this.parseComplete(line, cursor, topNode);

      case SECONDARY_PROMPT:
        return this.parseSecondaryPrompt(line, cursor, topNode);

      default:
        throw new IllegalArgumentException("Unknown context: " + context);
    }
  }

  private ParsedLine parseUnspecified(final String line, final int cursor, final AstNode topNode) {
    return new MagikJlineParsedLine(line, cursor, topNode);
  }

  private ParsedLine parseAcceptLine(final String line, final int cursor, final AstNode topNode) {
    if (topNode.hasDescendant(MagikGrammar.SYNTAX_ERROR)) {
      throw new EOFError(-1, -1, "Syntax error", "xxx");
    }

    return new MagikJlineParsedLine(line, cursor, topNode);
  }

  private ParsedLine parseSplitLine(final String line, final int cursor, final AstNode topNode) {
    return new MagikJlineParsedLine(line, cursor, topNode);
  }

  private ParsedLine parseComplete(final String line, final int cursor, final AstNode topNode) {
    return new MagikJlineParsedLine(line, cursor, topNode);
  }

  private ParsedLine parseSecondaryPrompt(
      final String line, final int cursor, final AstNode topNode) {
    return new MagikJlineParsedLine(line, cursor, topNode);
  }
}
