package nl.ramsolutions.sw.magik.languageserver;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentEdit;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** LSP4J utilities. */
public final class Lsp4jUtils {

  private static final int TEXT_DOCUMENT_VERSION = -1;

  private Lsp4jUtils() {
    // Utility class.
  }

  /** Comparator for {@link Position}s. */
  public static class PositionComparator implements Comparator<Position>, Serializable {

    @Override
    public int compare(final Position position0, final Position position1) {
      if (position0.getLine() != position1.getLine()) {
        return position0.getLine() - position1.getLine();
      }

      return position0.getCharacter() - position1.getCharacter();
    }
  }

  /**
   * Create a {@link CodeAction}.
   *
   * @param magikFile The {@link MagikTypedFile}.
   * @param description The description.
   * @param textEdits The {@link TextEdit}s.
   * @return The {@link CodeAction}.
   */
  public static CodeAction createCodeAction(
      final MagikTypedFile magikFile,
      final String description,
      final List<nl.ramsolutions.sw.magik.TextEdit> textEdits) {
    final List<org.eclipse.lsp4j.TextEdit> lsp4jTextEdits =
        textEdits.stream().map(Lsp4jConversion::textEditToLsp4j).collect(Collectors.toList());
    final VersionedTextDocumentIdentifier versionedTextDocumentIdentifier =
        new VersionedTextDocumentIdentifier(magikFile.getUri().toString(), TEXT_DOCUMENT_VERSION);
    final TextDocumentEdit textDocumentEdit =
        new TextDocumentEdit(versionedTextDocumentIdentifier, lsp4jTextEdits);
    final WorkspaceEdit workspaceEdit =
        new WorkspaceEdit(List.of(Either.forLeft(textDocumentEdit)));

    final CodeAction codeAction = new CodeAction(description);
    codeAction.setKind(CodeActionKind.QuickFix);
    codeAction.setEdit(workspaceEdit);
    return codeAction;
  }
}
