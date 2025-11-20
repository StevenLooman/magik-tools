package nl.ramsolutions.sw.productdef.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.impl.Parser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/** Smallworld product.def parser. */
public class ProductDefParser {

  private final Parser<LexerlessGrammar> parser;

  private static final Map<ProductDefinitionGrammar, ProductDefinitionGrammar> RULE_MAPPING =
      new EnumMap<>(ProductDefinitionGrammar.class);

  static {
    RULE_MAPPING.put(
        ProductDefinitionGrammar.SYNTAX_ERROR_SECTION, ProductDefinitionGrammar.SYNTAX_ERROR);
    RULE_MAPPING.put(
        ProductDefinitionGrammar.SYNTAX_ERROR_LINE, ProductDefinitionGrammar.SYNTAX_ERROR);
    RULE_MAPPING.put(
        ProductDefinitionGrammar.SYNTAX_ERROR_END, ProductDefinitionGrammar.SYNTAX_ERROR);
  }

  /** Constructor with default charset. */
  public ProductDefParser() {
    final LexerlessGrammar productDefGrammar = ProductDefinitionGrammar.create();
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

    AstNodeHelper.setUri(node, uri);

    this.applyRuleMapping(node);

    return node;
  }

  @SuppressWarnings("checkstyle:NestedIfDepth")
  private void applyRuleMapping(final AstNode node) {
    final AstNodeType type = node.getType();
    if (ProductDefParser.RULE_MAPPING.containsKey(type)) {
      final AstNodeType newType = ProductDefParser.RULE_MAPPING.get(type);
      final String newName = newType.toString();

      // Convert node to new type.
      AstNodeHelper.setType(node, newType);
      AstNodeHelper.setName(node, newName);
    }

    node.getChildren().forEach(this::applyRuleMapping);
  }
}
