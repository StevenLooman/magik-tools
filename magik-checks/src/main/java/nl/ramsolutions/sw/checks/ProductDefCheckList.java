package nl.ramsolutions.sw.checks;

import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.checks.productdef.ProductDefMissingDescriptionCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefMissingTitleCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefNameDoesNotMatchDirectoryNameCheck;
import nl.ramsolutions.sw.checks.productdef.ProductDefSyntaxErrorCheck;

/** product.def {@link Check} list. */
public class ProductDefCheckList extends CheckList<ProductDefCheck, ProductDefCodeActionSupplier> {

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String REPOSITORY_KEY = "product_def";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final String PROFILE_DIR = "nl/ramsolutions/sw/sonar/l10n/productdef/rules";

  @SuppressWarnings("checkstyle:JavadocVariable")
  public static final ProductDefCheckList INSTANCE = new ProductDefCheckList();

  private ProductDefCheckList() {}

  /**
   * Get the list of {@link ProductDefCheck}s.
   *
   * @return List of {@link ProductDefCheck}s.
   */
  @Override
  public List<Class<? extends ProductDefCheck>> getChecks() {
    return List.of(
        ProductDefMissingDescriptionCheck.class,
        ProductDefMissingTitleCheck.class,
        ProductDefNameDoesNotMatchDirectoryNameCheck.class,
        ProductDefSyntaxErrorCheck.class);
  }

  /**
   * Get the {@link ProductDefCheck}s which have a {@link ProductDefCodeActionSupplier}.
   *
   * @return Map of {@link ProductDefCheck}s and their {@link ProductDefCodeActionSupplier}s.
   */
  @Override
  public Map<Class<? extends ProductDefCheck>, List<Class<? extends ProductDefCodeActionSupplier>>>
      getFixers() {
    return Map.of();
  }
}
