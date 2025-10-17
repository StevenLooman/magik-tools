package nl.ramsolutions.sw.checks.productdef;

import com.sonar.sslr.api.AstNode;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a product.def file has a description. */
@Rule(key = ProductDefMissingDescriptionCheck.CHECK_KEY)
public class ProductDefMissingDescriptionCheck extends ProductDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ProductDefMissingDescription";

  private static final String MESSAGE = "Product description is missing, or is empty.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getProductDefFile().getTopNode();
    final AstNode descriptionNode =
        topNode.getFirstDescendant(ProductDefinitionGrammar.DESCRIPTION);
    if (descriptionNode == null) {
      this.addFileIssue(MESSAGE);
      return;
    }

    final AstNode descriptionNodeLines =
        descriptionNode.getFirstChild(ProductDefinitionGrammar.FREE_LINES);
    final String description =
        descriptionNodeLines.getChildren(ProductDefinitionGrammar.FREE_LINE).stream()
            .map(AstNode::getTokenValue)
            .map(String::trim)
            .collect(Collectors.joining());
    if (description.isEmpty()) {
      this.addIssue(descriptionNode, MESSAGE);
    }
  }
}
