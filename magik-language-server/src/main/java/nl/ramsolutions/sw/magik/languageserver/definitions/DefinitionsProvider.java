package nl.ramsolutions.sw.magik.languageserver.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Definitions provider.
 */
public class DefinitionsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefinitionsProvider.class);

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setDefinitionProvider(true);
    }

    /**
     * Provide definitions.
     * @param magikFile Magik file.
     * @param position Position.
     * @return Definitions.
     */
    public List<Location> provideDefinitions(final MagikTypedFile magikFile, final Position position) {
        // Parse magik.
        final AstNode node = magikFile.getTopNode();
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();

        // Should always be on an identifier.
        final AstNode currentNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
        if (currentNode == null) {
            return Collections.emptyList();
        }

        final AstNode wantedNode = currentNode.getFirstAncestor(
            MagikGrammar.METHOD_INVOCATION,
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.ATOM,
            MagikGrammar.CONDITION_NAME);
        LOGGER.trace("Wanted node: {}", wantedNode);
        final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
        if (wantedNode == null) {
            return Collections.emptyList();
        } else if (wantedNode.is(MagikGrammar.ATOM)
                   && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
            final Scope scope = magikFile.getGlobalScope().getScopeForNode(wantedNode);
            final String identifier = wantedNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            if (scopeEntry == null) {
                return Collections.emptyList();
            }

            if (scopeEntry.isType(
                    ScopeEntry.Type.DEFINITION,
                    ScopeEntry.Type.LOCAL,
                    ScopeEntry.Type.IMPORT,
                    ScopeEntry.Type.CONSTANT,
                    ScopeEntry.Type.PARAMETER)) {
                final AstNode definitionNode = scopeEntry.getDefinitionNode();
                final Location definitionLocation = new Location(magikFile.getUri(), definitionNode);
                return List.of(definitionLocation);
            }

            // Assume type.
            final String pakkage = packageHelper.getCurrentPackage();
            final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
            final AbstractType type = typeKeeper.getType(typeString);
            if (type != UndefinedType.INSTANCE
                && type.getLocation() != null) {
                final Location typeLocation = type.getLocation();
                return List.of(typeLocation);
            }
        } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
            final String conditionName = currentNode.getTokenValue();
            final Condition condition = typeKeeper.getCondition(conditionName);
            if (condition != null
                && condition.getLocation() != null) {
                final Location conditionLocation = condition.getLocation();
                return List.of(conditionLocation);
            }
        }

        return Collections.emptyList();
    }

}
