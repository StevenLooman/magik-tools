package nl.ramsolutions.sw.checks.productdef;

import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckAssert;
import nl.ramsolutions.sw.checks.ProductDefCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.productdef.ProductDefFile;

/**
 * Assertions for {@link ProductDefCheck} instances.
 *
 * <p>Use via {@link #assertThat(ProductDefCheck)}.
 */
public class ProductDefCheckAssert extends CheckAssert {

  protected ProductDefCheckAssert(final Check actual) {
    super(actual);
  }

  /**
   * Get a new instance for the given {@link Check}.
   *
   * @param actual {@link Check} instance.
   * @return Self.
   */
  public static ProductDefCheckAssert assertThat(final ProductDefCheck actual) {
    return new ProductDefCheckAssert(actual);
  }

  @Override
  protected OpenedFile createOpenedFile(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return new ProductDefFile(ProductDefFile.DEFAULT_URI, code, definitionKeeper, null);
  }

  @Override
  protected OpenedFile createOpenedFile(final URI uri, final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    return new ProductDefFile(uri, code, definitionKeeper, null);
  }
}
