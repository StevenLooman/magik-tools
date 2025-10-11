package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.checks.productdef.ProductDefMissingTitleCheck;

/** product.def {@link Check} list. */
public class ProductDefCheckList {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "product.def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/productdef/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_LOCATION = PROFILE_DIR + "/Sonar_way_profile.json";

  private ProductDefCheckList() {}

  /**
   * Get the list of {@link ProductDefCheck}s.
   *
   * @return List of {@link ProductDefCheck}s.
   */
  public static List<Class<? extends ProductDefCheck>> getChecks() {
    return List.of(ProductDefMissingTitleCheck.class);
  }

  /**
   * Get the list of {@link ProductDefCheck}s, casted to {@link Check}s.
   *
   * @return List of {@link Check}s.
   */
  public static List<Class<? extends Check>> getBaseChecks() {
    return getChecks().stream()
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
