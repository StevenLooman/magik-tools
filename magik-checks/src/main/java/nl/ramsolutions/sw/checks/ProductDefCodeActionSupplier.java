package nl.ramsolutions.sw.checks;

import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.productdef.ProductDefFile;

/** {@link CodeAction} supplier for {@link ProductDefFile}s. */
public abstract class ProductDefCodeActionSupplier extends CodeActionSupplier {

  /**
   * Provide {@link CodeAction}s for {@link ProductDefFile}s.
   *
   * @param productDefFile {@link ProductDefFile} to provide fixes for.
   * @param range {@link Range} to provide fixes for.
   * @return List of {@link CodeAction}s to be applied.
   */
  public abstract List<CodeAction> provideProductDefCodeActions(
      final ProductDefFile productDefFile, final Range range);

  @Override
  public List<CodeAction> provideCodeActions(final OpenedFile file, final Range range) {
    if (!(file instanceof ProductDefFile productDefFile)) {
      throw new IllegalArgumentException(
          "OpenedFile is not a ProductDefFile: " + file.getClass().getName());
    }

    return this.provideProductDefCodeActions(productDefFile, range);
  }
}
