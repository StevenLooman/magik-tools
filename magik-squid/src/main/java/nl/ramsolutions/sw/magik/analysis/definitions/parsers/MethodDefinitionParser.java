package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.ConditionUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.GlobalUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodUsage;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.SlotUsage;
import nl.ramsolutions.sw.magik.analysis.helpers.ArgumentsNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeBuilderVisitor;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

/**
 * Method definition parser.
 */
public class MethodDefinitionParser {

    private static final String CONDITION = "condition";
    private static final String SW_CONDITION = "sw:condition";
    private static final String NEW_CALL = "new()";
    private static final String RAISE_CALL = "raise()";

    private final MagikAnalysisConfiguration configuration;
    private final AstNode node;

    /**
     * Constructor.
     * @param node Method definition node.
     */
    public MethodDefinitionParser(final MagikAnalysisConfiguration configuration, final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_DEFINITION)) {
            throw new IllegalArgumentException();
        }

        this.configuration = configuration;
        this.node = node;
    }

    /**
     * Parse defitions.
     * @return List of parsed definitions.
     */
    public List<Definition> parseDefinitions() {
        // Don't burn ourselves on syntax errors.
        final AstNode syntaxErrorNode = this.node.getFirstChild(MagikGrammar.SYNTAX_ERROR);
        if (syntaxErrorNode != null) {
            return Collections.emptyList();
        }

        // Figure location.
        final URI uri = this.node.getToken().getURI();
        final Location location = new Location(uri, this.node);

        // Figure module name.
        final String moduleName = ModuleDefinitionScanner.getModuleName(uri);

        // Figure exemplar name & method name.
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(this.node);
        final TypeString exemplarName = helper.getTypeString();
        final String methodName = helper.getMethodName();

        // Figure modifers.
        final Set<MethodDefinition.Modifier> modifiers = new HashSet<>();
        if (helper.isPrivateMethod()) {
            modifiers.add(MethodDefinition.Modifier.PRIVATE);
        }
        if (helper.isIterMethod()) {
            modifiers.add(MethodDefinition.Modifier.ITER);
        }
        if (helper.isAbstractMethod()) {
            modifiers.add(MethodDefinition.Modifier.ABSTRACT);
        }

        // Figure parameters.
        final TypeDocParser typeDocParser = new TypeDocParser(this.node);
        final Map<String, TypeString> parameterTypes = typeDocParser.getParameterTypes();
        final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
        final List<ParameterDefinition> parameters =
            this.createParameterDefinitions(moduleName, parametersNode, parameterTypes);
        final AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
        final ParameterDefinition assignmentParamter =
            this.createAssignmentParameterDefinition(moduleName, assignmentParameterNode, parameterTypes);

        // Get return types from method docs.
        final List<TypeString> callResultDocs = typeDocParser.getReturnTypes();
        // Ensure we can believe the docs, sort of.
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
        final String doc = MagikCommentExtractor.extractDocCommentTokens(node)
            .map(Token::getValue)
            .map(line -> line.substring(2))  // Strip '##'
            .map(String::trim)
            .collect(Collectors.joining("\n"));

        final Set<GlobalUsage> usedGlobals = this.configuration.getMagikIndexerIndexUsages()
            ? this.getUsedGlobals()
            : Collections.emptySet();
        final Set<MethodUsage> usedMethods = this.configuration.getMagikIndexerIndexUsages()
            ? this.getUsedMethods()
            : Collections.emptySet();
        final Set<SlotUsage> usedSlots = this.configuration.getMagikIndexerIndexUsages()
            ? this.getUsedSlots()
            : Collections.emptySet();
        final Set<ConditionUsage> usedConditions = this.configuration.getMagikIndexerIndexUsages()
            ? this.getUsedConditions()
            : Collections.emptySet();

        final MethodDefinition methodDefinition = new MethodDefinition(
            location,
            moduleName,
            doc,
            this.node,
            exemplarName,
            methodName,
            modifiers,
            parameters,
            assignmentParamter,
            callResult,
            loopResult,
            usedGlobals,
            usedMethods,
            usedSlots,
            usedConditions);
        return List.of(methodDefinition);
    }

    private Set<GlobalUsage> getUsedGlobals() {
        final ScopeBuilderVisitor scopeBuilderVisitor = new ScopeBuilderVisitor();
        scopeBuilderVisitor.createGlobalScope(this.node);
        scopeBuilderVisitor.walkAst(this.node);
        final GlobalScope globalScope = scopeBuilderVisitor.getGlobalScope();
        final AstNode bodyNode = this.node.getFirstChild(MagikGrammar.BODY);
        final Scope bodyScope = globalScope.getScopeForNode(bodyNode);
        Objects.requireNonNull(bodyScope);

        final PackageNodeHelper packageNodeHelper = new PackageNodeHelper(node);
        final String currentPakkage = packageNodeHelper.getCurrentPackage();
        return bodyScope.getSelfAndDescendantScopes().stream()
            .flatMap(scope -> scope.getScopeEntriesInScope().stream())
            .filter(scopeEntry -> scopeEntry.isType(ScopeEntry.Type.GLOBAL, ScopeEntry.Type.DYNAMIC))
            .map(scopeEntry -> {
                final String identifier = scopeEntry.getIdentifier();
                final TypeString ref = TypeString.ofIdentifier(identifier, currentPakkage);
                final URI uri = this.node.getToken().getURI();
                final Location location = new Location(uri, scopeEntry.getDefinitionNode());
                final Location validLocation = Location.validLocation(location);
                // TODO: The type should be resolved here, but we don't have a type resolver yet.
                //       Now you might "see" the ref user:char16_vector, or any other package which is a child of `sw`.
                //       This will most likely be indexed invalidly.
                //       Though, we might be able to resolve it during the query itself.
                return new GlobalUsage(ref, validLocation);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<MethodUsage> getUsedMethods() {
        // TODO: To be implemented.
        return Collections.emptySet();
    }

    private Set<SlotUsage> getUsedSlots() {
        return this.node.getDescendants(MagikGrammar.SLOT).stream()
            .map(slotNode -> {
                final String slotName = slotNode.getFirstChild(MagikGrammar.IDENTIFIER).getTokenValue();
                final URI uri = this.node.getToken().getURI();
                final Location location = new Location(uri, slotNode);
                final Location validLocation = Location.validLocation(location);
                return new SlotUsage(slotName, validLocation);
            })
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<ConditionUsage> getUsedConditions() {
        final URI uri = this.node.getToken().getURI();
        final Stream<ConditionUsage> handledConditions = this.node.getDescendants(MagikGrammar.CONDITION_NAME).stream()
            .map(conditionNameNode -> {
                final String conditionName = conditionNameNode.getTokenValue();
                final Location location = new Location(uri, conditionNameNode);
                final Location validLocation = Location.validLocation(location);
                return new ConditionUsage(conditionName, validLocation);
            });
        final Stream<ConditionUsage> raisedConditions =
            this.node.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            .map(invocationNode -> {
                final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(invocationNode);
                final AstNode receiverNode = helper.getReceiverNode();
                if (receiverNode == null
                    || !receiverNode.is(MagikGrammar.ATOM)) {
                    return null;
                }

                final String identifier = receiverNode.getTokenValue();
                if (!identifier.equalsIgnoreCase(CONDITION)
                    && !identifier.equalsIgnoreCase(SW_CONDITION)) {
                    return null;
                }

                if (!helper.isMethodInvocationOf(NEW_CALL)
                    && !helper.isMethodInvocationOf(RAISE_CALL)) {
                    return null;
                }

                final AstNode argumentsNode = invocationNode.getFirstChild(MagikGrammar.ARGUMENTS);
                if (argumentsNode == null) {
                    return null;
                }

                final ArgumentsNodeHelper argumentsHelper = new ArgumentsNodeHelper(argumentsNode);
                final AstNode argumentNode = argumentsHelper.getArgument(0, MagikGrammar.SYMBOL);
                if (argumentNode == null) {
                    return null;
                }

                final String conditionName = argumentNode.getTokenValue().substring(1);
                final Location location = new Location(uri, argumentsNode);
                final Location validLocation = Location.validLocation(location);
                return new ConditionUsage(conditionName, validLocation);
            })
            .filter(Objects::nonNull);
        return Stream.concat(handledConditions, raisedConditions)
            .collect(Collectors.toUnmodifiableSet());
    }

    private List<ParameterDefinition> createParameterDefinitions(
            final @Nullable String moduleName,
            final @Nullable AstNode parametersNode,
            final Map<String, TypeString> parameterTypes) {
        if (parametersNode == null) {
            return Collections.emptyList();
        }

        final URI uri = this.node.getToken().getURI();
        final List<ParameterDefinition> parameterDefinitions = new ArrayList<>();
        for (final AstNode parameterNode : parametersNode.getChildren(MagikGrammar.PARAMETER)) {
            final Location location = new Location(uri, parameterNode);

            final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String identifier = identifierNode.getTokenValue();

            final ParameterNodeHelper helper = new ParameterNodeHelper(parameterNode);
            final ParameterDefinition.Modifier modifier;
            if (helper.isOptionalParameter()) {
                modifier = ParameterDefinition.Modifier.OPTIONAL;
            } else if (helper.isGatherParameter()) {
                modifier = ParameterDefinition.Modifier.GATHER;
            } else {
                modifier = ParameterDefinition.Modifier.NONE;
            }

            final TypeString typeRef = parameterTypes.getOrDefault(identifier, TypeString.UNDEFINED);

            final ParameterDefinition parameterDefinition = new ParameterDefinition(
                location,
                moduleName,
                null,
                parameterNode,
                identifier,
                modifier,
                typeRef);
            parameterDefinitions.add(parameterDefinition);
        }

        return parameterDefinitions;
    }

    @CheckForNull
    private ParameterDefinition createAssignmentParameterDefinition(
            final @Nullable String moduleName,
            final @Nullable AstNode assignmentParameterNode,
            final Map<String, TypeString> parameterTypes) {
        if (assignmentParameterNode == null) {
            return null;
        }

        final AstNode parameterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
        final URI uri = this.node.getToken().getURI();
        final Location location = new Location(uri, parameterNode);
        final String identifier = parameterNode.getTokenValue();
        final TypeString typeRef = parameterTypes.getOrDefault(identifier, TypeString.UNDEFINED);
        return new ParameterDefinition(
            location,
            moduleName,
            null,
            parameterNode,
            identifier,
            ParameterDefinition.Modifier.NONE,
            typeRef);
    }

}
