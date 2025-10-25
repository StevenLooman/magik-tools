package nl.ramsolutions.sw.checks.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.DisabledByDefault;
import nl.ramsolutions.sw.checks.MagikCheck;
import nl.ramsolutions.sw.magik.analysis.helpers.PragmaNodeHelper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check if the pragma topics include the mandatory topics (can be module name, product name or a
 * list of mandatory topics or a combination of all three).
 */
@DisabledByDefault
@Rule(key = PragmaDoesNotIncludeMandatoryTopicCheck.CHECK_KEY)
public class PragmaDoesNotIncludeMandatoryTopicCheck extends MagikCheck {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String CHECK_KEY = "PragmaDoesNotIncludeMandatoryTopic";

  private static final String MODULE_MESSAGE = "Module name '%s' is missing in pragma topics.";
  private static final String PRODUCT_MESSAGE = "Product name '%s' is missing in pragma topics.";
  private static final String TOPICS_MESSAGE = "'%s' is missing in pragma topics.";

  private static final boolean DEFAULT_INCLUDE_MODULE_NAME = false;
  private static final boolean DEFAULT_INCLUDE_PRODUCT_NAME = false;
  private static final String DEFAULT_MANDATORY_TOPICS = "";

  /** Include module name in pragma topics. */
  @RuleProperty(
      key = "include module name",
      defaultValue = "" + DEFAULT_INCLUDE_MODULE_NAME,
      description = "Whether to include the module name in the pragma topics",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean includeModuleName = DEFAULT_INCLUDE_MODULE_NAME;

  /** Include product name in pragma topics. */
  @RuleProperty(
      key = "include product name",
      defaultValue = "" + DEFAULT_INCLUDE_PRODUCT_NAME,
      description = "Whether to include the product name in the pragma topics",
      type = "BOOLEAN")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public boolean includeProductName = DEFAULT_INCLUDE_PRODUCT_NAME;

  /** List of mandatory topics, separated by ','. */
  @RuleProperty(
      key = "mandatory topics",
      defaultValue = "" + DEFAULT_MANDATORY_TOPICS,
      description = "List of mandatory topics, separated by ','",
      type = "STRING")
  @SuppressWarnings("checkstyle:VisibilityModifier")
  public String mandatoryTopics = DEFAULT_MANDATORY_TOPICS;

  @Override
  protected void walkPostPragma(final AstNode node) {
    if (includeModuleName) {
      this.checkModuleName(node);
    }

    if (includeProductName) {
      this.checkProductName(node);
    }

    this.checkMandatoryTopics(node);
  }

  private void checkModuleName(final AstNode node) {
    final URI uri = node.getToken().getURI();
    final String moduleName = ModuleDefFile.getModuleNameForUri(uri);
    if (moduleName == null) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    if (!topics.contains(moduleName)) {
      final String message = MODULE_MESSAGE.formatted(moduleName);
      this.addIssue(node, message);
    }
  }

  private void checkProductName(final AstNode node) {
    final URI uri = node.getToken().getURI();
    final String productName = ProductDefFile.getProductNameForUri(uri);
    if (productName == null) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    if (!topics.contains(productName)) {
      final String message = PRODUCT_MESSAGE.formatted(productName);
      this.addIssue(node, message);
    }
  }

  private void checkMandatoryTopics(final AstNode node) {
    final Set<String> mandatoryTopicsItems = this.getMandatoryTopicsItems();
    if (mandatoryTopicsItems.isEmpty()) {
      return;
    }

    final PragmaNodeHelper helper = new PragmaNodeHelper(node);
    final Set<String> topics = helper.getTopics();
    for (final String mandatoryTopic : mandatoryTopicsItems) {
      if (!topics.contains(mandatoryTopic)) {
        final String message = TOPICS_MESSAGE.formatted(mandatoryTopic);
        this.addIssue(node, message);
      }
    }
  }

  private Set<String> getMandatoryTopicsItems() {
    return Arrays.stream(this.mandatoryTopics.split(","))
        .map(String::trim)
        .filter(topic -> !topic.isEmpty())
        .collect(Collectors.toSet());
  }
}
