package nl.ramsolutions.sw.moduledef;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.moduledef.analysis.ModuleDefAstWalker;

/** Magik visitor. */
public abstract class ModuleDefVisitor extends ModuleDefAstWalker {

  private ModuleDefFile moduleDefFile;

  public ModuleDefFile getModuleDefFile() {
    return this.moduleDefFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedModuleDefFile Context to use.
   */
  public void scanFile(final ModuleDefFile scannedModuleDefFile) {
    this.moduleDefFile = scannedModuleDefFile;

    final AstNode topNode = this.moduleDefFile.getTopNode();
    this.walkAst(topNode);
  }
}
