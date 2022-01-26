package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.AstWalker;
import nl.ramsolutions.sw.magik.analysis.Location;
import nl.ramsolutions.sw.magik.analysis.helpers.LeaveStatementNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.CombinedType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.GlobalReference;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Package;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.ProcedureInstance;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.Slot;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.parser.NewDocParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reason over types in Magik code.
 *
 * <p>
 * To determine result types from methods/procedures,
 * all expressions are evaluated to determine types of variables etc.
 * This information is then used to determine the result type(s) of
 * methods and procedures.
 * In turn, this information is stored and used for other methods/procedures.
 * </p>
 *
 * <p>
 * If a type cannot be determined, the {{UndefinedType}} is used instead.
 * </p>
 *
 * <p>
 * If {{_self}} or {{_clone}} is returned in a method, the {{SelfType}} is used.
 * It is up to the user to determine the real type. I.e., in case of {{sw:date_time_mixin}},
 * none of the methods are called on that class directly, but always through an inherited class.
 * On declaration the inheriting classes are unknown, thus if {{_self}} is returned from a mixin,
 * we need to proxy the type.
 * </p>
 */
public class LocalTypeReasoner extends AstWalker {

    private static final GlobalReference SW_UNSET = GlobalReference.of("sw:unset");
    private static final GlobalReference SW_SIMPLE_VECTOR = GlobalReference.of("sw:simple_vector");
    private static final GlobalReference SW_FALSE = GlobalReference.of("sw:false");
    private static final GlobalReference SW_MAYBE = GlobalReference.of("sw:maybe");
    private static final GlobalReference SW_HEAVY_THREAD = GlobalReference.of("sw:heavy_thread");
    private static final GlobalReference SW_LIGHT_THREAD = GlobalReference.of("sw:light_thread");
    private static final GlobalReference SW_GLOBAL_VARIABLE = GlobalReference.of("sw:global_variable");
    private static final GlobalReference SW_BIGNUM = GlobalReference.of("sw:bignum");
    private static final GlobalReference SW_INTEGER = GlobalReference.of("sw:integer");
    private static final GlobalReference SW_FLOAT = GlobalReference.of("sw:float");
    private static final GlobalReference SW_CHARACTER = GlobalReference.of("sw:character");
    private static final GlobalReference SW_SW_REGEXP = GlobalReference.of("sw:sw_regexp");
    private static final GlobalReference SW_CHAR16_VECTOR = GlobalReference.of("sw:char16_vector");
    private static final GlobalReference SW_SYMBOL = GlobalReference.of("sw:symbol");
    private static final GlobalReference SW_CONDITION = GlobalReference.of("sw:condition");
    private static final GlobalReference SW_PROCEDURE = GlobalReference.of("sw:procedure");

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasoner.class);
    @SuppressWarnings("checkstyle:MagicNumber")
    private static final long BIGNUM_START = 1 << 29;

    private final ITypeKeeper typeKeeper;
    private final TypeParser typeParser;
    private final GlobalScope globalScope;
    private final Map<AstNode, ExpressionResult> nodeTypes = new HashMap<>();
    private final Map<AstNode, ExpressionResult> loopbodyNodeTypes = new HashMap<>();
    private final Map<ScopeEntry, AstNode> currentScopeEntryNodes = new HashMap<>();
    private Package currentPackage;
    private ExpressionResult iteratorType;

    /**
     * Constructor.
     * @param magikFile Magik file to reason on.
     */
    public LocalTypeReasoner(final MagikTypedFile magikFile) {
        this(magikFile.getTypeKeeper(), magikFile.getGlobalScope());
    }

    /**
     * Constructor.
     * @param typeKeeper TypeKeeper to use.
     * @param globalScope Scope to use for analysis.
     */
    public LocalTypeReasoner(final ITypeKeeper typeKeeper, final GlobalScope globalScope) {
        this.typeKeeper = typeKeeper;
        this.typeParser = new TypeParser(this.typeKeeper);
        this.globalScope = globalScope;
        this.currentPackage = this.typeKeeper.getPackage("sw");
    }

    /**
     * Evaluate the given top {{AstNode}}.
     */
    public void run(final AstNode node) {
        // Start walking.
        LOGGER.debug("Start walking");
        this.walkAst(node);
    }

    /**
     * Get the type for a {{AstNode}}.
     * @param node AstNode.
     * @return Resulting type.
     */
    public ExpressionResult getNodeType(final AstNode node) {
        final ExpressionResult result = this.nodeTypes.get(node);
        if (result == null) {
            LOGGER.debug("Node without type: {}", node);
            return ExpressionResult.UNDEFINED;
        }
        return result;
    }

    /**
     * Get the type for a {{AstNode}}.
     * @param node AstNode.
     * @return Resulting type.
     */
    @CheckForNull
    public ExpressionResult getNodeTypeSilent(final AstNode node) {
        return this.nodeTypes.get(node);
    }

    /**
     * Test if the type for a {{AstNode}} is known.
     * @param node AstNode.
     * @return True if known, false otherwise.
     */
    private boolean hasNodeType(final AstNode node) {
        return this.nodeTypes.containsKey(node);
    }

    /**
     * Set a type for a {{AstNode}}.
     * @param node AstNode.
     * @param result ExpressionResult.
     */
    private void setNodeType(final AstNode node, final ExpressionResult result) {
        LOGGER.trace("{} is of type: {}", node, result);
        this.nodeTypes.put(node, result);
    }

    /**
     * Set a type for the iterator.
     * @param result ExpressionResult.
     */
    private void setIteratorType(final @Nullable ExpressionResult result) {
        this.iteratorType = result;
    }

    @CheckForNull
    private ExpressionResult getIteratorType() {
        // XXX TODO: Return undefined instead of null.
        return this.iteratorType;
    }

    /**
     * Add a type for a {{AstNode}}. Combines type if a type is already known.
     * @param node AstNode.
     * @param result ExpressionResult.
     */
    private void addNodeType(final AstNode node, final ExpressionResult result) {
        if (this.hasNodeType(node)) {
            // Combine types.
            final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
            final ExpressionResult existingResult = this.getNodeType(node);
            final ExpressionResult combinedResult = new ExpressionResult(existingResult, result, unsetType);
            this.setNodeType(node, combinedResult);
        } else {
            this.setNodeType(node, result);
        }
    }

    /**
     * Get the loopbody type for a {{AstNode}}.
     * @param node AstNode.
     * @return Resulting type.
     */
    private ExpressionResult getLoopbodyNodeType(final AstNode node) {
        final ExpressionResult result = this.loopbodyNodeTypes.get(node);
        if (result == null) {
            LOGGER.debug("Node without type: {}", node);
            return ExpressionResult.UNDEFINED;
        }
        return result;
    }

    /**
     * Set a loopbody type for a {{AstNode}}.
     * @param node AstNode.
     * @param result Type.
     */
    private void setLoopbodyNodeType(final AstNode node, final ExpressionResult result) {
        this.loopbodyNodeTypes.put(node, result);
    }

    @Override
    protected void walkPostPackageSpecification(final AstNode node) {
        final PackageNodeHelper helper = new PackageNodeHelper(node);
        final String name = helper.getCurrentPackage();

        if (!this.typeKeeper.hasPackage(name)) {
            LOGGER.debug("Package not found: {}", name);
        }

        // Package is created on demand.
        this.currentPackage = this.typeKeeper.getPackage(name);
    }

    @Override
    protected void walkPostBody(final AstNode node) {
        // Get result from upper EXPRESSION.
        final AstNode expressionNode = node.getFirstAncestor(MagikGrammar.EXPRESSION);
        if (expressionNode == null) {
            // Happens with a return, don't do anything.
            return;
        }
        ExpressionResult result = this.getNodeType(expressionNode);

        // BODYs don't always have to result in something.
        // Find STATEMENT -> RETURN/EMIT/LEAVE
        // TODO: what about _loop _if .. _then _leave _with _endif _leave _with _endloop
        AstNode resultingNode = AstQuery.getFirstChildFromChain(
            node, MagikGrammar.STATEMENT, MagikGrammar.RETURN_STATEMENT);
        if (resultingNode == null) {
            resultingNode = AstQuery.getFirstChildFromChain(node, MagikGrammar.STATEMENT, MagikGrammar.EMIT_STATEMENT);
        }
        if (resultingNode == null) {
            resultingNode = AstQuery.getFirstChildFromChain(node, MagikGrammar.STATEMENT, MagikGrammar.LEAVE_STATEMENT);
        }
        if (resultingNode == null) {
            // Result can also be an unset, as no resulting statement was found.
            // TODO: but... "_block _block _return 1 _endblock _endblock"
            final ExpressionResult emptyResult = new ExpressionResult();
            final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
            result = new ExpressionResult(result, emptyResult, unsetType);
        }

        // Set parent EXPRESSION result.
        this.setNodeType(expressionNode, result);
    }

    @Override
    protected void walkPostIterableExpression(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Bind to identifiers, if any.
        final AstNode overNode = node.getParent();
        final AstNode forNode = overNode.getParent();
        if (forNode.is(MagikGrammar.FOR)) {
            final AstNode loopNode = overNode.getFirstChild(MagikGrammar.LOOP);
            final AstNode bodyNode = loopNode.getFirstChild(MagikGrammar.BODY);
            final List<AstNode> identifierNodes = AstQuery.getChildrenFromChain(
                forNode,
                MagikGrammar.FOR_VARIABLES,
                MagikGrammar.IDENTIFIERS_WITH_GATHER,
                MagikGrammar.IDENTIFIER);
            for (int i = 0; i < identifierNodes.size(); ++i) {
                final AstNode identifierNode = identifierNodes.get(i);
                final AstNode identifierPreviousNode = identifierNode.getPreviousSibling();
                final AbstractType type = identifierPreviousNode != null
                    && identifierPreviousNode.getTokenValue().equalsIgnoreCase("_gather")
                    ? this.typeKeeper.getType(SW_SIMPLE_VECTOR)
                    : this.getIteratorType().get(i, unsetType);

                final ExpressionResult result = new ExpressionResult(type);
                this.setNodeType(identifierNode, result);

                final Scope scope = this.globalScope.getScopeForNode(bodyNode);
                Objects.requireNonNull(scope);
                final String identifier = identifierNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                this.currentScopeEntryNodes.put(scopeEntry, identifierNode);
            }
        }
    }

    @Override
    protected void walkPostParameter(final AstNode node) {
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode.getTokenValue();

        // Parse method/proc docs and extract parameter type.
        final AstNode definitionNode =
            node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);
        final NewDocParser docParser = new NewDocParser(definitionNode);
        final Map<String, String> parameterTypes = docParser.getParameterTypes();
        final String parameterTypeStr = parameterTypes.get(identifier);

        final ExpressionResult result;
        final ParameterNodeHelper helper = new ParameterNodeHelper(node);
        if (helper.isGatherParameter()) {
            final AbstractType simpleVectorType = this.typeKeeper.getType(SW_SIMPLE_VECTOR);
            result = new ExpressionResult(simpleVectorType);
        } else if (parameterTypeStr != null && !parameterTypeStr.isBlank()) {
            final PackageNodeHelper packageHelper = new PackageNodeHelper(node);
            final String pakkage = packageHelper.getCurrentPackage();
            final AbstractType type = this.typeParser.parseTypeString(parameterTypeStr, pakkage);

            if (helper.isOptionalParameter()) {
                final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
                final AbstractType optionalType = new CombinedType(type, unsetType);
                result = new ExpressionResult(optionalType);
            } else {
                result = new ExpressionResult(type);
            }
        } else {
            result = ExpressionResult.UNDEFINED;
        }

        this.setNodeType(node, result);

        final Scope scope = this.globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        this.currentScopeEntryNodes.put(scopeEntry, node);
    }

    @Override
    protected void walkPostAssignmentParameter(final AstNode node) {
        final ExpressionResult result = ExpressionResult.UNDEFINED;
        this.setNodeType(node, result);
    }

    // region: Atoms
    @Override
    protected void walkPostSlot(final AstNode node) {
        // Get class type.
        final AbstractType type = this.getMethodOwnerType(node);
        if (type == UndefinedType.INSTANCE) {
            LOGGER.debug("Unknown type for node: {}", node);
            return;
        }

        // Get slot type.
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String slotName = identifierNode.getTokenValue();
        final Slot slot = type.getSlot(slotName);
        final AbstractType slotType = slot != null
            ? slot.getType()
            : UndefinedType.INSTANCE;
        Objects.requireNonNull(slotType);

        this.assignAtom(node, slotType);
    }

    @Override
    protected void walkPostIdentifier(final AstNode node) {
        final AstNode parent = node.getParent();
        if (!parent.is(MagikGrammar.ATOM)) {
            return;
        }

        final Scope scope = this.globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        String identifier = node.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        Objects.requireNonNull(scopeEntry);
        if (scopeEntry.isType(ScopeEntry.Type.GLOBAL)
            || scopeEntry.isType(ScopeEntry.Type.DYNAMIC)) {
            final String packageName = this.currentPackage.getName();
            final GlobalReference globalRef = identifier.contains(":")
                ? GlobalReference.of(identifier)
                : GlobalReference.of(packageName, identifier);
            this.assignAtom(node, globalRef);
        } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
            final ScopeEntry parentScopeEntry = scopeEntry.getImportedEntry();
            final AstNode lastNodeType = this.currentScopeEntryNodes.get(parentScopeEntry);
            final ExpressionResult result = this.getNodeType(lastNodeType);
            this.assignAtom(node, result);
        } else if (scopeEntry.isType(ScopeEntry.Type.PARAMETER)) {
            final AstNode parameterNode = scopeEntry.getNode();
            final ExpressionResult result = this.getNodeType(parameterNode);
            this.assignAtom(node, result);
        } else {
            final AstNode lastNodeType = this.currentScopeEntryNodes.get(scopeEntry);
            if (lastNodeType != null) {
                final ExpressionResult result = this.getNodeType(lastNodeType);
                this.assignAtom(node, result);
            }
        }
    }

    @Override
    protected void walkPostNumber(final AstNode node) {
        final String tokenValue = node.getTokenValue();

        // Parsable by Long?
        try {
            Long value = Long.parseLong(tokenValue);
            if (value > BIGNUM_START) {
                this.assignAtom(node, SW_BIGNUM);
            } else {
                this.assignAtom(node, SW_INTEGER);
            }
            return;
        } catch (NumberFormatException ex) {
            // pass
        }

        // Parsable by Float?
        try {
            Float.parseFloat(tokenValue);
            this.assignAtom(node, SW_FLOAT);
        } catch (NumberFormatException ex) {
            // pass
        }
    }

    @Override
    protected void walkPostSelf(final AstNode node) {
        this.assignAtom(node, SelfType.INSTANCE);
    }

    @Override
    protected void walkPostSuper(final AstNode node) {
        // Determine which type we are.
        final AbstractType methodOwnerType = this.getMethodOwnerType(node);
        if (methodOwnerType == UndefinedType.INSTANCE) {
            LOGGER.debug("Unknown type for node: {}", node);
            return;
        }

        // Find specified super, if given.
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode != null
            ? identifierNode.getTokenValue()
            : null;
        final AbstractType superType;
        if (identifier != null) {
            superType = methodOwnerType.getParents().stream()
                .filter(parentType -> {
                    final String fullTypeName = parentType.getFullName();
                    final String typeName = fullTypeName.split(":")[1];
                    return identifier.equals(typeName);
                })
                .findAny()
                .orElse(null);
        } else {
            superType = methodOwnerType.getParents().stream()
                .reduce(CombinedType::combine)
                .orElse(null);
        }

        if (superType == null) {
            return;
        }

        final ExpressionResult result = new ExpressionResult(superType);
        this.assignAtom(node, result);
    }

    @Override
    protected void walkPostClone(final AstNode node) {
        this.walkPostSelf(node);
    }

    @Override
    protected void walkPostTrue(final AstNode node) {
        this.assignAtom(node, SW_FALSE);
    }

    @Override
    protected void walkPostFalse(final AstNode node) {
        this.assignAtom(node, SW_FALSE);
    }

    @Override
    protected void walkPostMaybe(final AstNode node) {
        this.assignAtom(node, SW_MAYBE);
    }

    @Override
    protected void walkPostUnset(final AstNode node) {
        this.assignAtom(node, SW_UNSET);
    }

    @Override
    protected void walkPostCharacter(final AstNode node) {
        this.assignAtom(node, SW_CHARACTER);
    }

    @Override
    protected void walkPostRegexp(final AstNode node) {
        this.assignAtom(node, SW_SW_REGEXP);
    }

    @Override
    protected void walkPostString(final AstNode node) {
        this.assignAtom(node, SW_CHAR16_VECTOR);
    }

    @Override
    protected void walkPostSymbol(final AstNode node) {
        this.assignAtom(node, SW_SYMBOL);
    }

    @Override
    protected void walkPostSimpleVector(final AstNode node) {
        this.assignAtom(node, SW_SIMPLE_VECTOR);
    }

    @Override
    protected void walkPostGatherExpression(final AstNode node) {
        this.assignAtom(node, SW_SIMPLE_VECTOR);
    }

    @Override
    protected void walkPostGlobalRef(final AstNode node) {
        this.assignAtom(node, SW_GLOBAL_VARIABLE);
    }

    @Override
    protected void walkPostThisthread(final AstNode node) {
        final AbstractType heavyThreadType = this.typeKeeper.getType(SW_HEAVY_THREAD);
        final AbstractType lightThreadType = this.typeKeeper.getType(SW_LIGHT_THREAD);
        final AbstractType threadType = CombinedType.combine(lightThreadType, heavyThreadType);
        this.assignAtom(node, threadType);
    }

    private void assignAtom(final AstNode node, final GlobalReference globalRef) {
        final AbstractType type = this.typeKeeper.getType(globalRef);
        this.assignAtom(node, type);
    }

    private void assignAtom(final AstNode node, final AbstractType type) {
        final ExpressionResult result = new ExpressionResult(type);
        this.assignAtom(node, result);
    }

    private void assignAtom(final AstNode node, final ExpressionResult result) {
        final AstNode atomNode = node.getParent();
        this.setNodeType(atomNode, result);
    }
    // endregion

    // region: Statements
    @Override
    protected void walkPostReturnStatement(final AstNode node) {
        // Get results.
        final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
        if (tupleNode == null) {
            return;
        }

        // Find related node.
        final AstNode definitionNode =
            node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION, MagikGrammar.PROCEDURE_DEFINITION);

        // Save results at returned node.
        final ExpressionResult result = this.getNodeType(tupleNode);
        this.addNodeType(definitionNode, result);
    }

    @Override
    protected void walkPostVariableDefinition(final AstNode node) {
        // Left side
        final Scope scope = this.globalScope.getScopeForNode(node);
        Objects.requireNonNull(scope);
        final AstNode identifierNode = node.getFirstChild(MagikGrammar.IDENTIFIER);
        final String identifier = identifierNode.getTokenValue();
        final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
        Objects.requireNonNull(scopeEntry);

        // Right side
        final AstNode expressionNode = node.getFirstChild(MagikGrammar.EXPRESSION);
        ExpressionResult result = expressionNode == null
            ? new ExpressionResult(this.typeKeeper.getType(SW_UNSET))
            : this.getNodeType(expressionNode);

        if (scopeEntry.isType(ScopeEntry.Type.LOCAL)
            || scopeEntry.isType(ScopeEntry.Type.DEFINITION)
            || scopeEntry.isType(ScopeEntry.Type.CONSTANT)) {
            final AstNode scopeEntryNode = scopeEntry.getNode();
            this.setNodeType(scopeEntryNode, result);

            // TODO: test if it isn't a slot node
            this.currentScopeEntryNodes.put(scopeEntry, identifierNode);
        } else if (scopeEntry.isType(ScopeEntry.Type.IMPORT)) {
            // TODO: globals/dynamics/...?
            final ScopeEntry importedScopeEntry = scopeEntry.getImportedEntry();
            final AstNode activeImportedNode = this.currentScopeEntryNodes.get(importedScopeEntry);
            result = this.getNodeType(activeImportedNode);
            this.setNodeType(node, result);
        }
    }

    @Override
    protected void walkPostVariableDefinitionMulti(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Take result for right hand.
        final AstNode rightNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.getNodeType(rightNode);

        // Assign to all left hands.
        final List<AstNode> identifierNodes = node
                .getFirstChild(MagikGrammar.IDENTIFIERS_WITH_GATHER)
                .getChildren(MagikGrammar.IDENTIFIER);
        for (int i = 0; i < identifierNodes.size(); ++i) {
            // TODO: Does this work with gather?
            final AstNode identifierNode = identifierNodes.get(i);
            final ExpressionResult partialResult = new ExpressionResult(result.get(i, unsetType));
            this.setNodeType(identifierNode, partialResult);

            // Store 'active' type for future reference.
            final Scope scope = this.globalScope.getScopeForNode(node);
            Objects.requireNonNull(scope);
            final String identifier = identifierNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            // TODO: Test if it isn't a slot node
            this.currentScopeEntryNodes.put(scopeEntry, identifierNode);

        }
    }

    @Override
    protected void walkPostEmitStatement(final AstNode node) {
        // Get results.
        final AstNode tupleNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.getNodeType(tupleNode);

        // Find related node.
        final AstNode bodyNode = node.getFirstAncestor(MagikGrammar.BODY);
        final AstNode expressionNode = bodyNode.getFirstAncestor(
            MagikGrammar.EXPRESSION,  // for BLOCK etc
            MagikGrammar.METHOD_DEFINITION,  // for METHOD_DEFINITION
            MagikGrammar.PROCEDURE_DEFINITION);  // for PROC_DEFINITION

        // Save results.
        this.addNodeType(expressionNode, result);
    }

    @Override
    protected void walkPostLeaveStatement(final AstNode node) {
        // Get results.
        final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = multiValueExprNode != null
            ? this.getNodeType(multiValueExprNode)
            : new ExpressionResult();

        // Find related BODY/EXPRESION nodes.
        final LeaveStatementNodeHelper helper = new LeaveStatementNodeHelper(node);
        final AstNode bodyNode = helper.getRelatedBodyNode();
        final AstNode expressionNode = bodyNode.getFirstAncestor(MagikGrammar.EXPRESSION);
        this.addNodeType(expressionNode, result);
    }

    @Override
    protected void walkPostLoopbody(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get results.
        final AstNode multiValueExprNode = node.getFirstChild(MagikGrammar.TUPLE);
        final ExpressionResult result = this.getNodeType(multiValueExprNode);

        // Find related node to store at.
        final AstNode procMethodDefNode = node.getFirstAncestor(
            MagikGrammar.PROCEDURE_DEFINITION, MagikGrammar.METHOD_DEFINITION);

        // Save results.
        if (this.hasNodeType(procMethodDefNode)) {
            // Combine types.
            final ExpressionResult existingResult = this.getNodeType(procMethodDefNode);
            final ExpressionResult combinedResult = new ExpressionResult(existingResult, result, unsetType);
            this.setLoopbodyNodeType(procMethodDefNode, combinedResult);
        } else {
            this.setLoopbodyNodeType(procMethodDefNode, result);
        }
    }

    @Override
    protected void walkPostProcedureDefinition(final AstNode node) {
        // Get name of procedure.
        String procName = ProcedureInstance.ANONYMOUS_PROCEDURE;
        final AstNode labelNode = node.getFirstChild(MagikGrammar.LABEL);
        if (labelNode != null) {
            final List<AstNode> labelNodeChildren = labelNode.getChildren();
            procName = labelNodeChildren.get(1).getTokenValue();
        }

        // Parameters.
        final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
        final List<Parameter> parameters = new ArrayList<>();
        final List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
        for (final AstNode parameterNode : parameterNodes) {
            final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String identifier = identifierNode.getTokenValue();

            final ParameterNodeHelper helper = new ParameterNodeHelper(parameterNode);
            final Parameter.Modifier modifier;
            if (helper.isOptionalParameter()) {
                modifier = Parameter.Modifier.OPTIONAL;
            } else if (helper.isGatherParameter()) {
                modifier = Parameter.Modifier.GATHER;
            } else {
                modifier = Parameter.Modifier.NONE;
            }

            final AbstractType type = this.getNodeType(parameterNode).get(0, UndefinedType.INSTANCE);
            final Parameter parameter = new Parameter(identifier, modifier, type);
            parameters.add(parameter);
        }

        // Result.
        final ExpressionResult procResult = this.getNodeType(node);

        // Loopbody result.
        final ExpressionResult loopbodyResult = this.getLoopbodyNodeType(node);

        // Create procedure instance.
        final EnumSet<Method.Modifier> modifiers = EnumSet.noneOf(Method.Modifier.class);
        final URI uri = node.getToken().getURI();
        final Location location = new Location(uri, node);
        final GlobalReference globalRef = GlobalReference.of(ProcedureInstance.ANONYMOUS_PROCEDURE);
        final ProcedureInstance procType = new ProcedureInstance(globalRef, procName);
        procType.addMethod(modifiers, location, "invoke()", parameters, null, procResult, loopbodyResult);

        // Store result.
        final ExpressionResult result = new ExpressionResult(procType);
        this.assignAtom(node, result);
    }

    @Override
    protected void walkPostMultipleAssignmentStatement(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Take result for right hand.
        final AstNode rightNode = node.getLastChild();
        final ExpressionResult result = this.getNodeType(rightNode);

        // Assign to all left hands.
        final AstNode assignablesNode = node.getFirstChild(MagikGrammar.MULTIPLE_ASSIGNMENT_ASSIGNABLES);
        final List<AstNode> expressionNodes =
            assignablesNode.getChildren(MagikGrammar.EXPRESSION);
        for (int i = 0; i < expressionNodes.size(); ++i) {
            final AstNode expressionNode = expressionNodes.get(i);
            final ExpressionResult partialResult = new ExpressionResult(result.get(i, unsetType));
            this.setNodeType(expressionNode, partialResult);

            final AstNode identifierNode = AstQuery.getOnlyFromChain(
                expressionNode,
                MagikGrammar.ATOM,
                MagikGrammar.IDENTIFIER);
            if (identifierNode != null) {
                // Store 'active' type for future reference.
                final Scope scope = this.globalScope.getScopeForNode(node);
                Objects.requireNonNull(scope);
                final String identifier = identifierNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                // TODO: Test if it isn't a slot node
                this.currentScopeEntryNodes.put(scopeEntry, expressionNode);
            }
        }
    }

    @Override
    protected void walkPostTryVariable(final AstNode node) {
        final String identifier = node.getTokenValue();

        final AstNode tryNode = node.getParent();
        final List<AstNode> whenNodes = tryNode.getChildren(MagikGrammar.WHEN);
        for (final AstNode whenNode : whenNodes) {
            final AstNode whenBodyNode = whenNode.getFirstChild(MagikGrammar.BODY);
            final Scope scope = this.globalScope.getScopeForNode(whenBodyNode);
            Objects.requireNonNull(scope);
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            this.currentScopeEntryNodes.put(scopeEntry, node);
        }

        final AbstractType conditionType = this.typeKeeper.getType(SW_CONDITION);
        final ExpressionResult result = new ExpressionResult(conditionType);
        this.setNodeType(node, result);
    }
    // endregion

    // region: Expressions
    @Override
    protected void walkPostExpression(final AstNode node) {
        // Check for type annotations, those overrule normal operations.
        final String typeAnnotation = TypeAnnotationHandler.typeAnnotationForExpression(node);
        if (typeAnnotation != null) {
            final String pakkageName = this.currentPackage.getName();
            final ExpressionResult result = this.typeParser.parseExpressionResultString(typeAnnotation, pakkageName);
            this.setNodeType(node, result);
        } else {
            // Normal operations apply.
            final AstNode childNode = node.getFirstChild();
            final ExpressionResult childNodeResult = this.getNodeType(childNode);
            if (this.hasNodeType(childNode)
                && childNodeResult != ExpressionResult.UNDEFINED) {
                this.setNodeType(node, childNodeResult);
            }
        }

        // Check for iter type annotations.
        final String iterTypeAnnotation = TypeAnnotationHandler.iterTypeAnnotationForExpression(node);
        if (iterTypeAnnotation != null) {
            final String pakkageName = this.currentPackage.getName();
            final ExpressionResult iterResult =
                this.typeParser.parseExpressionResultString(iterTypeAnnotation, pakkageName);
            this.setIteratorType(iterResult);
            LOGGER.trace("{} is of iter-type: {}", node, iterResult);
        }
    }

    @Override
    protected void walkPostAssignmentExpression(final AstNode node) {
        // Take result from right hand.
        final AstNode rightNode = node.getLastChild();
        ExpressionResult result = this.getNodeType(rightNode);

        // Walking from back to front, assign result to each.
        // If left hand side is a method call, call the method and update result.
        final List<AstNode> assignedNodes = node.getChildren(MagikGrammar.values());
        assignedNodes.remove(rightNode);
        Collections.reverse(assignedNodes);
        for (final AstNode assignedNode : assignedNodes) {
            if (assignedNode.is(MagikGrammar.POSTFIX_EXPRESSION)) {
                // Find 2nd to last type.
                final int index = assignedNode.getChildren().size() - 2;
                final AstNode semiLastChildNode = assignedNode.getChildren().get(index);
                final ExpressionResult invokedResult = this.getNodeType(semiLastChildNode);

                // Get the result of the invocation.
                final AstNode lastChildNode = assignedNode.getLastChild();
                final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(lastChildNode);
                final String methodName = helper.getMethodName();
                final AbstractType type = invokedResult.get(0, null);
                if (type == null
                    || methodName == null) {
                    LOGGER.debug(""
                        + "Could not get type of last child of, "
                        + "assigned node: {}, last chid node: {}, methodName: {}",
                        assignedNode, lastChildNode, methodName);
                    return;
                }
                result = this.getMethodInvocationResult(type, methodName);
            } else if (assignedNode.is(MagikGrammar.ATOM)
                       && assignedNode.getFirstChild(MagikGrammar.IDENTIFIER) != null) {
                // Store 'active' type for future reference.
                final Scope scope = this.globalScope.getScopeForNode(assignedNode);
                Objects.requireNonNull(scope);
                final String identifier = assignedNode.getTokenValue();
                final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
                this.currentScopeEntryNodes.put(scopeEntry, assignedNode);

                this.setNodeType(assignedNode, result);
            } else if (assignedNode.is(MagikGrammar.SLOT)) {
                throw new UnsupportedOperationException();
            }
        }

        // Store result of complete expression.
        this.setNodeType(node, result);
    }

    @Override
    protected void walkPostAugmentedAssignmentExpression(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        final AbstractType falseType = this.typeKeeper.getType(SW_FALSE);

        // Take result from right hand.
        final AstNode rightNode = node.getLastChild();
        final ExpressionResult rightResult = this.getNodeType(rightNode);

        // Get left hand result.
        final AstNode assignedNode = node.getFirstChild();
        final ExpressionResult leftResult = this.getNodeType(assignedNode);

        // Get operator.
        final AstNode operatorNode = node.getChildren().get(1);
        final String operatorStr = operatorNode.getTokenValue();

        // Evaluate binary operator.
        final AbstractType leftType = leftResult.get(0, unsetType);
        final AbstractType rightType = rightResult.get(0, unsetType);
        final ExpressionResult result;
        switch (operatorStr.toLowerCase()) {
            case "_is":
            case "_isnt":
                result = new ExpressionResult(falseType);
                break;

            case "_andif":  // TODO: Not entirely true.
            case "_orif":   // TODO: Not entirely true.
                result = new ExpressionResult(falseType);
                break;

            default:
                final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
                final BinaryOperator binaryOperator = this.typeKeeper.getBinaryOperator(operator, leftType, rightType);
                final AbstractType resultingType = binaryOperator != null
                    ? binaryOperator.getResultType()
                    : UndefinedType.INSTANCE;
                result = new ExpressionResult(resultingType);
                break;
        }

        // Store result of expression.
        this.setNodeType(node, result);

        if (assignedNode.is(MagikGrammar.ATOM)) {
            // Store 'active' type for future reference.
            final Scope scope = this.globalScope.getScopeForNode(assignedNode);
            Objects.requireNonNull(scope);
            final String identifier = assignedNode.getTokenValue();
            final ScopeEntry scopeEntry = scope.getScopeEntry(identifier);
            this.currentScopeEntryNodes.put(scopeEntry, assignedNode);

            this.setNodeType(assignedNode, result);
        }
    }

    @Override
    protected void walkPostTuple(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        final List<AstNode> childNodes = node.getChildren(MagikGrammar.EXPRESSION);
        final ExpressionResult result;
        if (childNodes.size() == 1) {
            final AstNode firstChildNode = childNodes.get(0);
            result = this.getNodeType(firstChildNode);
        } else {
            // final List<AbstractType> types =
            result = node.getChildren(MagikGrammar.EXPRESSION).stream()
                .map(this::getNodeType)
                .map(expressionResult -> expressionResult.get(0, unsetType))
                .collect(ExpressionResult.COLLECTOR);
        }
        this.setNodeType(node, result);
    }
    // endregion

    // region: Operators
    @Override
    protected void walkPostOrExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostXorExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostAndExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostEqualityExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostRelationalExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostAdditiveExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostMultiplicativeExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    @Override
    protected void walkPostExponentialExpression(final AstNode node) {
        this.applyBinaryOperator(node);
    }

    /**
     * Apply a binary operator.
     * @param node Node to evaluate.
     */
    private void applyBinaryOperator(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        final AbstractType falseType = this.typeKeeper.getType(SW_FALSE);

        // Take left hand side as current.
        final AstNode currentNode = node.getFirstChild();
        ExpressionResult result = this.getNodeType(currentNode);

        final List<AstNode> chainNodes = new ArrayList<>(node.getChildren());
        chainNodes.remove(0);
        for (int i = 0; i < chainNodes.size() - 1; i += 2) {
            // Apply operator to operands.
            final AstNode operatorNode = chainNodes.get(i);
            final String operatorStr = operatorNode.getTokenValue().toLowerCase();
            final AstNode rightNode = chainNodes.get(i + 1);

            final AbstractType leftType = result.get(0, unsetType);
            final ExpressionResult rightResult = this.getNodeType(rightNode);
            final AbstractType rightType = rightResult.get(0, unsetType);

            switch (operatorStr.toLowerCase()) {
                case "_is":
                case "_isnt":
                    result = new ExpressionResult(falseType);
                    break;

                case "_andif":  // TODO: Not entirely true.
                case "_orif":   // TODO: Not entirely true.
                    result = new ExpressionResult(falseType);
                    break;

                default:
                    final BinaryOperator.Operator operator = BinaryOperator.Operator.valueFor(operatorStr);
                    final BinaryOperator binaryOperator =
                        this.typeKeeper.getBinaryOperator(operator, leftType, rightType);
                    final AbstractType resultingType = binaryOperator != null
                        ? binaryOperator.getResultType()
                        : UndefinedType.INSTANCE;
                    result = new ExpressionResult(resultingType);
                    break;
            }
        }

        // Apply operator to operands and store result.
        this.setNodeType(node, result);
    }

    @Override
    protected void walkPostUnaryExpression(final AstNode node) {
        if (node.getTokenValue().equalsIgnoreCase(MagikKeyword.ALLRESULTS.getValue())) {
            this.assignAtom(node, SW_SIMPLE_VECTOR);
        } else if (node.getTokenValue().equalsIgnoreCase(MagikKeyword.SCATTER.getValue())) {
            // We don't know what it will become... for now?
            this.assignAtom(node, ExpressionResult.UNDEFINED);
        }

        this.applyUnaryOperator(node);
    }

    /**
     * Apply a unary operator.
     * @param node Node to evaluate.
     */
    private void applyUnaryOperator(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get operand.
        final AstNode operatedNode = node.getLastChild();
        final ExpressionResult operatedResult = this.getNodeType(operatedNode);
        final AbstractType type = operatedResult.get(0, unsetType);

        // Get operator.
        final String operatorStr = node.getTokenValue();
        final UnaryOperator.Operator operator = UnaryOperator.Operator.valueFor(operatorStr);
        final UnaryOperator unaryOperator = this.typeKeeper.getUnaryOperator(operator, type);

        // Apply opertor to operand and store result.
        final AbstractType resultingType = unaryOperator != null
            ? unaryOperator.getResultType()
            : UndefinedType.INSTANCE;
        final ExpressionResult result = new ExpressionResult(resultingType);
        this.setNodeType(node, result);
    }

    @Override
    protected void walkPostPostfixExpression(final AstNode node) {
        // Get most far right expression result.
        final AstNode rightNode = node.getLastChild();
        final ExpressionResult result = this.getNodeType(rightNode);
        this.setNodeType(node, result);
    }

    @Override
    protected void walkPostMethodInvocation(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get called type for method.
        final AstNode calledNode = node.getPreviousSibling();
        final ExpressionResult calledResult = this.getNodeType(calledNode);
        final AbstractType originalCalledType = calledResult.get(0, unsetType);
        final AbstractType calledType = calledResult.get(0, unsetType) == SelfType.INSTANCE
            ? this.getMethodOwnerType(node)
            : calledResult.get(0, unsetType);

        // Clear iterator results.
        this.setIteratorType(null);

        // Perform method call and store iterator result(s).
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(node);
        final String methodName = helper.getMethodName();
        final Collection<Method> methods = calledType.getMethods(methodName);
        ExpressionResult callResult = null;
        if (methods.isEmpty()) {
            // Method not found, we cannot known what the results will be.
            callResult = ExpressionResult.UNDEFINED;
            this.setIteratorType(ExpressionResult.UNDEFINED);
        } else {
            for (final Method method : methods) {
                // Call.
                if (callResult == null) {
                    callResult = method.getCallResult();
                } else {
                    final ExpressionResult methodResult = method.getCallResult();
                    callResult = new ExpressionResult(callResult, methodResult, unsetType);
                }
                if (originalCalledType != SelfType.INSTANCE) {
                    callResult = callResult.substituteType(SelfType.INSTANCE, calledType);
                }

                // Iterator.
                final ExpressionResult loopbodyResult = method.getLoopbodyResult();
                if (this.getIteratorType() == null) {
                    this.setIteratorType(loopbodyResult);
                } else {
                    final ExpressionResult iterResult =
                        new ExpressionResult(this.iteratorType, loopbodyResult, unsetType);
                    this.setIteratorType(iterResult);
                }

                if (originalCalledType != SelfType.INSTANCE) {
                    final ExpressionResult subbedResult =
                        this.getIteratorType().substituteType(SelfType.INSTANCE, calledType);
                    this.setIteratorType(subbedResult);
                }
            }
        }

        // Store it!
        Objects.requireNonNull(callResult);  // Keep linters happy.
        this.setNodeType(node, callResult);
    }

    @Override
    protected void walkPostProcedureInvocation(final AstNode node) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);

        // Get called type for invocation.
        final AstNode calledNode = node.getPreviousSibling();
        final ExpressionResult calledResult = this.getNodeType(calledNode);
        AbstractType calledType = calledResult.get(0, unsetType);
        final AbstractType originalCalledType = calledType;

        if (calledType == SelfType.INSTANCE) {
            // Replace self type with concrete type, need to know the method we call.
            calledType = this.typeKeeper.getType(SW_PROCEDURE);
        }

        // Clear iterator results.
        this.setIteratorType(null);

        // Perform procedure call.
        ExpressionResult callResult = null;
        if (calledType instanceof ProcedureInstance) {
            final ProcedureInstance procedureType = (ProcedureInstance) calledType;
            final Collection<Method> methods = procedureType.getMethods("invoke()");
            final Method method = methods.stream().findAny().orElse(null);
            Objects.requireNonNull(method);
            callResult = method.getCallResult();

            final ExpressionResult loopbodyResult = method.getLoopbodyResult();
            this.setIteratorType(loopbodyResult);

            if (originalCalledType == SelfType.INSTANCE) {
                callResult = callResult.substituteType(SelfType.INSTANCE, calledType);
                final ExpressionResult subbedResult =
                    this.getIteratorType().substituteType(SelfType.INSTANCE, calledType);
                this.setIteratorType(subbedResult);
            }
        }
        if (callResult == null) {
            callResult = ExpressionResult.UNDEFINED;
            this.setIteratorType(ExpressionResult.UNDEFINED);
        }

        // Store it!
        this.setNodeType(node, callResult);
    }
    // endregion

    private AbstractType getMethodOwnerType(final AstNode node) {
        final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
        if (methodDefNode == null) {
            // This can happen in case of a procedure definition calling a method on _self.
            return UndefinedType.INSTANCE;
        }

        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefNode);
        final GlobalReference globalRef = helper.getTypeGlobalReference();
        return this.typeKeeper.getType(globalRef);
    }

    /**
     * Get the resulting {{ExpressionResult}} from a method invocation.
     * @param calledType Type method is invoked on.
     * @param methodName Name of method to invoke.
     * @return Result of invocation.
     */
    private ExpressionResult getMethodInvocationResult(final AbstractType calledType, final String methodName) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        return calledType.getMethods(methodName).stream()
            .map(method -> method.getCallResult())
            .reduce((result, element) -> new ExpressionResult(result, element, unsetType))
            .orElse(ExpressionResult.UNDEFINED);
    }

}
