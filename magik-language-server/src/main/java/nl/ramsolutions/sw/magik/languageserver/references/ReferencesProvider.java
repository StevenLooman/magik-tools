package nl.ramsolutions.sw.magik.languageserver.references;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
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
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.ATOM,
            MagikGrammar.CONDITION_NAME);
        LOGGER.trace("Wanted node: {}", wantedNode);
        final PackageNodeHelper packageHelper = new PackageNodeHelper(wantedNode);
        if (wantedNode == null) {
            return Collections.emptyList();
        } else if (wantedNode.is(MagikGrammar.METHOD_INVOCATION)) {
            final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(wantedNode);
            final String methodName = helper.getMethodName();
            LOGGER.debug("Getting references to method: {}", methodName);
            return this.referencesToMethod(typeKeeper, UndefinedType.SERIALIZED_NAME, methodName);
        } else if (wantedNode.is(MagikGrammar.METHOD_DEFINITION)) {
            final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(wantedNode);
            final String identifier = currentNode.getTokenValue();

            // Name of the method.
            if (currentNode.getFirstAncestor(MagikGrammar.METHOD_NAME) != null) {
                final String methodName = helper.getMethodName();
                LOGGER.debug("Getting references to method: {}", methodName);
                return this.referencesToMethod(typeKeeper, UndefinedType.SERIALIZED_NAME, methodName);
            } else if (currentNode.getFirstAncestor(MagikGrammar.EXEMPLAR_NAME) != null) {
                // Must be the exemplar name.
                final String pakkage = packageHelper.getCurrentPackage();
                final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
                final AbstractType type = typeKeeper.getType(typeString);
                if (type != UndefinedType.INSTANCE) {
                    final String typeName = type.getFullName();
                    LOGGER.debug("Getting references to type: {}", typeName);
                    return this.referencesToType(typeKeeper, typeName);
                }
            }
        } else if (wantedNode.is(MagikGrammar.ATOM)
                   && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
            // A random identifier, regard it as a type.
            final String pakkage = packageHelper.getCurrentPackage();
            final String identifier = currentNode.getTokenValue();
            final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
            final AbstractType type = typeKeeper.getType(typeString);
            if (type != UndefinedType.INSTANCE) {
                final String typeName = type.getFullName();
                LOGGER.debug("Getting references to type: {}", typeName);
                return this.referencesToType(typeKeeper, typeName);
            }
        } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
            final String conditionName = currentNode.getTokenValue();
            LOGGER.debug("Getting references to condition: {}", conditionName);
            return this.referencesToCondition(typeKeeper, conditionName);
        }

        return Collections.emptyList();
    }

    private List<Location> referencesToMethod(
            final ITypeKeeper typeKeeper, final String typeName, final String methodName) {
        LOGGER.debug("Finding references to method: {}", methodName);

        // Build set of types which may contain this method: type + ancestors.
        final AbstractType typeType = typeName.equals(UndefinedType.SERIALIZED_NAME)
            ? UndefinedType.INSTANCE
            : typeKeeper.getType(TypeString.ofIdentifier(typeName, TypeString.DEFAULT_PACKAGE));
        final Set<AbstractType> wantedTypes = new HashSet<>();
        wantedTypes.add(UndefinedType.INSTANCE);  // For unreasoned/undetermined calls.
        wantedTypes.add(typeType);
        wantedTypes.addAll(typeType.getAncestors());

        final Collection<Method.MethodUsage> wantedMethodUsages = wantedTypes.stream()
            .map(wantedType -> {
                final String wantedTypeName = wantedType.getFullName();
                final TypeString wantedTypeRef = TypeString.ofIdentifier(wantedTypeName, TypeString.DEFAULT_PACKAGE);
                return new Method.MethodUsage(wantedTypeRef, methodName);
            })
            .collect(Collectors.toSet());
        final Predicate<Method.MethodUsage> filterPredicate = wantedMethodUsages::contains;

        // Find references.
        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getMethods().stream())
            .flatMap(method -> method.getMethodUsages().stream())
            .filter(filterPredicate::test)
            .map(Method.MethodUsage::getLocation)
            .filter(Objects::nonNull)
            .map(Lsp4jConversion::locationToLsp4j)
            .collect(Collectors.toList());
    }

    private List<Location> referencesToType(final ITypeKeeper typeKeeper, final String typeName) {
        LOGGER.debug("Finding references to type: {}", typeName);

        final TypeString typeStr = TypeString.ofIdentifier(typeName, TypeString.DEFAULT_PACKAGE);
        final AbstractType typeType = typeKeeper.getType(typeStr);
        final Set<AbstractType> wantedTypes = new HashSet<>();
        wantedTypes.add(typeType);
        // wantedTypes.addAll(type.getAncestors());  // TODO: Ancestors or descendants?

        final Collection<Method.GlobalUsage> wantedGlobalUsages = wantedTypes.stream()
            .map(wantedType -> {
                final String wantedTypeName = wantedType.getFullName();
                final TypeString wantedTypeRef = TypeString.ofIdentifier(wantedTypeName, TypeString.DEFAULT_PACKAGE);
                return new Method.GlobalUsage(wantedTypeRef, null);
            })
            .collect(Collectors.toSet());
        final Predicate<Method.GlobalUsage> filterPredicate = wantedGlobalUsages::contains;

        // Find references.
        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getMethods().stream())
            .flatMap(method -> method.getGlobalUsages().stream())
            .filter(filterPredicate::test)
            .map(Method.GlobalUsage::getLocation)
            .filter(Objects::nonNull)
            .map(Lsp4jConversion::locationToLsp4j)
            .collect(Collectors.toList());
    }

    private List<Location> referencesToCondition(final ITypeKeeper typeKeeper, final String conditionName) {
        LOGGER.debug("Finding references to condition: {}", conditionName);

        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getLocalMethods().stream())
            .flatMap(method -> method.getConditionUsages().stream())
            .filter(conditionUsage -> conditionUsage.getConditionName().equals(conditionName))
            .map(Method.ConditionUsage::getLocation)
            .map(Lsp4jConversion::locationToLsp4j)
            .collect(Collectors.toList());
    }

}
