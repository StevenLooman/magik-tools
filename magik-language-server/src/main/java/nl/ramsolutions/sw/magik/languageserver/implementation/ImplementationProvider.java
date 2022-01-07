package nl.ramsolutions.sw.magik.languageserver.implementation;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation provider.
 */
public class ImplementationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImplementationProvider.class);

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setImplementationProvider(true);
    }

    /**
     * Provide implementations for {{position}} in {{path}}.
     * @param magikFile Magik file.
     * @param position Location in file.
     * @return List of Locations for implementation.
     */
    public List<org.eclipse.lsp4j.Location> provideImplementations(
            final MagikTypedFile magikFile, final Position position) {
        // Parse and reason magik.
        final AstNode node = magikFile.getTopNode();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();

        // Get method implementations.
        AstNode currentNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
        final List<org.eclipse.lsp4j.Location> locations = new ArrayList<>();
        while (currentNode != null) {
            // If current node is a method invocation, go to that method.
            if (currentNode.is(MagikGrammar.METHOD_INVOCATION)) {
                final List<org.eclipse.lsp4j.Location> methodLocations =
                    this.implementationsForMethodInvocation(typeKeeper, reasoner, currentNode);
                locations.addAll(methodLocations);
            }

            currentNode = currentNode.getParent();
        }

        return locations;
    }

    private List<org.eclipse.lsp4j.Location> implementationsForMethodInvocation(
            final ITypeKeeper typeKeeper, final LocalTypeReasoner reasoner, final AstNode currentNode) {
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(currentNode);
        final String methodName = helper.getMethodName();

        final AstNode previousSiblingNode = currentNode.getPreviousSibling();
        final ExpressionResult result = reasoner.getNodeType(previousSiblingNode);

        final AbstractType unsetType = typeKeeper.getType(GlobalReference.of("sw:unset"));
        AbstractType type = result.get(0, unsetType);
        final List<org.eclipse.lsp4j.Location> locations = new ArrayList<>();
        if (type == UndefinedType.INSTANCE) {
            LOGGER.debug("Finding implementations for method: {}", methodName);
            typeKeeper.getTypes().stream()
                .flatMap(anyType -> anyType.getMethods().stream())
                .filter(m -> m.getName().equals(methodName))
                .map(Method::getLocation)
                .map(Lsp4jConversion::locationToLsp4j)
                .forEach(locations::add);
        } else {
            if (type == SelfType.INSTANCE) {
                final AstNode methodDefNode = currentNode.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
                final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(methodDefNode);
                final GlobalReference globalRef = methodDefHelper.getTypeGlobalReference();
                type = typeKeeper.getType(globalRef);
            }
            LOGGER.debug("Finding implementations for type:, {}, method: {}", type.getFullName(), methodName);
            type.getMethods(methodName).stream()
                .map(Method::getLocation)
                .map(Lsp4jConversion::locationToLsp4j)
                .forEach(locations::add);
        }

        return locations;
    }

}
