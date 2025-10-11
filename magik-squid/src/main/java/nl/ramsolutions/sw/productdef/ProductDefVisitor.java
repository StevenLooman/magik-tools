package nl.ramsolutions.sw.productdef;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.productdef.analysis.ProductDefAstWalker;

/** Magik visitor. */
public abstract class ProductDefVisitor extends ProductDefAstWalker {

  private ProductDefFile productDefFile;

  public ProductDefFile getProductDefFile() {
    return this.productDefFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedProductDefFile Context to use.
   */
  public void scanFile(final ProductDefFile scannedProductDefFile) {
    this.productDefFile = scannedProductDefFile;

    final AstNode topNode = this.productDefFile.getTopNode();
    this.walkAst(topNode);
  }
}
