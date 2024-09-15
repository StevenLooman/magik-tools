package nl.ramsolutions.sw.magik.languageserver.semantictokens;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Token;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.DocumentFilter;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Semantic token provider. */
public class SemanticTokenProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SemanticTokenProvider.class);
  private static final int SIZE_PER_TOKEN = 5;

  private static final SemanticTokensLegend LEGEND =
      new SemanticTokensLegend(
          Arrays.stream(SemanticToken.Type.values())
              .map(SemanticToken.Type::getSemanticTokenName)
              .toList(),
          Arrays.stream(SemanticToken.Modifier.values())
              .map(SemanticToken.Modifier::getSemanticModifierName)
              .toList());

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    final SemanticTokensWithRegistrationOptions semanticTokensProvider =
        new SemanticTokensWithRegistrationOptions(SemanticTokenProvider.LEGEND);
    semanticTokensProvider.setFull(true);
    semanticTokensProvider.setRange(false);
    semanticTokensProvider.setDocumentSelector(
        List.of(
            new DocumentFilter("product.def", "file", null),
            new DocumentFilter("module.def", "file", null),
            new DocumentFilter("magik", "file", null)));
    capabilities.setSemanticTokensProvider(semanticTokensProvider);
  }

  /**
   * Build SemanticTokens.
   *
   * @param magikFile Magik file.
   * @return SemanticTokens.
   * @throws IOException -
   */
  public SemanticTokens provideSemanticTokensFull(final MagikTypedFile magikFile) {
    LOGGER.debug("Providing semantic tokens full, file: {}", magikFile);

    final MagikSemanticTokenWalker walker = new MagikSemanticTokenWalker(magikFile);
    final AstNode topNode = magikFile.getTopNode();
    walker.walkAst(topNode);

    final List<SemanticToken> walkedSemanticTokens = walker.getSemanticTokens();
    return this.buildSemanticTokens(walkedSemanticTokens);
  }

  public SemanticTokens provideSemanticTokensFull(final ModuleDefFile moduleDefFile) {
    LOGGER.debug("Providing semantic tokens full, file: {}", moduleDefFile);

    final ModuleDefSemanticTokenWalker walker = new ModuleDefSemanticTokenWalker();
    final AstNode topNode = moduleDefFile.getTopNode();
    walker.walkAst(topNode);

    final List<SemanticToken> walkedSemanticTokens = walker.getSemanticTokens();
    return this.buildSemanticTokens(walkedSemanticTokens);
  }

  public SemanticTokens provideSemanticTokensFull(final ProductDefFile productDefFile) {
    LOGGER.debug("Providing semantic tokens full, file: {}", productDefFile);

    final ProductDefSemanticTokenWalker walker = new ProductDefSemanticTokenWalker();
    final AstNode topNode = productDefFile.getTopNode();
    walker.walkAst(topNode);

    final List<SemanticToken> walkedSemanticTokens = walker.getSemanticTokens();
    return this.buildSemanticTokens(walkedSemanticTokens);
  }

  private SemanticTokens buildSemanticTokens(final List<SemanticToken> walkedSemanticTokens) {
    if (walkedSemanticTokens.isEmpty()) {
      return new SemanticTokens(Collections.emptyList());
    }

    final ArrayList<Integer> data =
        new ArrayList<>((walkedSemanticTokens.size() - 1) * SIZE_PER_TOKEN);
    final SemanticToken startSemanticToken = this.createStartSemanticToken();
    Stream.concat(Stream.of(startSemanticToken), walkedSemanticTokens.stream())
        .reduce( // NOSONAR: Data is stored in `data` list.
            (s1, s2) -> {
              s2.dataToPrevious(s1).collect(Collectors.toCollection(() -> data));
              return s2;
            });
    return new SemanticTokens(data);
  }

  private SemanticToken createStartSemanticToken() {
    final Token startToken =
        Token.builder()
            .setLine(1)
            .setColumn(0)
            .setValueAndOriginalValue("")
            .setType(GenericTokenType.UNKNOWN_CHAR)
            .setURI(URI.create("magik://dummy"))
            .build();
    return new SemanticToken(startToken, SemanticToken.Type.CLASS, Collections.emptySet());
  }
}
