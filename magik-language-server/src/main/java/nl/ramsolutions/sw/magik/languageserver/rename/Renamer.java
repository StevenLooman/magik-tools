package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.List;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.TextEdit;
import org.eclipse.lsp4j.PrepareRenameResult;

abstract class Renamer {

  private final MagikTypedFile magikFile;
  private final AstNode node;

  Renamer(final MagikTypedFile magikFile, final AstNode node) {
    this.magikFile = magikFile;
    this.node = node;
  }

  protected AstNode getNode() {
    return this.node;
  }

  protected MagikTypedFile getMagikFile() {
    return this.magikFile;
  }

  /**
   * Prepare rename, returns the range of the token to be renamed.
   *
   * @return The prepare result.
   */
  abstract PrepareRenameResult prepareRename();

  /**
   * Provide rename, provides the edits to be done.
   *
   * @param newName New name.
   * @return The {@link TextEdit}s to be done for renaming.
   */
  abstract Map<URI, List<TextEdit>> provideRename(final String newName);
}
