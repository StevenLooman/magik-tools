package nl.ramsolutions.sw.magik.analysis.typing.indexer;

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
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.EnumerationDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IndexedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MixinDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlottedExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.BinaryOperator;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.AliasType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Condition;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType;
import nl.ramsolutions.sw.magik.analysis.typing.types.MagikType.Sort;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;
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

    private static final CommentInstructionReader.InstructionType TYPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createInstructionType("type");

    private final ITypeKeeper typeKeeper;
    private final TypeReader typeParser;
    private final Map<Path, Set<Package>> indexedPackages = new HashMap<>();
    private final Map<Path, Set<AbstractType>> indexedTypes = new HashMap<>();
    private final Map<Path, Set<Method>> indexedMethods = new HashMap<>();
    private final Map<Path, Set<AliasType>> indexedGlobals = new HashMap<>();
    private final Map<Path, Set<BinaryOperator>> indexedBinaryOperators = new HashMap<>();
    private final Map<Path, Set<Condition>> indexedConditions = new HashMap<>();

    public MagikIndexer(final ITypeKeeper typeKeeper) {
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeReader(this.typeKeeper);
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
        } else if (definition instanceof ConditionDefinition) {
            final ConditionDefinition conditionDefinition = (ConditionDefinition) definition;
            this.handleDefinition(magikFile, conditionDefinition);
        }
    }

    @SuppressWarnings("checkstyle:NestedIfDepth")
    private void handleDefinition(final MagikFile magikFile, final PackageDefinition definition) {
        final String name = definition.getName();
        final Package pakkage;
        if (!this.typeKeeper.hasPackage(name)) {
            // Create new package.
            pakkage = new Package(this.typeKeeper, name);
        } else {
            pakkage = this.typeKeeper.getPackage(name);
        }
        Objects.requireNonNull(pakkage);

        final AstNode node = definition.getNode();
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        pakkage.setLocation(location);

        // Add uses.
        pakkage.clearUses();
        definition.getUses()
            .forEach(pakkage::addUse);

        LOGGER.debug("Indexed package: {}", pakkage);

        final Path path = Paths.get(uri);
        this.indexedPackages.get(path).add(pakkage);
    }

    private void handleDefinition(final MagikFile magikFile, final IndexedExemplarDefinition definition) {
        this.ensurePackageExists(definition);

        final AstNode node = definition.getNode();
        final TypeString typeString = definition.getTypeString();
        final MagikType magikType = this.findOrCreateType(typeString, MagikType.Sort.INDEXED);

        final Map<String, TypeString> slots = Collections.emptyMap();
        final List<TypeString> parents = definition.getParents();
        final TypeString defaultParentRef = TypeString.ofIdentifier("indexed_format_mixin", "sw");
        this.fillType(magikType, magikFile, node, slots, parents, defaultParentRef);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(magikType);

        LOGGER.debug("Indexed type: {}", magikType);  // NOSONAR
    }

    private void handleDefinition(final MagikFile magikFile, final EnumerationDefinition definition) {
        this.ensurePackageExists(definition);

        final AstNode node = definition.getNode();
        final TypeString typeString = definition.getTypeString();
        final MagikType magikType = this.findOrCreateType(typeString, MagikType.Sort.SLOTTED);

        final Map<String, TypeString> slots = Collections.emptyMap();
        final List<TypeString> parents = definition.getParents();
        final TypeString defaultParentRef = TypeString.ofIdentifier("enumeration_value", "sw");
        this.fillType(magikType, magikFile, node, slots, parents, defaultParentRef);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(magikType);

        LOGGER.debug("Indexed type: {}", magikType);  // NOSONAR
    }

    private void handleDefinition(final MagikFile magikFile, final SlottedExemplarDefinition definition) {
        this.ensurePackageExists(definition);

        final AstNode node = definition.getNode();
        final TypeString typeString = definition.getTypeString();
        final MagikType magikType = this.findOrCreateType(typeString, MagikType.Sort.SLOTTED);

        // Slots.
        final TypeDocParser docParser = new TypeDocParser(node);
        final Map<String, TypeString> slotTypes = docParser.getSlotTypes();
        // This needs a default value ("") due to https://bugs.openjdk.java.net/browse/JDK-8148463
        final Map<String, TypeString> slots = definition.getSlots().stream()
            .map(SlottedExemplarDefinition.Slot::getName)
            .collect(Collectors.toMap(
                slotName -> slotName,
                slotName -> slotTypes.getOrDefault(slotName, TypeString.UNDEFINED)));
        final List<TypeString> parents = definition.getParents();
        final TypeString defaultParentRef = TypeString.ofIdentifier("slotted_format_mixin", "sw");
        this.fillType(magikType, magikFile, node, slots, parents, defaultParentRef);

        // Generics.
        docParser.getGenericTypeNodes().entrySet()
            .forEach(entry -> {
                final AstNode genericNode = entry.getKey();
                final URI uri = magikFile.getUri();
                final Location location = new Location(uri, genericNode);
                final TypeString genericTypeStr = entry.getValue();
                final String name = genericTypeStr.getIdentifier();
                magikType.addGeneric(location, name);
            });


        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(magikType);

        LOGGER.debug("Indexed type: {}", magikType);  // NOSONAR
    }

    private void handleDefinition(final MagikFile magikFile, final MixinDefinition definition) {
        this.ensurePackageExists(definition);

        final AstNode node = definition.getNode();
        final TypeString typeString = definition.getTypeString();
        final MagikType magikType = this.findOrCreateType(typeString, MagikType.Sort.INTRINSIC);

        final Map<String, TypeString> slots = Collections.emptyMap();
        final List<TypeString> parents = definition.getParents();
        this.fillType(magikType, magikFile, node, slots, parents, null);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(magikType);

        LOGGER.debug("Indexed type: {}", magikType);  // NOSONAR
    }

    private void handleDefinition(final MagikFile magikFile, final MethodDefinition definition) {
        this.ensurePackageExists(definition);

        if (!definition.isActualMethodDefinition()) {
            // No slot accessors, shared variables, shared constants.
            this.handleMethodDefinitionOther(magikFile, definition);
            return;
        }

        // Get exemplar.
        final TypeString typeString = definition.getExemplarName();
        final AbstractType exemplarType = this.findOrCreateType(typeString, MagikType.Sort.UNDEFINED);

        // Combine parameter types with method docs.
        final AstNode node = definition.getNode();
        final TypeDocParser typeDocParser = new TypeDocParser(node);
        final Map<String, TypeString> parameterTypes = typeDocParser.getParameterTypes();
        final List<Parameter> parameters = definition.getParameters().stream()
            .map(parameterDefinition -> {
                final String name = parameterDefinition.getName();
                final AbstractType type;
                if (!parameterTypes.containsKey(name)) {
                    type = UndefinedType.INSTANCE;
                } else {
                    final TypeString parameterType = parameterTypes.get(name);
                    type = this.typeParser.parseTypeString(parameterType);
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

        // Get return types from method docs.
        final List<TypeString> callResultDocs = typeDocParser.getReturnTypes();
        // Ensure we can believe the docs, sort of.
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        final boolean returnsAnything = helper.returnsAnything();
        final ExpressionResultString callResult =
            !callResultDocs.isEmpty()
            || callResultDocs.isEmpty() && !returnsAnything
            ? new ExpressionResultString(callResultDocs)
            : ExpressionResultString.UNDEFINED;

        // Get iterator types from method docs.
        final List<TypeString> loopResultDocs = typeDocParser.getLoopTypes();
        // Ensure method docs match actual loopbody, sort of.
        final boolean hasLoopbody = helper.hasLoopbody();
        final ExpressionResultString loopResult =
            !loopResultDocs.isEmpty()
            || loopResultDocs.isEmpty() && !hasLoopbody
            ? new ExpressionResultString(loopResultDocs)
            : ExpressionResultString.UNDEFINED;

        // Method doc.
        final String methodDoc = MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .map(line -> line.substring(2))  // Strip '##'
            .collect(Collectors.joining("\n"));

        // Create method.
        final MagikType magikType = (MagikType) exemplarType;
        final EnumSet<Method.Modifier> modifiers = definition.getModifiers().stream()
            .map(MagikIndexer.METHOD_MODIFIER_MAPPING::get)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        final String methodName = definition.getMethodName();
        final Method method = magikType.addMethod(
            location, modifiers, methodName, parameters, assignmentParameter, methodDoc, callResult, loopResult);

        // Save used types.
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
                final AstNode usageNode = scopeEntry.getNode();
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

        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);

        LOGGER.debug("Indexed method: {}", method);
    }

    @SuppressWarnings("java:S1172")
    private void handleDefinition(final MagikFile magikFile, final GlobalDefinition globalDefinition) {
        this.ensurePackageExists(globalDefinition);

        final String pakkage = globalDefinition.getPackage();
        final String identifier = globalDefinition.getName();
        final TypeString typeStr = TypeString.ofIdentifier(identifier, pakkage);

        if (this.typeKeeper.getType(typeStr) != null) {
            // Don't overwrite any existing types with a AliasType.
            return;
        }

        final AstNode node = globalDefinition.getNode();
        final CommentInstructionReader instructionReader =
            new CommentInstructionReader(magikFile, Set.of(TYPE_INSTRUCTION));
        final String typeAnnotation = instructionReader.getInstructionForNode(node, TYPE_INSTRUCTION);
        final TypeString aliasedRef = typeAnnotation != null
            ? TypeStringParser.parseTypeString(typeAnnotation, globalDefinition.getPackage())
            : TypeString.UNDEFINED;
        final AbstractType globalType = new AliasType(this.typeKeeper, typeStr, aliasedRef);
        this.typeKeeper.addType(globalType);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedTypes.get(path).add(globalType);
    }

    @SuppressWarnings("java:S1172")
    private void handleDefinition(final MagikFile magikFile, final BinaryOperatorDefinition binaryOperatorDefinition) {
        this.ensurePackageExists(binaryOperatorDefinition);

        final BinaryOperator.Operator operator =
            BinaryOperator.Operator.valueFor(binaryOperatorDefinition.getOperator());
        final TypeString lhsRef = binaryOperatorDefinition.getLhs();
        final TypeString rhsRef = binaryOperatorDefinition.getRhs();
        final TypeString resultRef = TypeString.UNDEFINED;  // TODO: Determine type.
        final BinaryOperator binaryOperator = new BinaryOperator(operator, lhsRef, rhsRef, resultRef);
        this.typeKeeper.addBinaryOperator(binaryOperator);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedBinaryOperators.get(path).add(binaryOperator);
    }

    private void handleDefinition(final MagikFile magikFile, final ConditionDefinition definition) {
        final AstNode node = definition.getNode();
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        final String name = definition.getName();
        final String parent = definition.getParent();
        final List<String> dataNameList = definition.getDataNames();
        final String doc = null;
        final Condition condition = new Condition(location, name, parent, dataNameList, doc);
        this.typeKeeper.addCondition(condition);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedConditions.get(path).add(condition);
    }

    private void fillType(
            final MagikType magikType,
            final MagikFile magikFile,
            final AstNode node,
            final Map<String, TypeString> slots,
            final List<TypeString> parents,
            final @Nullable TypeString defaultParentRef) {
        // Set location.
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        magikType.setLocation(location);

        // Set slots.
        magikType.clearSlots();
        slots.entrySet()
            .forEach(entry -> {
                final String slotName = entry.getKey();
                final TypeString slotTypeString = entry.getValue();
                magikType.addSlot(null, slotName, slotTypeString);
            });

        // Parents.
        magikType.clearParents();
        parents.forEach(magikType::addParent);

        // Default parent types.
        boolean parentNonIntrinsicType = magikType.getParents().stream()
            .filter(MagikType.class::isInstance)
            .map(MagikType.class::cast)
            .anyMatch(parentType -> parentType.getSort() == Sort.SLOTTED
                                    || parentType.getSort() == Sort.INDEXED);
        if (defaultParentRef != null
            && !parentNonIntrinsicType) {
            magikType.addParent(defaultParentRef);
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
        final TypeString typeString = definition.getExemplarName();
        final AbstractType exemplarType = this.findOrCreateType(typeString, MagikType.Sort.UNDEFINED);

        // Get method return types from docs, if any.
        final AstNode node = definition.getNode();
        final TypeDocParser methodDocParser = new TypeDocParser(node);
        final List<TypeString> methodReturnTypes = methodDocParser.getReturnTypes();

        // Get slot type from docs, if any.
        final AstNode statementNode = node.getFirstAncestor(MagikGrammar.STATEMENT);
        final TypeString slotType;
        if (statementNode != null) {
            final TypeDocParser exemplarDocParser = new TypeDocParser(statementNode);
            final String slotName = definition.getMethodName();
            final Map<String, TypeString> slotTypes = exemplarDocParser.getSlotTypes();
            slotType = slotTypes.getOrDefault(slotName, TypeString.UNDEFINED);
        } else {
            slotType = TypeString.UNDEFINED;
        }

        // Determine the result to use.
        final ExpressionResultString result = !methodReturnTypes.isEmpty()
            ? new ExpressionResultString(methodReturnTypes)
            : new ExpressionResultString(slotType);

        // TODO: Will this work?
        // TODO: Ensure we don't pick up class comment of def_slotted_exemplar.
        final String methodDoc = MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .collect(Collectors.joining("\n"));

        // Create method.
        final MagikType magikType = (MagikType) exemplarType;
        final EnumSet<Method.Modifier> modifiers = definition.getModifiers().stream()
            .map(MagikIndexer.METHOD_MODIFIER_MAPPING::get)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Method.Modifier.class)));
        final URI uri = magikFile.getUri();
        final Location location = new Location(uri, node);
        final String methodName = definition.getMethodName();
        final List<Parameter> parameters = Collections.emptyList();
        final Parameter assignmentParameter = null;
        final ExpressionResultString loopbodyResult = new ExpressionResultString();
        final Method method = magikType.addMethod(
            location, modifiers, methodName, parameters, assignmentParameter, methodDoc, result, loopbodyResult);

        final Path path = Paths.get(magikFile.getUri());
        this.indexedMethods.get(path).add(method);

        LOGGER.debug("Indexed method: {}", method);
    }

    private void ensurePackageExists(final Definition definition) {
        final String pakkageName = definition.getPackage();
        if (!this.typeKeeper.hasPackage(pakkageName)) {
            new Package(this.typeKeeper, pakkageName);
        }
    }

    private MagikType findOrCreateType(final TypeString typeString, final MagikType.Sort sort) {
        final AbstractType type = this.typeKeeper.getType(typeString);
        if (type == UndefinedType.INSTANCE) {
            return new MagikType(this.typeKeeper, sort, typeString);
        } else if (type instanceof MagikType) {
            final MagikType magikType = (MagikType) type;
            if (sort != MagikType.Sort.UNDEFINED  // If target sort is UNDEFINED, we don't care.
                && magikType.getSort() != MagikType.Sort.UNDEFINED  // Otherwise ensure it isn't overwritten.
                && magikType.getSort() != sort) {
                throw new IllegalStateException();
            }

            magikType.setSort(sort);
            return magikType;
        }

        throw new IllegalStateException();
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
