package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.Objects;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry.Type;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.sonar.check.Rule;

/**
 * Check if global is a known global.
 */
@Rule(key = GlobalKnownTypedCheck.CHECK_KEY)
public class GlobalKnownTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "GlobalKnownTypedCheck";

    private static final String MESSAGE = "Unknown global: %s";
    private static final String DEFAULT_PACKAGE = "user";

    private String currentPakkage = DEFAULT_PACKAGE;

    @Override
    protected void walkPostPackageSpecification(final AstNode node) {
        final PackageNodeHelper helper = new PackageNodeHelper(node);
        this.currentPakkage = helper.getCurrentPackage();
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
        final TypeString typeString = TypeString.ofIdentifier(identifier, this.currentPakkage);
        if (typeKeeper.hasType(typeString)) {
            return;
        }

        final String message = String.format(MESSAGE, identifier);
        this.addIssue(node, message);
    }

}
