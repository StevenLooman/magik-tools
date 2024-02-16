package nl.ramsolutions.sw.magik.languageserver.rename;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
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

/**
 * Rename provider.
 */
public class RenameProvider {

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        final RenameOptions renameOptions = new RenameOptions();
        renameOptions.setPrepareProvider(true);
        capabilities.setRenameProvider(renameOptions);
    }

    /**
     * Provide prepare rename.
     * @param magikFile Magik file.
     * @param position Position in magik source.
     * @return Prepare rename or null if no rename possible.
     */
    public Either3<org.eclipse.lsp4j.Range, PrepareRenameResult, PrepareRenameDefaultBehavior> providePrepareRename(
            final MagikTypedFile magikFile, final Position position) {
        // Parse magik.
        final AstNode topNode = magikFile.getTopNode();

        // Should always be on an identifier.
        final AstNode node = AstQuery.nodeAt(
            topNode,
            Lsp4jConversion.positionFromLsp4j(position),
            MagikGrammar.IDENTIFIER);
        if (node == null) {
            return null;
        }

        // Set up scope.
        final AstNode identifierNode = node.getParent();
        final ScopeEntry scopeEntry = this.findScopeEntry(magikFile, identifierNode);
        if (scopeEntry == null
            || scopeEntry.isType(ScopeEntry.Type.GLOBAL)
            || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)
            || scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
            return null;
        }

        final Range range = new Range(node);
        final org.eclipse.lsp4j.Range rangeLsp4j = Lsp4jConversion.rangeToLsp4j(range);
        final String identifier = node.getTokenOriginalValue();
        final PrepareRenameResult result = new PrepareRenameResult(rangeLsp4j, identifier);
        return Either3.forSecond(result);
    }

    /**
     * Provide rename.
     * @param magikFile Magik file.
     * @param position Position in magik source.
     * @param newName New name.
     * @return Edits to workspace.
     */
    public WorkspaceEdit provideRename(final MagikTypedFile magikFile, final Position position, final String newName) {
        // Parse magik.
        final AstNode topNode = magikFile.getTopNode();

        // Should always be on an identifier.
        final AstNode node = AstQuery.nodeAt(
            topNode,
            Lsp4jConversion.positionFromLsp4j(position),
            MagikGrammar.IDENTIFIER);
        if (node == null) {
            return null;
        }

        // Set up scope.
        final AstNode identifierNode = node.getParent();
        final ScopeEntry scopeEntry = this.findScopeEntry(magikFile, identifierNode);
        if (scopeEntry == null
            || scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC, ScopeEntry.Type.IMPORT)) {
            return null;
        }

        // Provide edits.
        final String uri = magikFile.getUri().toString();
        final AstNode definitionNode = scopeEntry.getDefinitionNode();
        final List<TextEdit> textEdits =
            Stream.concat(Stream.of(definitionNode), scopeEntry.getUsages().stream())
                .map(renameNode -> renameNode.isNot(MagikGrammar.IDENTIFIER)
                    ? renameNode.getFirstChild(MagikGrammar.IDENTIFIER)
                    : renameNode)
                .map(Range::new)
                .map(Lsp4jConversion::rangeToLsp4j)
                .map(range -> new TextEdit(range, newName))
                .collect(Collectors.toList());
        return new WorkspaceEdit(Map.of(uri, textEdits));
    }

    @CheckForNull
    private ScopeEntry findScopeEntry(final MagikTypedFile magikFile, final AstNode node) {
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        if (scope == null) {
            return null;
        }

        return scope.getScopeEntry(node);
    }

}
