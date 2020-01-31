package org.stevenlooman.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.TokenType;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MessagePatchGrammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MagikParser {

  public enum UtilityTokenType implements TokenType {
    DUMMY,
    SYNTAX_ERROR;

    @Override
    public String getName() {
      return name();
    }

    @Override
    public String getValue() {
      return name();
    }

    @Override
    public boolean hasToBeSkippedFromAst(AstNode node) {
      return false;
    }
  }

  private static final String TRANSMIT = "$";

  public Parser<LexerlessGrammar> magikParser;
  public Parser<LexerlessGrammar> messagePatchParser;

  /**
   * Constructor.
   * @param charset Encoding of file to parse
   */
  public MagikParser(Charset charset) {
    magikParser = new ParserAdapter<>(charset, MagikGrammar.create());
    messagePatchParser = new ParserAdapter<>(
        charset,
        MessagePatchGrammar.create(TRANSMIT));
  }

  /**
   * Parse a string and return the AstNode.
   * IOExceptions are caught, not handled.
   *
   * @param source Source to parse
   * @return Tree
   */
  public AstNode parseSafe(String source) {
    try {
      return parse(source);
    } catch (IOException ex) {
      System.out.println("Caught exception: " + ex);
    }
    return null;
  }

  /**
   * Parse a file and return the AstNode.
   * IOExceptions are caught, not handled.
   *
   * @param path Path to file
   * @return Tree
   * @throws IOException -
   */
  public AstNode parseSafe(Path path) {
    try {
      return parse(path);
    } catch (IOException ex) {
      System.out.println("Caught exception: " + ex);
    }
    return null;
  }

  /**
   * Parse a string and return the AstNode.
   *
   * @param source Source to parse
   * @return Tree
   * @throws IOException -
   */
  public AstNode parse(String source) throws IOException {
    StringReader sr = new StringReader(source);
    return parse(sr);
  }

  /**
   * Parse a file and return the AstNode.
   *
   * @param path Path to file
   * @return Tree
   * @throws IOException -
   */
  public AstNode parse(Path path) throws IOException {
    FileReader sr = new FileReader(path.toFile());
    return parse(sr);
  }

  /**
   * Parse sources from reader.
   *
   * @param reader Reader to read from.
   * @return Parsed node.
   * @throws IOException Exception raised.
   */
  private AstNode parse(Reader reader) throws IOException {
    AstNode resultNode = new AstNode(MagikGrammar.MAGIK, "MAGIK", null);

    int lineNumber = 0;
    int startLineOffset = 0;
    StringBuilder sb = new StringBuilder();
    boolean readMessagePatch = false;

    BufferedReader br = new BufferedReader(reader);
    String line;
    do {
      line = br.readLine();
      lineNumber++;

      if (line != null) {
        sb.append(line);
        sb.append('\n');
      }

      if (TRANSMIT.equals(line) || line == null) {
        // transmit to parser/compiler and reset buffer
        String part = sb.toString();

        // parse part
        AstNode node = null;
        if (readMessagePatch) {
          part += "\n";  // Add newline at the end of patch, to help MessagePatchGrammar.
          node = parsePartMessagePatch(part);
        } else {
          if (!isEmpty(part)) {
            try {
              node = parsePartMagik(part);
            } catch (RecognitionException exception) {
              // Don't throw, magik parser also can continue if it encounters invalid code.
              // Instead, build a special node so this is recognizable.
              node = buildSyntaxErrorNode(part, exception);
            }
          }
        }

        if (node != null) {
          // fix tokens/lines
          updateTokenLines(node.getTokens(), startLineOffset);

          // fix identifier casing
          updateIdentifiersSymbolsCasing(node);

          // rebuild AST as if a single file/parsePartMagik
          addChildNodesToParent(resultNode, node.getChildren());

          // read message patch next up?
          readMessagePatch = hasReadMessagePatch(node);
        }

        // reset buffer
        sb.setLength(0);
        startLineOffset = lineNumber;
      }
    } while (line != null);

    return resultNode;
  }

  private AstNode buildSyntaxErrorNode(String part, RecognitionException recognitionException) {
    int line = recognitionException.getLine();
    String[] lines = part.split("\n");
    if (lines.length <= line) {
      line = lines.length;
    }

    // parse message, as the exception doesn't provide the raw value
    String message = recognitionException.getMessage();
    Pattern pattern = Pattern.compile("Parse error at line (\\d+) column (\\d+):.*");
    Matcher matcher = pattern.matcher(message);
    if (!matcher.find()) {
      throw new IllegalStateException("Unrecognized RecognitionException message");
    }
    String columnStr = matcher.group(2);
    int column = Integer.parseInt(columnStr);

    URI uri = buildUri();
    Token.Builder builder = Token.builder();
    Token dummyToken = builder.setValueAndOriginalValue("dummy")
        .setURI(uri)
        .setLine(line)
        .setColumn(column)
        .setType(UtilityTokenType.DUMMY)
        .build();
    Token syntaxErrorToken = builder.setValueAndOriginalValue("X")
        .setURI(uri)
        .setLine(line)
        .setColumn(column)
        .setType(UtilityTokenType.SYNTAX_ERROR)
        .build();

    AstNode dummyNode =
        new AstNode(MagikGrammar.SYNTAX_ERROR, "SYNTAX_ERROR", dummyToken);
    AstNode errorNode =
        new AstNode(MagikGrammar.SYNTAX_ERROR, "SYNTAX_ERROR", syntaxErrorToken);
    dummyNode.addChild(errorNode);
    return dummyNode;
  }

  /**
   * Update token value.
   *
   * @param node Node to update.
   */
  private void updateIdentifiersSymbolsCasing(AstNode node) {
    Field field = null;
    try {
      field = Token.class.getDeclaredField("value");
      field.setAccessible(true);
    } catch (NoSuchFieldException ex) {
      System.out.println("Caught exception during parsing: " + ex);
      ex.printStackTrace();
    }

    AstNodeType nodeType = node.getType();
    if (nodeType == MagikGrammar.IDENTIFIER || nodeType == MagikGrammar.SYMBOL) {
      Token token = node.getToken();
      String value = parseIdentifier(token.getValue());
      try {
        field.set(token, value);
      } catch (IllegalAccessException ex) {
        System.out.println("Caught exception during parsing: " + ex);
        ex.printStackTrace();
      }
    }

    for (AstNode childNode: node.getChildren()) {
      updateIdentifiersSymbolsCasing(childNode);
    }
  }

  /**
   * Test if source contains any magik source.
   *
   * @param source Potential magik code
   * @return True if it contains no magik code, false otherwise.
   */
  private boolean isEmpty(String source) {
    source = source.replaceAll("#.*", ""); // remove any comment lines
    return source.trim().isEmpty();
  }

  /**
   * Parse a part as a message patch.
   *
   * @param source Part to parse.
   * @return Resulting AstNode.
   */
  private AstNode parsePartMessagePatch(String source) {
    source = source.substring(0, source.length() - 1); // remove trailing \n
    return messagePatchParser.parse(source);
  }

  /**
   * Parse a part as Magik code.
   *
   * @param source Part to parse.
   * @return Resulting AstNode.
   */
  public AstNode parsePartMagik(String source) {
    return magikParser.parse(source);
  }

  /**
   * Test if 'read_message_patch' was parsed, indicating the start of a message patch.
   *
   * @param node AstNode to test.
   * @return True if the next part is a message patch, false if not.
   */
  private boolean hasReadMessagePatch(AstNode node) {
    if ("read_message_patch".equalsIgnoreCase(node.getTokenValue())) {
      return true;
    }

    for (AstNode child : node.getChildren()) {
      if (hasReadMessagePatch(child)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Update the token lines to match the lines in the source file, instead of the parsed part.
   *
   * @param tokens Tokens to update.
   * @param lineOffset Offset to add to lines.
   */
  private void updateTokenLines(List<Token> tokens, int lineOffset) {
    Field field = null;
    try {
      field = Token.class.getDeclaredField("line");
      field.setAccessible(true);
    } catch (NoSuchFieldException ex) {
      System.out.println("Caught exception during parsing: " + ex);
      ex.printStackTrace();
    }

    for (Token token : tokens) {
      // update token lines
      int newLine = lineOffset + token.getLine();
      try {
        field.set(token, newLine);
      } catch (IllegalAccessException ex) {
        System.out.println("Caught exception during parsing: " + ex);
        ex.printStackTrace();
      }

      // update trivia-token lines
      for (Trivia trivia : token.getTrivia()) {
        for (Token triviaToken : trivia.getTokens()) {
          newLine = lineOffset + triviaToken.getLine();
          try {
            field.set(triviaToken, newLine);
          } catch (IllegalAccessException ex) {
            System.out.println("Caught exception during parsing: " + ex);
            ex.printStackTrace();
          }
        }
      }
    }
  }

  /**
   * Add child nodes to parent node.
   *
   * @param parentNode Parent node to add to.
   * @param children Child nodes to add.
   */
  private void addChildNodesToParent(AstNode parentNode, List<AstNode> children) {
    for (AstNode child : children) {
      parentNode.addChild(child);
    }
  }

  /**
   * Parse an identifier.
   *
   * @param value Identifier to parse.
   * @return Parsed identifier.
   */
  static String parseIdentifier(String value) {
    // if |, read until next |
    // if \\., read .
    // else read lowercase
    StringBuilder sb = new StringBuilder(value.length());

    for (int i = 0; i < value.length(); ++i) {
      char chr = value.charAt(i);
      if (chr == '|') {
        // piped segment
        ++i; // skip first |
        // read until next |
        for (; i < value.length(); ++i) {
          chr = value.charAt(i);
          if (chr == '|') {
            break;
          }
          sb.append(chr);
        }
      } else if (chr == '\\') {
        // escaped character
        ++i; // skip \
        chr = value.charAt(i);
        sb.append(chr);
      } else {
        // normal character
        chr = Character.toLowerCase(chr);
        sb.append(chr);
      }
    }

    return sb.toString();
  }

  private static URI buildUri() {
    try {
      return new URI("tests://unittest");
    } catch (URISyntaxException exception) {
      // pass
    }
    return null;
  }

}
