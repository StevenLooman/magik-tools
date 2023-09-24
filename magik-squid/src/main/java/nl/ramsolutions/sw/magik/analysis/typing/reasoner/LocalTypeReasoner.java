package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstWalker;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
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
 * If a type cannot be determined, the {@code UndefinedType} is used instead.
 * </p>
 *
 * <p>
 * If {@code _self} or {@code _clone} is returned in a method, the {@code SelfType} is used.
 * It is up to the user to determine the real type. I.e., in case of {@code sw:date_time_mixin},
 * none of the methods are called on that class directly, but always through an inherited class.
 * On declaration the inheriting classes are unknown, thus if {@code _self} is returned from a mixin,
 * we need to proxy the type.
 * </p>
 */
public class LocalTypeReasoner extends AstWalker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasoner.class);

    private final MagikTypedFile magikFile;
    private final AstNode topNode;
    private final Map<AstNode, ExpressionResult> nodeTypes = new HashMap<>();
    private final Map<AstNode, ExpressionResult> iterNodeTypes = new HashMap<>();
    private final Map<ScopeEntry, AstNode> currentScopeEntryNodes = new HashMap<>();

    /**
     * Constructor.
     * @param magikFile Magik file to reason on.
     */
    public LocalTypeReasoner(final MagikTypedFile magikFile) {
        this.magikFile = magikFile;
        this.topNode = magikFile.getTopNode();
    }

    /**
     * Evaluate the given top {@link AstNode}.
     */
    public void run() {
        // Start walking.
        LOGGER.debug("Start walking");
        this.walkAst(this.topNode);
    }

    /**
     * Get the type for a {@link AstNode}.
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
     * Get the type for a {@link AstNode}.
     * @param node AstNode.
     * @return Resulting type.
     */
    @CheckForNull
    public ExpressionResult getNodeTypeSilent(final AstNode node) {
        return this.nodeTypes.get(node);
    }

    @Override
    protected void walkPostExemplarName(final AstNode node) {
        final IdentifierHandler handler = new IdentifierHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleExemplarName(node);
    }

    @Override
    protected void walkPostMethodDefinition(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleMethodDefinition(node);
    }

    @Override
    protected void walkPostBody(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleBody(node);
    }

    @Override
    protected void walkPostIterableExpression(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleIterableExpression(node);
    }

    @Override
    protected void walkPostParameter(final AstNode node) {
        final ParameterHandler handler = new ParameterHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleParameter(node);
    }

    @Override
    protected void walkPostAssignmentParameter(final AstNode node) {
        final ParameterHandler handler = new ParameterHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleAssignmentParameter(node);
    }

    // region: Atoms
    @Override
    protected void walkPostSlot(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSlot(node);
    }

    @Override
    protected void walkPostIdentifier(final AstNode node) {
        final IdentifierHandler handler = new IdentifierHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleIdentifier(node);
    }

    @Override
    protected void walkPostNumber(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleNumber(node);
    }

    @Override
    protected void walkPostSelf(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSelf(node);
    }

    @Override
    protected void walkPostClone(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleClone(node);
    }

    @Override
    protected void walkPostSuper(final AstNode node) {
        final SuperHandler handler = new SuperHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSuper(node);
    }

    @Override
    protected void walkPostTrue(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleFalse(node);  // True is of type sw:false.
    }

    @Override
    protected void walkPostFalse(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleFalse(node);
    }

    @Override
    protected void walkPostMaybe(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleMaybe(node);
    }

    @Override
    protected void walkPostUnset(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleUnset(node);
    }

    @Override
    protected void walkPostCharacter(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleCharacter(node);
    }

    @Override
    protected void walkPostRegexp(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleRegexp(node);
    }

    @Override
    protected void walkPostString(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleString(node);
    }

    @Override
    protected void walkPostSymbol(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSymbol(node);
    }

    @Override
    protected void walkPostSimpleVector(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSimpleVector(node);
    }

    @Override
    protected void walkPostGatherExpression(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleSimpleVector(node);
    }

    @Override
    protected void walkPostGlobalRef(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleGlobalRef(node);
    }

    @Override
    protected void walkPostThisthread(final AstNode node) {
        final AtomHandler handler = new AtomHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleThread(node);
    }
    // endregion

    // region: Statements
    @Override
    protected void walkPostReturnStatement(final AstNode node) {
        final StatementHandler handler = new StatementHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleReturn(node);
    }

    @Override
    protected void walkPostVariableDefinition(final AstNode node) {
        final StatementHandler handler = new StatementHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleVariableDefinition(node);
    }

    @Override
    protected void walkPostVariableDefinitionMulti(final AstNode node) {
        final StatementHandler handler = new StatementHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleVariableDefinitionMulti(node);
    }

    @Override
    protected void walkPostEmitStatement(final AstNode node) {
        final StatementHandler handler = new StatementHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleEmit(node);
    }

    @Override
    protected void walkPostLeaveStatement(final AstNode node) {
        final StatementHandler handler = new StatementHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleLeave(node);
    }

    @Override
    protected void walkPostLoopbody(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleLoopbody(node);
    }

    @Override
    protected void walkPostProcedureDefinition(final AstNode node) {
        final ProcedureDefinitionHandler handler = new ProcedureDefinitionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleProcedureDefinition(node);
    }

    @Override
    protected void walkPostMultipleAssignmentStatement(final AstNode node) {
        final AssignmentHandler handler = new AssignmentHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleMultipleAssignment(node);
    }

    @Override
    protected void walkPostTryVariable(final AstNode node) {
        final IdentifierHandler handler = new IdentifierHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleTryVariable(node);
    }
    // endregion

    // region: Expressions
    @Override
    protected void walkPostExpression(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleExpression(node);
    }

    @Override
    protected void walkPostAssignmentExpression(final AstNode node) {
        final AssignmentHandler handler = new AssignmentHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleAssignment(node);
    }

    @Override
    protected void walkPostAugmentedAssignmentExpression(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleAugmentedAssignmentExpression(node);
    }

    @Override
    protected void walkPostTuple(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleTuple(node);
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

    private void applyBinaryOperator(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleBinaryExpression(node);
    }

    @Override
    protected void walkPostUnaryExpression(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleUnaryExpression(node);
    }
    // endregion

    @Override
    protected void walkPostPostfixExpression(final AstNode node) {
        final ExpressionHandler handler = new ExpressionHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handlePostfixExpression(node);
    }

    @Override
    protected void walkPostMethodInvocation(final AstNode node) {
        final InvocationHandler handler = new InvocationHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleMethodInvocation(node);
    }

    @Override
    protected void walkPostProcedureInvocation(final AstNode node) {
        final InvocationHandler handler = new InvocationHandler(
            this.magikFile,
            this.nodeTypes,
            this.iterNodeTypes,
            this.currentScopeEntryNodes);
        handler.handleProcedureInvocation(node);
    }

}
