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
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
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
        final AstNode currentNode = AstQuery.nodeAt(node, position, MagikGrammar.IDENTIFIER);
        if (currentNode == null) {
            return Collections.emptyList();
        }

        final AstNode wantedNode = currentNode.getFirstAncestor(
            MagikGrammar.METHOD_INVOCATION,
            MagikGrammar.METHOD_NAME,
            MagikGrammar.EXEMPLAR_NAME,
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
        } else if (wantedNode.is(MagikGrammar.METHOD_NAME)) {
            final AstNode methodDefinitionNode = wantedNode.getParent();
            final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefinitionNode);
            final String methodName = helper.getMethodName();
            LOGGER.debug("Getting references to method: {}", methodName);
            return this.referencesToMethod(typeKeeper, UndefinedType.SERIALIZED_NAME, methodName);
        } else if (wantedNode.is(MagikGrammar.EXEMPLAR_NAME)) {
            final String identifier = currentNode.getTokenValue();
            final String pakkage = packageHelper.getCurrentPackage();
            final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
            final AbstractType type = typeKeeper.getType(typeString);
            if (type != UndefinedType.INSTANCE) {
                final String typeName = type.getFullName();
                LOGGER.debug("Getting references to type: {}", typeName);
                return this.referencesToType(typeKeeper, typeName);
            }
        } else if (wantedNode.is(MagikGrammar.ATOM)
                   && wantedNode.getFirstChild().is(MagikGrammar.IDENTIFIER)) {
            final Scope scope = magikFile.getGlobalScope().getScopeForNode(wantedNode);
            Objects.requireNonNull(scope);
            final String identifier = currentNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            if (scopeEntry == null) {
                return Collections.emptyList();
            } else if (scopeEntry.isType(
                    ScopeEntry.Type.DEFINITION,
                    ScopeEntry.Type.LOCAL,
                    ScopeEntry.Type.IMPORT,
                    ScopeEntry.Type.CONSTANT,
                    ScopeEntry.Type.PARAMETER)) {
                final List<AstNode> usages = scopeEntry.getUsages();
                return usages.stream()
                    .map(usageNode -> new Location(magikFile.getUri(), usageNode))
                    .collect(Collectors.toList());
            } else if (scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC)) {
                final String pakkage = packageHelper.getCurrentPackage();
                final TypeString typeString = TypeString.ofIdentifier(identifier, pakkage);
                final AbstractType type = typeKeeper.getType(typeString);
                if (type != UndefinedType.INSTANCE) {
                    final String typeName = type.getFullName();
                    LOGGER.debug("Getting references to type: {}", typeName);
                    return this.referencesToType(typeKeeper, typeName);
                }
            }
        } else if (wantedNode.is(MagikGrammar.CONDITION_NAME)) {
            final String conditionName = currentNode.getTokenValue();
            LOGGER.debug("Getting references to condition: {}", conditionName);
            return this.referencesToCondition(typeKeeper, conditionName);
        }

        // TODO: Slot references.

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
            .collect(Collectors.toList());
    }

    private List<Location> referencesToCondition(final ITypeKeeper typeKeeper, final String conditionName) {
        LOGGER.debug("Finding references to condition: {}", conditionName);

        return typeKeeper.getTypes().stream()
            .flatMap(type -> type.getLocalMethods().stream())
            .flatMap(method -> method.getConditionUsages().stream())
            .filter(conditionUsage -> conditionUsage.getConditionName().equals(conditionName))
            .map(Method.ConditionUsage::getLocation)
            .collect(Collectors.toList());
    }

}
