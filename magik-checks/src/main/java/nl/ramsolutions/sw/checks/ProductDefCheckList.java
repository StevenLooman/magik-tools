package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.productdef.ProductDefMissingDescriptionCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefMissingTitleCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefNameDoesNotMatchDirectoryNameCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefSyntaxErrorCheck;

/** product.def {@link Check} list. */
public class ProductDefCheckList {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "product_def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/productdef/rules";

  private ProductDefCheckList() {}

  /**
   * Get the list of {@link ProductDefCheck}s.
   *
   * @return List of {@link ProductDefCheck}s.
   */
  public static List<Class<? extends ProductDefCheck>> getChecks() {
    return List.of(
        ProductDefMissingDescriptionCheck.class,
        ProductDefMissingTitleCheck.class,
        ProductDefNameDoesNotMatchDirectoryNameCheck.class,
        ProductDefSyntaxErrorCheck.class);
  }

  /**
   * Get the list of {@link ProductDefCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public static List<Class<? extends Check>> getBaseChecks() {
    return ProductDefCheckList.getChecks().stream()
        .map(clazz -> (Class<? extends Check>) clazz)
        .collect(Collectors.toList());
  }

  /**
   * Get the {@link ProductDefCheck}s which have a {@link CheckFixer}.
   *
   * @return Map of {@link ProductDefCheck}s and their {@link CheckFixer}s.
   */
  public static Map<Class<? extends ProductDefCheck>, List<Class<? extends CheckFixer>>>
      getFixers() {
    return Map.of();
  }
}
