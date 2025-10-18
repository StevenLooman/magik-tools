package nl.ramsolutions.sw.checks.productdef;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import nl.ramsolutions.sw.productdef.api.ProductDefinitionGrammar;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/** Check that a product definition matches the directory name. */
@Rule(key = ProductDefNameDoesNotMatchDirectoryNameCheck.CHECK_KEY)
public class ProductDefNameDoesNotMatchDirectoryNameCheck extends ProductDefCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "ProductDefNameDoesNotMatchDirectoryName";

  private static final String MESSAGE = "Product name does not match directory name.";

  private static final String DEFAULT_IGNORED_DIRECTORY_NAMES = "config,tests,unit_tests";

  @RuleProperty(
      key = "ignored directory names",
      defaultValue = "" + DEFAULT_IGNORED_DIRECTORY_NAMES,
      description = "List of ignored directory names, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String ignoredDirectoryNames = DEFAULT_IGNORED_DIRECTORY_NAMES;

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
    final String loweredDirectoryName = directoryName.toLowerCase();
    final Set<String> ignoredDirectoryNames = this.getIgnoredDirectoryNames();
    if (ignoredDirectoryNames.contains(loweredDirectoryName)) {
      return;
    }

    if (!productName.toLowerCase().equalsIgnoreCase(loweredDirectoryName)) {
      this.addIssue(productNameNode, MESSAGE);
    }
  }

  private Set<String> getIgnoredDirectoryNames() {
    return Arrays.stream(this.ignoredDirectoryNames.split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());
  }
}
