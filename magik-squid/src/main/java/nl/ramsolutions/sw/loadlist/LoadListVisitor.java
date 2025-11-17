package nl.ramsolutions.sw.loadlist;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.loadlist.analysis.LoadListAstWalker;

/** Load list visitor. */
public abstract class LoadListVisitor extends LoadListAstWalker {

  private LoadListFile loadListFile;

  public LoadListFile getLoadListFile() {
    return this.loadListFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedLoadListFile Context to use.
   */
  public void scanFile(final LoadListFile scannedLoadListFile) {
    this.loadListFile = scannedLoadListFile;

    final AstNode topNode = this.loadListFile.getTopNode();
    this.walkAst(topNode);
  }
}
