package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.RenameOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;

/** Rename provider. */
public class RenameProvider {

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    final RenameOptions renameOptions = new RenameOptions();
    renameOptions.setPrepareProvider(true);
    capabilities.setRenameProvider(renameOptions);
  }

  /**
   * Provide prepare rename.
   *
   * @param magikFile Magik file.
   * @param position Position in magik source.
   * @return Prepare rename or null if no rename possible.
   */
  public Either3<org.eclipse.lsp4j.Range, PrepareRenameResult, PrepareRenameDefaultBehavior>
      providePrepareRename(final MagikTypedFile magikFile, final Position position) {
    // Parse magik.
    final AstNode topNode = magikFile.getTopNode();

    // Should always be on an identifier.
    final AstNode node =
        AstQuery.nodeAt(
            topNode, Lsp4jConversion.positionFromLsp4j(position), MagikGrammar.IDENTIFIER);
    if (node == null) {
      return null;
    }

    final Renamer renamer = this.getRenamerForNode(magikFile, node);
    if (renamer == null) {
      return null;
    }

    final PrepareRenameResult result = renamer.prepareRename();
    if (result == null) {
      return null;
    }

    return Either3.forSecond(result);
  }

  /**
   * Provide rename.
   *
   * @param magikFile Magik file.
   * @param position Position in magik source.
   * @param newName New name.
   * @return Edits to workspace.
   */
  public WorkspaceEdit provideRename(
      final MagikTypedFile magikFile, final Position position, final String newName) {
    // Parse magik.
    final AstNode topNode = magikFile.getTopNode();

    // Should always be on an identifier.
    final AstNode node =
        AstQuery.nodeAt(
            topNode, Lsp4jConversion.positionFromLsp4j(position), MagikGrammar.IDENTIFIER);
    if (node == null) {
      return null;
    }

    final Renamer renamer = this.getRenamerForNode(magikFile, node);
    if (renamer == null) {
      return null;
    }

    final Map<URI, List<nl.ramsolutions.sw.magik.TextEdit>> uriEdits =
        renamer.provideRename(newName);
    final Map<String, List<TextEdit>> uriEditsLsp4j =
        uriEdits.entrySet().stream()
            .map(
                entry -> {
                  final String uriStr = entry.getKey().toString();
                  final List<TextEdit> editsLsp4j =
                      entry.getValue().stream().map(Lsp4jConversion::textEditToLsp4j).toList();
                  return Map.entry(uriStr, editsLsp4j);
                })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new WorkspaceEdit(uriEditsLsp4j);
  }

  @CheckForNull
  private Renamer getRenamerForNode(final MagikTypedFile magikFile, final AstNode node) {
    if (VariableRenamer.canHandleRename(magikFile, node)) {
      return new VariableRenamer(magikFile, node);
    } else if (MethodRenamer.canHandleRename(magikFile, node)) {
      return new MethodRenamer(magikFile, node);
    }

    return null;
  }
}
