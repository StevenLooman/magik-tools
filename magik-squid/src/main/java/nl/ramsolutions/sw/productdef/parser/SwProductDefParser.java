package nl.ramsolutions.sw.productdef.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.productdef.api.SwProductDefinitionGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/** Smallworld product.def parser. */
public class SwProductDefParser {

  private final Parser<LexerlessGrammar> parser;

  /** Constructor with default charset. */
  public SwProductDefParser() {
    final LexerlessGrammar productDefGrammar = SwProductDefinitionGrammar.create();
    this.parser = new ParserAdapter<>(StandardCharsets.ISO_8859_1, productDefGrammar);
  }

  /**
   * Parse a file and return the AstNode.
   *
   * @param path Path to file
   * @return Tree
   * @throws IOException -
   */
  public AstNode parse(final Path path) throws IOException {
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String source = Files.readString(path, charset);
    final URI uri = path.toUri();
    return this.parse(source, uri);
  }

  /**
   * Parse safe and set {@link URI}.
   *
   * @param source Source to parse.
   * @param uri URI to set.
   * @return Parsed source.
   */
  public AstNode parse(final String source, final URI uri) {
    final AstNode node = this.parser.parse(source);
    AstNodeHelper.updateUri(node, uri);
    return node;
  }
}
