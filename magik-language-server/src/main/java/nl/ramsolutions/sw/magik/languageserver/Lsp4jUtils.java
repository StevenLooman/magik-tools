package nl.ramsolutions.sw.magik.languageserver;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import nl.ramsolutions.sw.OpenedFile;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
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
   * @param openedFile The {@link OpenedFile}.
   * @param description The description.
   * @param textEdits The {@link TextEdit}s.
   * @return The {@link CodeAction}.
   */
  public static CodeAction createCodeAction(
      final OpenedFile openedFile,
      final String description,
      final List<nl.ramsolutions.sw.magik.TextEdit> textEdits) {
    return createCodeAction(openedFile, description, textEdits, CodeActionKind.QuickFix, null);
  }

  /**
   * Create a {@link CodeAction} from a domain {@link nl.ramsolutions.sw.magik.CodeAction}.
   *
   * @param openedFile The {@link OpenedFile}.
   * @param domainCodeAction The domain {@link nl.ramsolutions.sw.magik.CodeAction}.
   * @return The LSP4J {@link CodeAction}.
   */
  public static CodeAction createCodeAction(
      final OpenedFile openedFile, final nl.ramsolutions.sw.magik.CodeAction domainCodeAction) {
    final String kind = domainCodeAction.getKind();
    final String lsp4jKind = kind != null ? kind : CodeActionKind.QuickFix;

    Command lsp4jCommand = null;
    final nl.ramsolutions.sw.magik.CodeAction.Command domainCommand = domainCodeAction.getCommand();
    if (domainCommand != null) {
      lsp4jCommand =
          new Command(domainCommand.title(), domainCommand.commandId(), domainCommand.arguments());
    }

    return createCodeAction(
        openedFile,
        domainCodeAction.getTitle(),
        domainCodeAction.getEdits(),
        lsp4jKind,
        lsp4jCommand);
  }

  /**
   * Create a {@link CodeAction} with explicit kind and optional command.
   *
   * @param openedFile The {@link OpenedFile}.
   * @param description The description.
   * @param textEdits The {@link TextEdit}s.
   * @param kind The {@link CodeActionKind}.
   * @param command Optional {@link Command} to execute after applying edits.
   * @return The {@link CodeAction}.
   */
  public static CodeAction createCodeAction(
      final OpenedFile openedFile,
      final String description,
      final List<nl.ramsolutions.sw.magik.TextEdit> textEdits,
      final String kind,
      final Command command) {
    final List<org.eclipse.lsp4j.TextEdit> lsp4jTextEdits =
        textEdits.stream().map(Lsp4jConversion::textEditToLsp4j).toList();
    final VersionedTextDocumentIdentifier versionedTextDocumentIdentifier =
        new VersionedTextDocumentIdentifier(openedFile.getUri().toString(), TEXT_DOCUMENT_VERSION);
    final TextDocumentEdit textDocumentEdit =
        new TextDocumentEdit(versionedTextDocumentIdentifier, lsp4jTextEdits);
    final WorkspaceEdit workspaceEdit =
        new WorkspaceEdit(List.of(Either.forLeft(textDocumentEdit)));

    final CodeAction codeAction = new CodeAction(description);
    codeAction.setKind(kind);
    codeAction.setEdit(workspaceEdit);
    if (command != null) {
      codeAction.setCommand(command);
    }
    return codeAction;
  }
}
