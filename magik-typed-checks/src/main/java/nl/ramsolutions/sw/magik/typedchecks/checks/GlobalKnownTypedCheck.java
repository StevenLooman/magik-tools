package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry.Type;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;

/**
 * Check if global is a known global.
 */
public class GlobalKnownTypedCheck extends MagikTypedCheck {

    private static final String MESSAGE = "Unknown global: %s";
    private static final String DEFAULT_PACKAGE = "user";

    private String currentPakkage = DEFAULT_PACKAGE;

    @Override
    protected void walkPostPackageSpecification(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        this.currentPakkage = identifierNode.getTokenValue();
    }

    @Override
    protected void walkPostIdentifier(final AstNode node) {
        final AstNode parentNode = node.getParent();
        if (parentNode.isNot(MagikGrammar.ATOM)) {
            return;
        }

        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);

        final String identifier = node.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        if (scopeEntry == null) {
            return;
        }

        if (!scopeEntry.isType(Type.GLOBAL)
            && !scopeEntry.isType(Type.DYNAMIC)) {
            return;
        }

        final ITypeKeeper typeKeeper = this.getTypeKeeper();
        final GlobalReference globalRef = identifier.indexOf(':') != -1
            ? GlobalReference.of(identifier)
            : GlobalReference.of(this.currentPakkage, identifier);
        if (typeKeeper.hasType(globalRef)) {
            return;
        }

        final String message = String.format(MESSAGE, identifier);
        this.addIssue(node, message);
    }

}
