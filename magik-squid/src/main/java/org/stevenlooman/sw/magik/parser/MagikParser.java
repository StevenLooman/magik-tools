package org.stevenlooman.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import com.sonar.sslr.impl.Parser;

import org.apache.commons.lang.reflect.FieldUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;
import org.stevenlooman.sw.magik.api.MagikGrammar;
import org.stevenlooman.sw.magik.api.MessagePatchGrammar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

public class MagikParser {

  private static final String TRANSMIT = "$";
  private static final String END_OF_MESSAGE_PATCH = "$";
  private static final Logger LOGGER = Loggers.get(MagikParser.class);

  Parser<LexerlessGrammar> magikParser;
  ParserAdapter<LexerlessGrammar> messagePatchParser;

  /**
   * Constructor.
   * @param charset Encoding of file to parse
   */
  public MagikParser(Charset charset) {
    magikParser = new ParserAdapter<>(charset, MagikGrammar.create());
    messagePatchParser = new ParserAdapter<>(
        charset,
        MessagePatchGrammar.create(END_OF_MESSAGE_PATCH));
  }

  /**
   * Parse a string and return the AstNode.
   * @param source Source to parse
   * @return Tree
   */
  public AstNode parse(String source) {
    AstNode node = null;
    try (StringReader sr = new StringReader(source)) {
      node = parse(sr);
    } catch (IOException ex) {
      LOGGER.error("Caught exception during parsing", ex);
    }
    return node;
  }

  /**
   * Parse a file and return the AstNode.
   * @param path Path to file
   * @return Tree
   */
  public AstNode parse(Path path) {
    AstNode node = null;
    try (FileReader sr = new FileReader(path.toFile())) {
      node = parse(sr);
    } catch (IOException ex) {
      LOGGER.error("Caught exception during parsing", ex);
    }
    return node;
  }

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
        AstNode node;
        if (readMessagePatch) {
          node = parsePartMessagePatch(part);
        } else {
          node = parsePartMagik(part);
        }

        if (isNotParsed(part, node)) {
          LOGGER.error("Could not parse: '" + part + "'");
        }

        // fix tokens/lines
        updateTokenLines(node.getTokens(), startLineOffset);

        // fix identifier casing
        updateIdentifiersSymbolsCasing(node);

        // reset buffer
        sb.setLength(0);
        startLineOffset = lineNumber;

        // rebuild AST as if a single file/parsePartMagik
        addChildNodesToParent(resultNode, node.getChildren());

        // read message patch next up?
        readMessagePatch = hasReadMessagePatch(node);
      }
    } while (line != null);

    return resultNode;
  }

  private void updateIdentifiersSymbolsCasing(AstNode node) {
    Field field = FieldUtils.getField(Token.class, "value", true);

    AstNodeType nodeType = node.getType();
    if (nodeType == MagikGrammar.IDENTIFIER || nodeType == MagikGrammar.SYMBOL) {
      Token token = node.getToken();
      String value = parseIdentifier(token.getValue());
      try {
        field.set(token, value);
      } catch (IllegalAccessException ex) {
        LOGGER.error("Caught exception during update token lines", ex);
      }
    }

    for (AstNode childNode: node.getChildren()) {
      updateIdentifiersSymbolsCasing(childNode);
    }
  }

  private boolean isNotParsed(String source, AstNode node) {
    source = source.replaceAll("#.*", ""); // remove any comment lines
    return node.getChildren().isEmpty() && !source.trim().isEmpty();
  }

  private AstNode parsePartMessagePatch(String source) {
    source = source.substring(0, source.length() - 1); // remove trailing \n
    return messagePatchParser.parse(source);
  }

  private AstNode parsePartMagik(String source) {
    return magikParser.parse(source);
  }

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

  private void updateUri(List<Token> tokens, File file) {
    Field field = FieldUtils.getField(Token.class, "uri", true);
    URI uri = file.toURI();
    for (Token token : tokens) {
      try {
        field.set(token, uri);
      } catch (IllegalAccessException ex) {
        LOGGER.error("Caught exception during update URI", ex);
      }
    }
  }

  private void updateTokenLines(List<Token> tokens, int lineOffset) {
    Field field = FieldUtils.getField(Token.class, "line", true);
    for (Token token : tokens) {
      // update token lines
      int newLine = lineOffset + token.getLine();
      try {
        field.set(token, newLine);
      } catch (IllegalAccessException ex) {
        LOGGER.error("Caught exception during update token lines", ex);
      }

      // update trivia-token lines
      for (Trivia trivia : token.getTrivia()) {
        for (Token triviaToken : trivia.getTokens()) {
          newLine = lineOffset + triviaToken.getLine();
          try {
            field.set(triviaToken, newLine);
          } catch (IllegalAccessException ex) {
            LOGGER.error("Caught exception during update trivia lines", ex);
          }
        }
      }
    }
  }

  private void addChildNodesToParent(AstNode resultNode, List<AstNode> children) {
    for (AstNode child : children) {
      resultNode.addChild(child);
    }
  }

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

}
