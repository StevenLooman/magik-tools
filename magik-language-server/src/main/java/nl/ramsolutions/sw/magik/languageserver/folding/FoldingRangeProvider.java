package nl.ramsolutions.sw.magik.languageserver.folding;

import com.sonar.sslr.api.AstNode;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.ServerCapabilities;

/** Folding range provider. */
public class FoldingRangeProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setFoldingRangeProvider(true);
  }

  /**
   * Provide folding ranges.
   *
   * @param magikFile Magik file.
   * @return {@link FoldingRange}s.
   */
  public List<FoldingRange> provideFoldingRanges(final MagikTypedFile magikFile) {
    // Extract all BODY nodes and use these as folding ranges.
    final AstNode topNode = magikFile.getTopNode();
    return topNode.getDescendants(MagikGrammar.BODY).stream()
        .map(
            node -> {
              final AstNode parentNode = node.getParent();
              final AstNode folderNode = parentNode != null ? parentNode : node;
              final int startLine = folderNode.getTokenLine() - 1;
              final int endLine = folderNode.getLastToken().getLine() - 1;
              return new FoldingRange(startLine, endLine);
            })
        .toList();
  }

  /**
   * Provide folding ranges.
   *
   * @param moduleDefFile Module.def file.
   * @return {@link FoldingRange}s.
   */
  public List<FoldingRange> provideFoldingRanges(final ModuleDefFile moduleDefFile) {
    final AstNode topNode = moduleDefFile.getTopNode();
    return topNode.getChildren().stream()
        .filter(
            node ->
                node.getLastChild() != null
                    && node.getLastChild().getTokenValue() != null
                    && node.getLastChild().getTokenValue().equalsIgnoreCase("end"))
        .map(
            node -> {
              final int startLine = node.getTokenLine() - 1;
              final int endLine = node.getLastToken().getLine() - 1;
              return new FoldingRange(startLine, endLine);
            })
        .toList();
  }

  /**
   * Provide folding ranges.
   *
   * @param productDefFile Product.def file.
   * @return {@link FoldingRange}s.
   */
  public List<FoldingRange> provideFoldingRanges(final ProductDefFile productDefFile) {
    final AstNode topNode = productDefFile.getTopNode();
    return topNode.getChildren().stream()
        .filter(
            node ->
                node.getLastChild() != null
                    && node.getLastChild().getTokenValue() != null
                    && node.getLastChild().getTokenValue().equalsIgnoreCase("end"))
        .map(
            node -> {
              final int startLine = node.getTokenLine() - 1;
              final int endLine = node.getLastToken().getLine() - 1;
              return new FoldingRange(startLine, endLine);
            })
        .toList();
  }
}
