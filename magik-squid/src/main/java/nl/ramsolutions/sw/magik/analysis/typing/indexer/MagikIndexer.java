package nl.ramsolutions.sw.magik.analysis.typing.indexer;

import com.sonar.sslr.api.AstNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeKeeperDefinitionInserter;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes all the magik files in the workspace.
 *
 * <p>
 * Does the following:
 * - (re)build type hierarchy
 * - add slots to exemplars
 * - add methods to exemplars/mixins
 * </p>
 */
public class MagikIndexer {

    // TODO: Now that we no longer have the MagikPreIndexer, will every method definition land at the right type?
    //       I.e., in the case of these events, in order:
    //       - indexer sees type.method(), no _package, resolved to to user:type
    //       - indexer sees def_slotted_exemplar(:type, {}), _package sw, creates type sw:type
    //       Now there are two types `type`, one is from the method, other is from the real definition.
    //       Fix this by adding a post-process step? Or back to the MagikPreIndexer?

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikIndexer.class);

    private static final Map<ParameterDefinition.Modifier, Parameter.Modifier> PARAMETER_MODIFIER_MAPPING = Map.of(
        ParameterDefinition.Modifier.NONE, Parameter.Modifier.NONE,
        ParameterDefinition.Modifier.OPTIONAL, Parameter.Modifier.OPTIONAL,
        ParameterDefinition.Modifier.GATHER, Parameter.Modifier.GATHER);
    private static final Map<MethodDefinition.Modifier, Method.Modifier> METHOD_MODIFIER_MAPPING = Map.of(
        MethodDefinition.Modifier.ABSTRACT, Method.Modifier.ABSTRACT,
        MethodDefinition.Modifier.ITER, Method.Modifier.ITER,
        MethodDefinition.Modifier.PRIVATE, Method.Modifier.PRIVATE);

    private static final CommentInstructionReader.Instruction TYPE_INSTRUCTION =
        new CommentInstructionReader.Instruction("type", CommentInstructionReader.Instruction.Sort.STATEMENT);

    private final ITypeKeeper typeKeeper;
    private final TypeKeeperDefinitionInserter typeKeeperInserter;
    private final Map<Path, Set<Package>> indexedPackages = new HashMap<>();
    private final Map<Path, Set<AbstractType>> indexedTypes = new HashMap<>();
    private final Map<Path, Set<Method>> indexedMethods = new HashMap<>();
    private final Map<Path, Set<AliasType>> indexedGlobals = new HashMap<>();
    private final Map<Path, Set<BinaryOperator>> indexedBinaryOperators = new HashMap<>();
    private final Map<Path, Set<Condition>> indexedConditions = new HashMap<>();

    public MagikIndexer(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
        this.typeKeeperInserter = new TypeKeeperDefinitionInserter(typeKeeper);
    }

    /**
     * Index all magik file(s).
     * @param paths Paths to index.
     * @throws IOException -
     */
    public void indexPaths(final Stream<Path> paths) throws IOException {
        paths.forEach(this::indexPathCreated);
    }

    /**
     * Index a single magik file when it is created (or first read).
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathCreated(final Path path) {
        LOGGER.debug("Scanning created file: {}", path);

        try {
            this.scrubDefinitions(path);
            this.readDefinitions(path);
        } catch (Exception exception) {
            LOGGER.error("Error indexing created file: " + path, exception);
        }
    }

    /**
     * Index a single magik file when it is changed.
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathChanged(final Path path) {
        LOGGER.debug("Scanning changed file: {}", path);

        try {
            this.scrubDefinitions(path);
            this.readDefinitions(path);
        } catch (Exception exception) {
            LOGGER.error("Error indexing changed file: " + path, exception);
        }
    }

    /**
     * Un-index a single magik file when it is deleted.
     * @param path Path to magik file.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void indexPathDeleted(final Path path) {
        LOGGER.debug("Scanning deleted file: {}", path);

        try {
            this.scrubDefinitions(path);
        } catch (Exception exception) {
            LOGGER.error("Error indexing deleted file: " + path, exception);
        }
    }

    private void handleDefinition(final MagikFile magikFile, final Definition definition) {
        if (definition instanceof PackageDefinition) {
            final PackageDefinition packageDefinition = (PackageDefinition) definition;
            this.handleDefinition(magikFile, packageDefinition);
        } else if (definition instanceof ExemplarDefinition) {
            final ExemplarDefinition slottedExemplarDefinition = (ExemplarDefinition) definition;
            this.handleDefinition(magikFile, slottedExemplarDefinition);
        } else if (definition instanceof MethodDefinition) {
            final MethodDefinition methodDefinition = (MethodDefinition) definition;
            this.handleDefinition(magikFile, methodDefinition);
        } else if (definition instanceof GlobalDefinition) {
            final GlobalDefinition globalDefinition = (GlobalDefinition) definition;
            this.handleDefinition(magikFile, globalDefinition);
        } else if (definition instanceof BinaryOperatorDefinition) {
            final BinaryOperatorDefinition binaryOperatorDefinition = (BinaryOperatorDefinition) definition;
            this.handleDefinition(magikFile, binaryOperatorDefinition);
        } else if (definition instanceof ConditionDefinition) {
            final ConditionDefinition conditionDefinition = (ConditionDefinition) definition;
            this.handleDefinition(magikFile, conditionDefinition);
        }
    }

    private void handleDefinition(final MagikFile magikFile, final PackageDefinition definition) {
        final Package pakkage = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedPackages.get(path).add(pakkage);
    }

    private void handleDefinition(final MagikFile magikFile, final ExemplarDefinition definition) {
        final AbstractType magikType = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(magikType);
    }

    private void handleDefinition(final MagikFile magikFile, final MethodDefinition definition) {
        if (!definition.isActualMethodDefinition()) {
            // Slot accessors, shared variables, shared constants.
            this.handleMethodDefinitionOther(magikFile, definition);
            return;
        }

        final Method method = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);

        // TODO: Move saving used things to somewhere else.
        // Save used types.
        final MagikType magikType = method.getOwner();
        final URI uri = definition.getLocation().getUri();
        final AstNode node = definition.getNode();
        final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope bodyScope = globalScope.getScopeForNode(bodyNode);
        final PackageNodeHelper packageNodeHelper = new PackageNodeHelper(node);
        final String currentPakkage = packageNodeHelper.getCurrentPackage();
        Objects.requireNonNull(bodyScope);
        bodyScope.getSelfAndDescendantScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(scopeEntry -> scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC))
            .map(scopeEntry -> {
                final String identifier = scopeEntry.getIdentifier();
                final TypeString ref = TypeString.ofIdentifier(identifier, currentPakkage);
                AbstractType type = this.typeKeeper.getType(ref);
                if (type == UndefinedType.INSTANCE) {
                    return null;
                } else if (type == SelfType.INSTANCE) {
                    type = magikType;
                }
                final AstNode usageNode = scopeEntry.getDefinitionNode();
                final Location usageLocation = new Location(uri, usageNode);
                return new Method.GlobalUsage(ref, usageLocation);
            })
            .filter(Objects::nonNull)
            .forEach(method::addUsedGlobal);

        // Save called method names (thus without type).
        node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            .map(invocationNode -> {
                final MethodInvocationNodeHelper invocationHelper = new MethodInvocationNodeHelper(invocationNode);
                final TypeString usedTypeRef = TypeString.UNDEFINED;
                final String usedMethodName = invocationHelper.getMethodName();
                final Location methodUseLocation = new Location(uri, invocationNode);
                return new Method.MethodUsage(usedTypeRef, usedMethodName, methodUseLocation);
            })
            .forEach(method::addCalledMethod);

        // Save used slots.
        node.getDescendants(MagikGrammar.SLOT).stream()
            .map(slotNode -> {
                final String slotName = slotNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue();
                final Location slotUseLocation = new Location(uri, slotNode);
                return new Method.SlotUsage(slotName, slotUseLocation);
            })
            .forEach(method::addUsedSlot);

        // Save used conditions.
        node.getDescendants(MagikGrammar.CONDITION_NAME).stream()
            .map(conditionNameNode -> {
                final String conditionName = conditionNameNode.getTokenValue();
                final Location conditionUseLocation = new Location(uri, conditionNameNode);
                return new Method.ConditionUsage(conditionName, conditionUseLocation);
            })
            .forEach(method::addUsedCondition);
    }

    private void handleDefinition(final MagikFile magikFile, final GlobalDefinition definition) {
        final AliasType alias = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(alias);
    }

    private void handleDefinition(final MagikFile magikFile, final BinaryOperatorDefinition definition) {
        final BinaryOperator binaryOperator = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedBinaryOperators.get(path).add(binaryOperator);
    }

    private void handleDefinition(final MagikFile magikFile, final ConditionDefinition definition) {
        final Condition condition = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedConditions.get(path).add(condition);
    }

    private void handleMethodDefinitionOther(final MagikFile magikFile, final MethodDefinition definition) {
        final Method method = this.typeKeeperInserter.feed(definition);
        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);
    }

    /**
     * Read definitions from path.
     * @param path Path to magik file.
     */
    private void readDefinitions(final Path path) {
        this.indexedMethods.put(path, new HashSet<>());
        this.indexedGlobals.put(path, new HashSet<>());
        this.indexedBinaryOperators.put(path, new HashSet<>());
        this.indexedPackages.put(path, new HashSet<>());
        this.indexedTypes.put(path, new HashSet<>());
        this.indexedConditions.put(path, new HashSet<>());

        try {
            final MagikFile magikFile = new MagikFile(path);
            magikFile.getDefinitions()
                .forEach(definition -> this.handleDefinition(magikFile, definition));
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
        }
    }

    /**
     * Scrub definitions.
     * @param path Path to magik file.
     */
    private void scrubDefinitions(final Path path) {
        this.indexedMethods.getOrDefault(path, Collections.emptySet())
            .forEach(method -> method.getOwner().removeMethod(method));
        this.indexedMethods.remove(path);

        this.indexedGlobals.getOrDefault(path, Collections.emptySet())
            .forEach(this.typeKeeper::removeType);
        this.indexedGlobals.remove(path);

        this.indexedBinaryOperators.getOrDefault(path, Collections.emptySet())
            .forEach(this.typeKeeper::removeBinaryOperator);
        this.indexedBinaryOperators.remove(path);

        this.indexedTypes.getOrDefault(path, Collections.emptySet()).stream()
            .filter(type -> type.getLocalMethods().isEmpty())
            .forEach(this.typeKeeper::removeType);
        this.indexedTypes.remove(path);

        this.indexedPackages.getOrDefault(path, Collections.emptySet()).stream()
            .filter(pakkage -> pakkage.getTypes().isEmpty())
            .forEach(this.typeKeeper::removePackage);
        this.indexedPackages.remove(path);

        this.indexedConditions.getOrDefault(path, Collections.emptySet())
            .forEach(this.typeKeeper::removeCondition);
        this.indexedConditions.remove(path);
    }

}
