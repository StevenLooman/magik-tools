package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.Nullable;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import org.eclipse.lsp4j.Position;

/**
 * Shared, pre-computed context passed to each {@link CompletionModule}.
 *
 * @param magikFile The usable (possibly re-parsed) Magik file.
 * @param position The original LSP position.
 * @param removedPart The stripped current token (e.g. {@code "_"}, {@code ".foo"}).
 * @param tokenNode The token node at the (adjusted) position, or {@code null}.
 * @param replaceRange The range of the identifier being completed, from the original source (used
 *     by identifier-word completions to replace a package prefix), or {@code null}.
 */
public record CompletionContext(
    MagikTypedFile magikFile,
    Position position,
    String removedPart,
    @Nullable AstNode tokenNode,
    @Nullable Range replaceRange) {

  /**
   * Get the top AST node of the file.
   *
   * @return Top node.
   */
  public AstNode getTopNode() {
    return this.magikFile().getTopNode();
  }
}
