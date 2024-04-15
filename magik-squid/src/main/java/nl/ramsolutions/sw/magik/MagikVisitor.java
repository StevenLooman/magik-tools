package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.MagikAstWalker;

/** Magik visitor. */
public abstract class MagikVisitor extends MagikAstWalker {

  private MagikFile magikFile;

  public MagikFile getMagikFile() {
    return this.magikFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedMagikFile Context to use.
   */
  public void scanFile(final MagikFile scannedMagikFile) {
    this.magikFile = scannedMagikFile;

    final AstNode topNode = this.magikFile.getTopNode();
    this.walkAst(topNode);
  }
}
