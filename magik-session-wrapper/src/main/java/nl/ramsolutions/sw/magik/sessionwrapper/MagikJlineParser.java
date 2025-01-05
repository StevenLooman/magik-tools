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
    if (line.endsWith("\n$")) {
      // If ends with single $ on line, then force sending it to our session.
      return new MagikJlineParsedLine(line, cursor, topNode);
    }

    final AstNode syntaxErrorNode = topNode.getFirstDescendant(MagikGrammar.SYNTAX_ERROR);
    if (syntaxErrorNode != null) {
      final int tokenLine = syntaxErrorNode.getToken().getLine();
      final int tokenColumn = syntaxErrorNode.getToken().getColumn();
      throw new EOFError(tokenLine, tokenColumn, "Syntax error");
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
