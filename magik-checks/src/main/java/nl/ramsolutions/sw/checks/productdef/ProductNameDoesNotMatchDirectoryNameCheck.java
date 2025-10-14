package nl.ramsolutions.sw.checks.productdef;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.sonar.check.Rule;

/** Check that a product definition matches the directory name. */
@Rule(key = ProductNameDoesNotMatchDirectoryNameCheck.CHECK_KEY)
public class ProductNameDoesNotMatchDirectoryNameCheck extends ProductDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ProductNameDoesNotMatchDirectoryName";

  private static final String MESSAGE = "Product name does not match directory name.";

  @Override
  protected void scanFile() {
    final ProductDefFile productDefFile = this.getProductDefFile();
    final AstNode topNode = productDefFile.getTopNode();
    final AstNode productNameNode =
        topNode.getFirstDescendant(ProductDefinitionGrammar.PRODUCT_NAME);
    if (productNameNode == null) {
      return;
    }

    final String productName = productNameNode.getTokenValue();
    final URI uri = productDefFile.getUri();
    final Path path = Path.of(uri);
    final Path parentPath = path.getParent();
    if (parentPath == null) {
      return;
    }

    final String directoryName = parentPath.getFileName().toString();
    if (!productName.equalsIgnoreCase(directoryName)) {
      this.addIssue(productNameNode, MESSAGE);
    }
  }
}
