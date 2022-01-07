package nl.ramsolutions.sw.magik.languageserver.indexer;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.BinaryOperatorDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.EnumerationDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IndexedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MixinDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlottedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeAnnotationHandler;
import nl.ramsolutions.sw.magik.analysis.typing.TypeParser;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.IndexedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.IntrinsicType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.SlottedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.NewDocParser;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikIndexer.class);

    private static final Map<ParameterDefinition.Modifier, Parameter.Modifier> PARAMETER_MODIFIER_MAPPING = Map.of(
        ParameterDefinition.Modifier.NONE, Parameter.Modifier.NONE,
        ParameterDefinition.Modifier.OPTIONAL, Parameter.Modifier.OPTIONAL,
        ParameterDefinition.Modifier.GATHER, Parameter.Modifier.GATHER);
    private static final Map<MethodDefinition.Modifier, Method.Modifier> METHOD_MODIFIER_MAPPING = Map.of(
        MethodDefinition.Modifier.ABSTRACT, Method.Modifier.ABSTRACT,
        MethodDefinition.Modifier.ITER, Method.Modifier.ITER,
        MethodDefinition.Modifier.PRIVATE, Method.Modifier.PRIVATE);

    private final ITypeKeeper typeKeeper;
    private final TypeParser typeParser;
    private final Map<Path, Set<Package>> indexedPackages = new HashMap<>();
    private final Map<Path, Set<AbstractType>> indexedTypes = new HashMap<>();
    private final Map<Path, Set<Method>> indexedMethods = new HashMap<>();
    private final Map<Path, Set<AliasType>> indexedGlobals = new HashMap<>();
    private final Map<Path, Set<BinaryOperator>> indexedBinaryOperators = new HashMap<>();

    public MagikIndexer(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeParser(this.typeKeeper);
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
    public void indexPathCreated(final Path path) {
        LOGGER.debug("Scanning created file: {}", path);

        this.scrubDefinitions(path);
        this.readDefinitions(path);
    }

    /**
     * Index a single magik file when it is changed.
     * @param path Path to magik file.
     */
    public void indexPathChanged(final Path path) {
        LOGGER.debug("Scanning changed file: {}", path);

        this.scrubDefinitions(path);
        this.readDefinitions(path);
    }

    /**
     * Un-index a single magik file when it is deleted.
     * @param path Path to magik file.
     */
    public void indexPathDeleted(final Path path) {
        LOGGER.debug("Scanning deleted file: {}", path);

        this.scrubDefinitions(path);
    }

    private void handleDefinition(final Path path, final MagikFile magikFile, final Definition definition) {
        if (definition instanceof PackageDefinition) {
            final PackageDefinition packageDefinition = (PackageDefinition) definition;
            this.handleDefinition(magikFile, packageDefinition);
        } else if (definition instanceof IndexedExemplarDefinition) {
            final IndexedExemplarDefinition indexedExemplarDefinition = (IndexedExemplarDefinition) definition;
            this.handleDefinition(magikFile, indexedExemplarDefinition);
        } else if (definition instanceof EnumerationDefinition) {
            final EnumerationDefinition enumerationDefinition = (EnumerationDefinition) definition;
            this.handleDefinition(magikFile, enumerationDefinition);
        } else if (definition instanceof SlottedExemplarDefinition) {
            final SlottedExemplarDefinition slottedExemplarDefinition = (SlottedExemplarDefinition) definition;
            this.handleDefinition(magikFile, slottedExemplarDefinition);
        } else if (definition instanceof MixinDefinition) {
            final MixinDefinition mixinDefinition = (MixinDefinition) definition;
            this.handleDefinition(magikFile, mixinDefinition);
        } else if (definition instanceof MethodDefinition) {
            final MethodDefinition methodDefinition = (MethodDefinition) definition;
            this.handleDefinition(magikFile, methodDefinition);
        } else if (definition instanceof GlobalDefinition) {
            final GlobalDefinition globalDefinition = (GlobalDefinition) definition;
            this.handleDefinition(magikFile, globalDefinition);
        } else if (definition instanceof BinaryOperatorDefinition) {
            final BinaryOperatorDefinition binaryOperatorDefinition = (BinaryOperatorDefinition) definition;
            this.handleDefinition(magikFile, binaryOperatorDefinition);
        }
    }

    @SuppressWarnings("checkstyle:NestedIfDepth")
    private void handleDefinition(final MagikFile magikFile, final PackageDefinition definition) {
        final String name = definition.getName();
        if (!this.typeKeeper.hasPackage(name)) {
            // Create new package.
            final Package pakkage = new Package(name);
            final AstNode node = definition.getNode();
            final URI uri = magikFile.getUri();
            final Location location = new Location(uri, node);
            pakkage.setLocation(location);
            this.typeKeeper.addPackage(pakkage);

            // Add uses.
            definition.getUses().stream()
                .forEach(uses -> {
                    final Package usesPakkage = this.typeKeeper.getPackage(uses);
                    if (usesPakkage != null) {
                        pakkage.addUse(usesPakkage);
                    }
                });

            LOGGER.debug("Indexed package: {}", pakkage);
        }

        final Path path = Paths.get(magikFile.getUri());
        final Package pakkage = this.typeKeeper.getPackage(name);
        this.indexedPackages.get(path).add(pakkage);
    }

    private void handleDefinition(final MagikFile magikFile, final IndexedExemplarDefinition definition) {
        final AstNode node = definition.getNode();
        final GlobalReference globalRef = definition.getGlobalReference();
        final MagikType type = this.typeKeeper.getType(globalRef) != UndefinedType.INSTANCE
            ? (MagikType) this.typeKeeper.getType(globalRef)
            : new IndexedType(globalRef);
        this.typeKeeper.addType(type);

        final Map<String, String> slots = Collections.emptyMap();
        final List<String> parents = definition.getParents();
        final MagikType defaultParentType =
            (MagikType) this.typeKeeper.getType(GlobalReference.of("sw:indexed_format_mixin"));
        this.fillType(type, magikFile, node, globalRef.getPakkage(), slots, parents, defaultParentType);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(type);

        LOGGER.debug("Indexed type: {}", type);
    }

    private void handleDefinition(final MagikFile magikFile, final EnumerationDefinition definition) {
        final AstNode node = definition.getNode();
        final GlobalReference globalRef = definition.getGlobalReference();
        final MagikType type = this.typeKeeper.getType(globalRef) != UndefinedType.INSTANCE
            ? (MagikType) this.typeKeeper.getType(globalRef)
            : new SlottedType(globalRef);
        this.typeKeeper.addType(type);

        final Map<String, String> slots = Collections.emptyMap();
        final List<String> parents = definition.getParents();
        final MagikType defaultParentType =
            (MagikType) this.typeKeeper.getType(GlobalReference.of("sw:enumeration_value"));
        this.fillType(type, magikFile, node, globalRef.getPakkage(), slots, parents, defaultParentType);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(type);

        LOGGER.debug("Indexed type: {}", type);
    }

    private void handleDefinition(final MagikFile magikFile, final SlottedExemplarDefinition definition) {
        final AstNode node = definition.getNode();
        final GlobalReference globalRef = definition.getGlobalReference();
        final MagikType type = this.typeKeeper.getType(globalRef) instanceof SlottedType
            ? (MagikType) this.typeKeeper.getType(globalRef)
            : new SlottedType(globalRef);
        this.typeKeeper.addType(type);

        final NewDocParser docParser = new NewDocParser(node);
        final Map<String, String> slotTypes = docParser.getSlotTypes();
        // This needs a default value ("") due to https://bugs.openjdk.java.net/browse/JDK-8148463
        final Map<String, String> slots = definition.getSlots().stream()
            .map(SlottedExemplarDefinition.Slot::getName)
            .collect(Collectors.toMap(
                slotName -> slotName,
                slotName -> slotTypes.getOrDefault(slotName, "")));
        final List<String> parents = definition.getParents();
        final MagikType defaultParentType =
            (MagikType) this.typeKeeper.getType(GlobalReference.of("sw:slotted_format_mixin"));
        this.fillType(type, magikFile, node, globalRef.getPakkage(), slots, parents, defaultParentType);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(type);

        LOGGER.debug("Indexed type: {}", type);
    }

    private void handleDefinition(final MagikFile magikFile, final MixinDefinition definition) {
        final AstNode node = definition.getNode();
        final GlobalReference globalRef = definition.getGlobalReference();
        final MagikType type = this.typeKeeper.getType(globalRef) != UndefinedType.INSTANCE
            ? (MagikType) this.typeKeeper.getType(globalRef)
            : new IntrinsicType(globalRef);
        this.typeKeeper.addType(type);

        final Map<String, String> slots = Collections.emptyMap();
        final List<String> parents = definition.getParents();
        this.fillType(type, magikFile, node, globalRef.getPakkage(), slots, parents, null);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(type);

        LOGGER.debug("Indexed type: {}", type);
    }

    private void handleDefinition(final MagikFile magikFile, final MethodDefinition definition) {
        final AstNode node = definition.getNode();
        if (node.isNot(MagikGrammar.METHOD_DEFINITION)) {
            // No slot accessors, shared variables, shared constants.
            this.handleMethodDefinitionOther(magikFile, definition);
            return;
        }

        // Get exemplar.
        final GlobalReference globalRef = definition.getTypeGlobalReference();
        final AbstractType exemplarType = this.typeKeeper.getType(globalRef);
        if (exemplarType == UndefinedType.INSTANCE) {
            LOGGER.warn("Unknown type: {}", globalRef);
            return;
        }

        // Combine parameter types with method docs.
        final NewDocParser newDocParser = new NewDocParser(node);
        final Map<String, String> parameterTypes = newDocParser.getParameterTypes();
        final List<Parameter> parameters = definition.getParameters().stream()
            .map(parameterDefinition -> {
                final String name = parameterDefinition.getName();
                final AbstractType type;
                if (!parameterTypes.containsKey(name)) {
                    type = UndefinedType.INSTANCE;
                } else {
                    final String parameterType = parameterTypes.get(name);
                    type = this.typeParser.parseTypeString(parameterType, globalRef.getPakkage());
                }

                final Parameter.Modifier modifier =
                    MagikIndexer.PARAMETER_MODIFIER_MAPPING.get(parameterDefinition.getModifier());
                return new Parameter(name, modifier, type);
            })
            .collect(Collectors.toList());
        final ParameterDefinition assignParamDef = definition.getAssignmentParameter();
        final Parameter assignmentParameter = assignParamDef != null
            ? new Parameter(
                assignParamDef.getName(),
                MagikIndexer.PARAMETER_MODIFIER_MAPPING.get(assignParamDef.getModifier()))
            : null;

        // Combine iterator types with method docs.
        final ExpressionResult loopResult = newDocParser.getLoopTypes().stream()
            .map(type -> this.typeParser.parseTypeString(type, globalRef.getPakkage()))
            .collect(ExpressionResult.COLLECTOR);

        // Combine return types with method docs.
        final ExpressionResult callResult = newDocParser.getReturnTypes().stream()
            .map(type -> this.typeParser.parseTypeString(type, globalRef.getPakkage()))
            .collect(ExpressionResult.COLLECTOR);

        // Create method.
        final MagikType magikType = (MagikType) exemplarType;
        final EnumSet<Method.Modifier> modifiers = definition.getModifiers().stream()
            .map(modifier -> MagikIndexer.METHOD_MODIFIER_MAPPING.get(modifier))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        final String methodName = definition.getMethodName();
        final Method method = magikType.addMethod(
            modifiers, location, methodName, parameters, assignmentParameter, callResult, loopResult);

        // Method doc.
        final String methodDoc = MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .map(line -> line.substring(2))  // Strip '##'
            .collect(Collectors.joining("\n"));
        method.setDoc(methodDoc);

        // Save used types.
        final AstNode bodyNode = node.getFirstChild(MagikGrammar.BODY);
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope bodyScope = globalScope.getScopeForNode(bodyNode);
        Objects.requireNonNull(bodyScope);
        bodyScope.getSelfAndDescendantScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(scopeEntry -> scopeEntry.isType(ScopeEntry.Type.GLOBAL)
                                    || scopeEntry.isType(ScopeEntry.Type.DYNAMIC))
            .map(ScopeEntry::getIdentifier)
            .map(identifier -> {
                AbstractType type = this.typeKeeper.getType(globalRef);
                if (type == UndefinedType.INSTANCE) {
                    return null;
                } else if (type == SelfType.INSTANCE) {
                    // TODO: Does this actually happen?
                    type = this.typeKeeper.getType(globalRef);
                }
                return type;
            })
            .filter(Objects::nonNull)
            .forEach(method::addUsedType);

        // Save called method names (thus without type).
        node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            .map(invocationNode -> {
                final MethodInvocationNodeHelper invocationHelper = new MethodInvocationNodeHelper(invocationNode);
                return invocationHelper.getMethodName();
            })
            .forEach(method::addCalledMethod);

        // Save used slot names.
        node.getDescendants(MagikGrammar.SLOT).stream()
            .map(slotNode -> slotNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue())
            .forEach(method::addUsedSlot);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);

        LOGGER.debug("Indexed method: {}", method);
    }

    @SuppressWarnings("java:S1172")
    private void handleDefinition(final MagikFile magikFile, final GlobalDefinition globalDefinition) {
        final String pakkage = globalDefinition.getPackage();
        final String identifier = globalDefinition.getName();
        final GlobalReference globalRef = typeParser.getGlobalRefeference(identifier, pakkage);

        if (this.typeKeeper.getType(globalRef) != null) {
            // Don't overwrite any existing types with a AliasType.
            return;
        }

        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(globalDefinition.getNode());
        final AbstractType aliasedType = typeAnnotation != null
            ? this.typeParser.parseTypeString(typeAnnotation, globalDefinition.getPackage())
            : UndefinedType.INSTANCE;
        final AbstractType globalType = new AliasType(globalRef, aliasedType);
        this.typeKeeper.addType(globalType);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(globalType);
    }

    @SuppressWarnings("java:S1172")
    private void handleDefinition(final MagikFile magikFile, final BinaryOperatorDefinition binaryOperatorDefinition) {
        final BinaryOperator.Operator operator =
            BinaryOperator.Operator.valueFor(binaryOperatorDefinition.getOperator());
        final String pakkage = binaryOperatorDefinition.getPackage();
        final String leftTypeStr = binaryOperatorDefinition.getLhs();
        final AbstractType leftType = this.typeParser.parseTypeString(leftTypeStr, pakkage);
        final String rightTypeStr = binaryOperatorDefinition.getRhs();
        final AbstractType rightType = this.typeParser.parseTypeString(rightTypeStr, pakkage);
        if (leftType == UndefinedType.INSTANCE) {
            LOGGER.warn("Unknown lhs type: {}", leftType);
            return;
        }
        if (rightType == UndefinedType.INSTANCE) {
            LOGGER.warn("Unknown rhs type: {}", rightType);
            return;
        }

        final AbstractType resultType = UndefinedType.INSTANCE;  // TODO: Determine type.
        final BinaryOperator binaryOperator = new BinaryOperator(operator, leftType, rightType, resultType);
        this.typeKeeper.addBinaryOperator(binaryOperator);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedBinaryOperators.get(path).add(binaryOperator);
    }

    private void fillType(
            final MagikType magikType,
            final MagikFile magikFile,
            final AstNode node,
            final String packageName,
            final Map<String, String> slots,
            final List<String> parents,
            final @Nullable AbstractType defaultParentType) {
        // Set location.
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        magikType.setLocation(location);

        // Set slots.
        magikType.clearSlots();
        slots.entrySet()
            .forEach(entry -> {
                final String slotName = entry.getKey();
                final String slotTypeName = entry.getValue();
                final AbstractType slotType = this.typeParser.parseTypeString(slotTypeName, packageName);
                final Slot slot = magikType.addSlot(null, slotName);
                slot.setType(slotType);
            });

        // Parents.
        magikType.clearParents();
        final PackageNodeHelper packageHelper = new PackageNodeHelper(node);
        final String pakkageName = packageHelper.getCurrentPackage();
        parents.stream()
            .forEach(parent -> {
                final GlobalReference parentGlobalRef = parent.indexOf(':') != -1
                    ? GlobalReference.of(parent)
                    : GlobalReference.of(pakkageName, parent);
                final AbstractType parentType = this.typeKeeper.getType(parentGlobalRef);
                if (parentType == UndefinedType.INSTANCE) {
                    LOGGER.warn("Parent type not found: {} from package: {}", parent, pakkageName);
                } else {
                    magikType.addParent(parentType);
                }
            });

        // Default parent types ()
        boolean parentNonIntrinsicType = magikType.getParents().stream()
            .anyMatch(parentType -> parentType instanceof SlottedType
                                    || parentType instanceof IndexedType);
        if (defaultParentType != null
            && !parentNonIntrinsicType) {
            magikType.addParent(defaultParentType);
        }

        // Type doc.
        final String typeDoc = MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .map(line -> line.substring(2))  // Strip '##'
            .collect(Collectors.joining("\n"));
        magikType.setDoc(typeDoc);
    }

    private void handleMethodDefinitionOther(final MagikFile magikFile, final MethodDefinition definition) {
        // Slot accessors, shared variables, shared constants.
        final GlobalReference globalRef = definition.getTypeGlobalReference();
        final AbstractType exemplarType = this.typeKeeper.getType(globalRef);
        if (exemplarType == UndefinedType.INSTANCE) {
            LOGGER.warn("Unknown type: {}", globalRef);
            return;
        }

        // Get method return types from docs, if any.
        final AstNode node = definition.getNode();
        final NewDocParser methodDocParser = new NewDocParser(node);
        final List<AbstractType> methodReturnTypes = methodDocParser.getReturnTypes().stream()
            .map(typeStr -> this.typeParser.parseTypeString(typeStr, globalRef.getPakkage()))
            .collect(Collectors.toList());

        // Get slot type from docs, if any.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
        final AbstractType slotType;
        if (statementNode != null) {
            final NewDocParser exemplarDocParser = new NewDocParser(statementNode);
            final String slotName = definition.getMethodName();
            final String slotTypeStr = exemplarDocParser.getSlotTypes().get(slotName);
            slotType = this.typeParser.parseTypeString(slotTypeStr, globalRef.getPakkage());
        } else {
            slotType = UndefinedType.INSTANCE;
        }

        // Determine the result to use.
        final ExpressionResult result = !methodReturnTypes.isEmpty()
            ? new ExpressionResult(methodReturnTypes)
            : new ExpressionResult(slotType);

        // Create method.
        final MagikType magikType = (MagikType) exemplarType;
        final EnumSet<Method.Modifier> modifiers = definition.getModifiers().stream()
            .map(modifier -> MagikIndexer.METHOD_MODIFIER_MAPPING.get(modifier))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        final String methodName = definition.getMethodName();
        final List<Parameter> parameters = Collections.emptyList();
        final Parameter assignmentParameter = null;
        final Method method = magikType.addMethod(
            modifiers, location, methodName, parameters, assignmentParameter, result);

        // TODO: Will this work?
        // TODO: Ensure we don't pick up class comment of def_slotted_exemplar.
        final String methodDoc = MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .collect(Collectors.joining("\n"));
        method.setDoc(methodDoc);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);

        LOGGER.debug("Indexed method: {}", method);
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

        try {
            final MagikFile magikFile = new MagikFile(path);
            magikFile.getDefinitions()
                .forEach(definition -> this.handleDefinition(path, magikFile, definition));
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
            .forEach(type -> this.typeKeeper.removeType(type));
        this.indexedGlobals.remove(path);

        this.indexedBinaryOperators.getOrDefault(path, Collections.emptySet())
            .forEach(binaryOperator -> this.typeKeeper.removeBinaryOperator(binaryOperator));
        this.indexedBinaryOperators.remove(path);

        this.indexedTypes.getOrDefault(path, Collections.emptySet()).stream()
            .filter(type -> type.getLocalMethods().isEmpty())
            .forEach(type -> this.typeKeeper.removeType(type));
        this.indexedTypes.remove(path);

        this.indexedPackages.getOrDefault(path, Collections.emptySet()).stream()
            .filter(pakkage -> pakkage.getTypes().isEmpty())
            .forEach(pakkage -> this.typeKeeper.removePackage(pakkage));
        this.indexedPackages.remove(path);
    }

}
