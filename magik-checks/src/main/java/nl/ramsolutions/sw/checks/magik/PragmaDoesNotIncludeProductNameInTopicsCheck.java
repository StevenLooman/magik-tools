package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.util.Set;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.sonar.check.Rule;

/** Check if the pragma topics includes the product name. */
@DisabledByDefault
@Rule(key = PragmaDoesNotIncludeProductNameInTopicsCheck.CHECK_KEY)
public class PragmaDoesNotIncludeProductNameInTopicsCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaDoesNotIncludeProductNameInTopics";

  private static final String PRODUCT_MESSAGE = "Product name '%s' is missing in pragma topics.";

  @Override
  protected void walkPostPragma(final AstNode node) {
    this.checkProductName(node);
  }

  private void checkProductName(final AstNode node) {
    final ProductDefFile productDefFile = this.getMagikFile().getProductDefFile();
    if (productDefFile == null) {
      return;
    }

    final String productName = productDefFile.getProductDefinition().getName();
    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    if (!topics.contains(productName)) {
      final String message = PRODUCT_MESSAGE.formatted(productName);
      this.addIssue(node, message);
    }
  }
}
