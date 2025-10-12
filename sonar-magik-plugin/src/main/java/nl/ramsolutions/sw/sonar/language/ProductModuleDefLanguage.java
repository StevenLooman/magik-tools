package nl.ramsolutions.sw.sonar.language;

import java.util.Arrays;
import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/**
 * Smallworld product.def and module.def languages.
 *
 * <p>Combined into one {@link AbstractLanguage} because they share the same file suffixes.
 */
public class ProductModuleDefLanguage extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "product_module_def";

  /** Name for language. */
  public static final String NAME = "Product Module Definition";

  /** Category for language. */
  public static final String PRODUCT_DEF_CATEGORY = "product_module_def";

  /** File suffixes key. */
  public static final String FILE_SUFFIXES_KEY = "sonar.product_module_def.file.suffixes";

  /** File suffixes. */
  public static final String DEFAULT_FILE_SUFFIXES = ".def";

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public ProductModuleDefLanguage(final Configuration configuration) {
    super(ProductModuleDefLanguage.KEY, ProductModuleDefLanguage.NAME);
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    final String[] stringArray =
        this.configuration.getStringArray(ProductModuleDefLanguage.FILE_SUFFIXES_KEY);
    final String[] suffixes =
        Arrays.stream(stringArray)
            .filter(s -> s != null)
            .filter(s -> !s.trim().isEmpty())
            .toArray(String[]::new);
    return suffixes.length == 0
        ? ProductModuleDefLanguage.DEFAULT_FILE_SUFFIXES.split(",")
        : suffixes;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + Objects.hash(this.configuration);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (!super.equals(obj)) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final ProductModuleDefLanguage other = (ProductModuleDefLanguage) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
