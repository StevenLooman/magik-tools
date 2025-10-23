package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Set;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.sonar.check.Rule;

/** Check if the pragma topics include the product name. */
@DisabledByDefault
@Rule(key = PragmaTopicsDoesNotIncludeProductNameCheck.CHECK_KEY)
public class PragmaTopicsDoesNotIncludeProductNameCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaTopicsDoesNotIncludeProductName";

  private static final String MESSAGE = "Product name '%s' is missing in pragma topics.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    final URI uri = node.getToken().getURI();
    final String productName = ProductDefFile.getProductNameForUri(uri);

    if (productName == null) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();

    if (!topics.contains(productName)) {
      final String message = MESSAGE.formatted(productName);
      this.addIssue(node, message);
    }
  }
}
