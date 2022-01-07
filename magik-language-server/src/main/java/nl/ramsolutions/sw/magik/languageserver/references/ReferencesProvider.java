package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References provider.
 */
public class ReferencesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencesProvider.class);

    /**
     * Set server capabilities.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setReferencesProvider(true);
    }

    /**
     * Provide references.
     * @param magikFile Magik file.
     * @param position Position in file.
     * @return Locations for references.
     */
    @SuppressWarnings("checkstyle:NestedIfDepth")
    public List<Location> provideReferences(final MagikTypedFile magikFile, final Position position) {
        // Parse magik.
        final AstNode node = magikFile.getTopNode();
        final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();

        // Should always be on an identifier.
        final AstNode currentNode =
            AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position), MagikGrammar.IDENTIFIER);
        if (currentNode == null) {
            return Collections.emptyList();
        }

        final AstNode wantedNode = currentNode.getFirstAncestor(
            MagikGrammar.METHOD_INVOCATION,
            MagikGrammar.METHOD_DEFINITION);
        LOGGER.trace("Wanted node: {}", wantedNode);
        if (wantedNode == null) {
            return Collections.emptyList();
        } else if (wantedNode.is(MagikGrammar.METHOD_INVOCATION)) {
            final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(wantedNode);
            final String methodName = helper.getMethodName();
            LOGGER.debug("Getting references to method: {}", methodName);
            return this.referencesToMethod(typeKeeper, methodName);
        } else if (wantedNode.is(MagikGrammar.METHOD_DEFINITION)) {
            final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(wantedNode);
            final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
            final String identifier = currentNode.getTokenValue();

            // Name of the method.
            if (this.nodeIsMethodDefinitionMethodName(currentNode, wantedNode)) {
                String methodName = helper.getMethodName();
                LOGGER.debug("Getting references to method: {}", methodName);
                return this.referencesToMethod(typeKeeper, methodName);
            } else if (this.nodeIsMethodDefinitionExemplarName(currentNode, wantedNode)) {
                // Must be the exemplar name.
                final String pakkage = packageHelper.getCurrentPackage();
                final GlobalReference globalRef = GlobalReference.of(pakkage, identifier);
                final AbstractType type = magikFile.getTypeKeeper().getType(globalRef);
                if (type != UndefinedType.INSTANCE) {
                    final String typeName = type.getFullName();
                    LOGGER.debug("Getting references to type: {}", typeName);
                    return this.referencesToType(typeKeeper, typeName);
                }
            } else {
                // A random identifier, regard it as a type.
                final String pakkage = packageHelper.getCurrentPackage();
                final GlobalReference globalRef = GlobalReference.of(pakkage, identifier);
                final AbstractType type = magikFile.getTypeKeeper().getType(globalRef);
                if (type != UndefinedType.INSTANCE) {
                    final String typeName = type.getFullName();
                    LOGGER.debug("Getting references to method: {}", typeName);
                    return this.referencesToType(typeKeeper, typeName);
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean nodeIsMethodDefinitionMethodName(final AstNode currentNode, final AstNode methodDefinitionNode) {
        // TODO: This does not support [] methods.
        final List<AstNode> identifierNodes = methodDefinitionNode.getChildren(MagikGrammar.IDENTIFIER);
        final AstNode methodNameNode = identifierNodes.get(1);
        return methodNameNode.getToken() == currentNode.getToken();
    }

    private boolean nodeIsMethodDefinitionExemplarName(final AstNode currentNode, final AstNode methodDefinitionNode) {
        final List<AstNode> identifierNodes = methodDefinitionNode.getChildren(MagikGrammar.IDENTIFIER);
        final AstNode exemplarNameNode = identifierNodes.get(0);
        return exemplarNameNode.getToken() == currentNode.getToken();
    }

    private List<Location> referencesToMethod(final ITypeKeeper typeKeeper, final String methodName) {
        LOGGER.debug("Finding references to method: {}", methodName);

        // Find references.
        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getMethods().stream())
            .filter(method -> method.getCalledMethods().contains(methodName))
            // TODO: can't we get the location to the usage?
            .map(Method::getLocation)
            .filter(Objects::nonNull)
            .map(Lsp4jConversion::locationToLsp4j)
            .collect(Collectors.toList());
    }

    private List<Location> referencesToType(final ITypeKeeper typeKeeper, final String typeName) {
        LOGGER.debug("Finding references to type: {}", typeName);

        // Find references.
        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getMethods().stream())
            .filter(method -> method.getUsedTypes().contains(typeName))
            // TODO: can't we get the location to the usage?
            .map(Method::getLocation)
            .filter(Objects::nonNull)
            .map(Lsp4jConversion::locationToLsp4j)
            .collect(Collectors.toList());
    }

}
