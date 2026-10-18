package nl.ramsolutions.sw.translation;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.translation.analysis.TranslationAstWalker;

/** Translation file visitor. */
public abstract class TranslationVisitor extends TranslationAstWalker {

  private TranslationFile translationFile;

  public TranslationFile getTranslationFile() {
    return this.translationFile;
  }

  /**
   * Scan the file.
   *
   * @param scannedTranslationFile Context to use.
   */
  public void scanFile(final TranslationFile scannedTranslationFile) {
    this.translationFile = scannedTranslationFile;

    final AstNode topNode = this.translationFile.getTopNode();
    this.walkAst(topNode);
  }
}
