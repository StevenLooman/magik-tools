package nl.ramsolutions.sw.magik.typedchecks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.RecognitionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.definitions.ModuleDefinition;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.Rule;

/**
 * Check to test if the module is required for a used global.
 */
@Rule(key = ModuleRequiredForGlobalTypedCheck.CHECK_KEY)
public class ModuleRequiredForGlobalTypedCheck extends MagikTypedCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "ModuleRequiredForGlobal";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModuleRequiredForGlobalTypedCheck.class);
    private static final String MESSAGE = "Module '%s' defining global '%s' is not required";

    @Override
    protected void walkPostIdentifier(final AstNode node) {
        final AstNode parent = node.getParent();
        if (!parent.is(MagikGrammar.ATOM)) {
            return;
        }

        final GlobalScope globalScope = this.getMagikFile().getGlobalScope();
        final Scope scope = globalScope.getScopeForNode(node);
        if (scope == null) {
            return;
        }

        final ScopeEntry scopeEntry = scope.getScopeEntry(node);
        if (scopeEntry == null
            || !scopeEntry.isType(ScopeEntry.Type.GLOBAL)) {
            return;
        }

        final LocalTypeReasonerState state = this.getTypeReasonerState();
        final ExpressionResult result = state.getNodeType(parent);
        final AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == UndefinedType.INSTANCE) {
            return;
        }

        final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();

        // Get own module + requires.
        final URI uri = this.getMagikFile().getUri();
        final Path path = Path.of(uri);
        final ModuleDefinition moduleDefinition;
        try {
            moduleDefinition = ModuleDefinitionScanner.swModuleForPath(path);
        } catch (final RecognitionException exception) {
            LOGGER.warn("Unable to parse module.def");
            return;
        } catch (final IOException exception) {
            LOGGER.warn("Caught exception", exception);
            return;
        }
        if (moduleDefinition == null) {
            return;
        }

        // See if the target module is required.
        final String moduleName = moduleDefinition.getName();
        final TypeString typeString = type.getTypeString();
        definitionKeeper.getExemplarDefinitions(typeString).stream()
            .filter(def -> def.getModuleName() != null)
            .filter(def -> !this.isModuleRequired(moduleName, def.getModuleName()))
            .forEach(def -> {
                final String globalModuleName = def.getModuleName();
                final String typeStringStr = typeString.getFullString();
                final String message = String.format(MESSAGE, globalModuleName, typeStringStr);
                this.addIssue(node, message);
            });
    }

    private boolean isModuleRequired(final String currentModuleName, final String wantedModuleName) {
        if (currentModuleName.equals(wantedModuleName)) {
            return true;
        }

        // Recurse through module tree and test if the wanted module name is required.
        final IDefinitionKeeper definitionKeeper = this.getDefinitionKeeper();
        for (final ModuleDefinition moduleDefinition : definitionKeeper.getModuleDefinitions(currentModuleName)) {
            final List<String> requiredModuleNames = moduleDefinition.getRequireds();
            for (final String requiredModuleName : requiredModuleNames) {
                if (this.isModuleRequired(requiredModuleName, wantedModuleName)) {
                    return true;
                }
            }
        }

        return false;
    }

}
