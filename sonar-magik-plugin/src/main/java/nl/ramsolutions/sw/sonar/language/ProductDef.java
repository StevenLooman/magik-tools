package nl.ramsolutions.sw.sonar.language;

import java.util.Objects;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

/** Smallworld product.def language. */
public class ProductDef extends AbstractLanguage {

  /** Key for language. */
  public static final String KEY = "product.def";

  /** Name for language. */
  public static final String NAME = "Product Definition";

  /** Category for language. */
  public static final String PRODUCT_DEF_CATEGORY = "product.def";

  /** File suffixes. */
  public static final String[] FILE_SUFFIXES = new String[] {".def"};

  private final Configuration configuration;

  /**
   * Constructor.
   *
   * @param configuration Configuration.
   */
  public ProductDef(final Configuration configuration) {
    super(ProductDef.KEY, ProductDef.NAME);
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sonar.api.resources.AbstractLanguage#getFileSuffixes()
   */
  @Override
  public String[] getFileSuffixes() {
    return ProductDef.FILE_SUFFIXES;
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

    final ProductDef other = (ProductDef) obj;
    return Objects.equals(this.configuration, other.configuration);
  }
}
