package nl.ramsolutions.sw.checks.productdef;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;

/** Check that a product.def file has a title. */
public class ProductDefMissingTitleCheck extends ProductDefCheck {

  private static final String MESSAGE = "Product title is missing, or is empty.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getProductDefFile().getTopNode();
    final AstNode titleNode = topNode.getFirstDescendant(ProductDefinitionGrammar.TITLE);
    if (titleNode == null) {
      this.addFileIssue(MESSAGE);
      return;
    }

    final AstNode titleNodeLines = titleNode.getFirstChild(ProductDefinitionGrammar.FREE_LINES);
    final String title =
        titleNodeLines.getChildren(ProductDefinitionGrammar.FREE_LINE).stream()
            .map(AstNode::getTokenValue)
            .map(String::trim)
            .collect(Collectors.joining());
    if (title.isEmpty()) {
      this.addIssue(titleNode, MESSAGE);
    }
  }
}
