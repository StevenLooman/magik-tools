package nl.ramsolutions.sw.checks.productdef;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a product.def file has no syntax errors. */
@Rule(key = ProductDefSyntaxErrorCheck.CHECK_KEY)
public class ProductDefSyntaxErrorCheck extends ProductDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ProductDefSyntaxError";

  private static final String MESSAGE = "Invalid syntax.";

  @Override
  protected void scanFile() {
    final AstNode topNode = this.getProductDefFile().getTopNode();
    topNode
        .getDescendants(ProductDefinitionGrammar.SYNTAX_ERROR)
        .forEach(node -> this.addIssue(node, MESSAGE));
  }
}
