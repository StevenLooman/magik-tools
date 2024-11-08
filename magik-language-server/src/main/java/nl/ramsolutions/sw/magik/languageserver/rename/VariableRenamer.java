package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.PrepareRenameResult;

/** Magik variable renamer. */
class VariableRenamer extends Renamer {

  VariableRenamer(final MagikTypedFile magikFile, final AstNode node) {
    super(magikFile, node);
  }

  @Override
  PrepareRenameResult prepareRename() {
    final AstNode node = this.getNode();
    final MagikTypedFile magikFile = this.getMagikFile();

    // Set up scope.
    final AstNode parentNode = node.getParent();
    final ScopeEntry scopeEntry = VariableRenamer.findScopeEntry(magikFile, parentNode);
    if (scopeEntry == null
        || scopeEntry.isType(ScopeEntry.Type.GLOBAL)
        || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)
        || scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
      return null;
    }

    final Range range = new Range(node);
    final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
    final String identifier = node.getTokenOriginalValue();
    return new PrepareRenameResult(rangeLsp4j, identifier);
  }

  @Override
  Map<URI, List<TextEdit>> provideRename(final String newName) {
    final AstNode node = this.getNode();
    final MagikTypedFile magikFile = this.getMagikFile();

    // Set up scope.
    final AstNode identifierNode = node.getParent();
    final ScopeEntry scopeEntry = VariableRenamer.findScopeEntry(magikFile, identifierNode);
    if (scopeEntry == null
        || scopeEntry.isType(
            ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC, ScopeEntry.Type.IMPORT)) {
      return Collections.emptyMap();
    }

    // Provide edits.
    final URI uri = magikFile.getUri();
    final AstNode definitionNode = scopeEntry.getDefinitionNode();
    final List<TextEdit> textEdits =
        Stream.concat(Stream.of(definitionNode), scopeEntry.getUsages().stream())
            .map(
                renameNode ->
                    renameNode.isNot(MagikGrammar.IDENTIFIER)
                        ? renameNode.getFirstChild(MagikGrammar.IDENTIFIER)
                        : renameNode)
            .map(Range::new)
            .map(range -> new TextEdit(range, newName))
            .toList();
    return Map.of(uri, textEdits);
  }

  static boolean canHandleRename(final MagikTypedFile magikFile, final AstNode node) {
    final AstNode parentNode = node.getParent();
    final ScopeEntry scopeEntry = VariableRenamer.findScopeEntry(magikFile, parentNode);
    if (scopeEntry == null
        || scopeEntry.isType(ScopeEntry.Type.GLOBAL)
        || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)
        || scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
      return false;
    }

    return true;
  }

  @CheckForNull
  private static ScopeEntry findScopeEntry(final MagikTypedFile magikFile, final AstNode node) {
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getScopeForNode(node);
    if (scope == null) {
      return null;
    }

    return scope.getScopeEntry(node);
  }
}
