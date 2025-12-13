package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
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
    final List<TextEdit> textEdits = new ArrayList<>();

    // Add variable usage edits.
    Stream.concat(Stream.of(definitionNode), scopeEntry.getUsages().stream())
        .map(
            renameNode ->
                renameNode.isNot(MagikGrammar.IDENTIFIER)
                    ? renameNode.getFirstChild(MagikGrammar.IDENTIFIER)
                    : renameNode)
        .map(Range::new)
        .map(range -> new TextEdit(range, newName))
        .forEach(textEdits::add);

    // Also rename in type doc.
    if (scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
      final String oldName = definitionNode.getTokenOriginalValue();
      final List<TextEdit> typeDocEdits = this.getTypeDocEdits(definitionNode, oldName, newName);
      textEdits.addAll(typeDocEdits);
    }

    // Sort edits by position (line, then column).
    textEdits.sort(
        Comparator.comparingInt((TextEdit edit) -> edit.getRange().getStartPosition().getLine())
            .thenComparingInt(edit -> edit.getRange().getStartPosition().getColumn()));

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

  /**
   * Get text edits for renaming parameter in type doc.
   *
   * @param definitionNode The parameter definition node.
   * @param oldName The old parameter name.
   * @param newName The new parameter name.
   * @return List of text edits for type doc parameter names.
   */
  private List<TextEdit> getTypeDocEdits(
      final AstNode definitionNode, final String oldName, final String newName) {
    // Find the parent method/procedure definition node.
    final AstNode methodNode =
        definitionNode.getFirstAncestor(
            MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
    if (methodNode == null) {
      return Collections.emptyList();
    }

    // Parse the type doc for the method/procedure.
    final TypeDocParser typeDocParser = new TypeDocParser(methodNode);
    final Map<AstNode, String> parameterNameNodes = typeDocParser.getParameterNameNodes();

    // Find parameter name nodes matching the old name and create text edits.
    return parameterNameNodes.entrySet().stream()
        .filter(entry -> entry.getValue().equals(oldName))
        .map(Map.Entry::getKey)
        .map(Range::new)
        .map(range -> new TextEdit(range, newName))
        .toList();
  }
}
