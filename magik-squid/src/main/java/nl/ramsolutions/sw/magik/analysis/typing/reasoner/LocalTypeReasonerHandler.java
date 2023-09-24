package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import java.util.Map;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.ScopeEntry;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for handlers.
 */
@SuppressWarnings("visibilitymodifier")
abstract class LocalTypeReasonerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalTypeReasonerHandler.class);
    private static final TypeString SW_UNSET = TypeString.ofIdentifier("unset", "sw");

    protected final MagikTypedFile magikFile;
    protected final ITypeKeeper typeKeeper;
    protected final Map<AstNode, ExpressionResult> nodeTypes;
    protected final Map<AstNode, ExpressionResult> nodeIterTypes;
    protected final Map<ScopeEntry, AstNode> currentScopeEntryNodes;
    protected final TypeReader typeReader;

    /**
     * Constructor.
     * @param magikFile MagikFile
     * @param nodeTypes Node types.
     * @param nodeIterTypes Node iter types.
     * @param currentScopeEntryNodes Current scope entry nodes.
     */
    LocalTypeReasonerHandler(
            final MagikTypedFile magikFile,
            final Map<AstNode, ExpressionResult> nodeTypes,
            final Map<AstNode, ExpressionResult> nodeIterTypes,
            final Map<ScopeEntry, AstNode> currentScopeEntryNodes) {
        this.magikFile = magikFile;
        this.nodeTypes = nodeTypes;
        this.nodeIterTypes = nodeIterTypes;
        this.currentScopeEntryNodes = currentScopeEntryNodes;

        this.typeKeeper = magikFile.getTypeKeeper();
        this.typeReader = new TypeReader(this.typeKeeper);
    }

    /**
     * Test if the type for a {@link AstNode} is known.
     * @param node AstNode.
     * @return True if known, false otherwise.
     */
    protected boolean hasNodeType(final AstNode node) {
        return this.nodeTypes.containsKey(node);
    }

    /**
     * Get the type for a {@link AstNode}.
     * @param node AstNode.
     * @return Resulting type.
     */
    protected ExpressionResult getNodeType(final AstNode node) {
        final ExpressionResult result = this.nodeTypes.get(node);
        if (result == null) {
            LOGGER.debug("Node without type: {}", node);
            return ExpressionResult.UNDEFINED;
        }

        return result;
    }

    /**
     * Set a type for a {@link AstNode}.
     * @param node AstNode.
     * @param result ExpressionResult.
     */
    protected void setNodeType(final AstNode node, final ExpressionResult result) {
        LOGGER.trace("{} is of type: {}", node, result);
        this.nodeTypes.put(node, result);
    }

    /**
     * Test if the type for a {@link AstNode} is known.
     * @param node AstNode.
     * @return True if known, false otherwise.
     */
    protected boolean hasNodeIterType(final AstNode node) {
        return this.nodeIterTypes.containsKey(node);
    }

    /**
     * Get the loopbody type for a {@link AstNode}.
     * @param node AstNode.
     * @return Resulting type.
     */
    protected ExpressionResult getNodeIterType(final AstNode node) {
        final ExpressionResult result = this.nodeIterTypes.get(node);
        if (result == null) {
            LOGGER.debug("Node without type: {}", node);
            return ExpressionResult.UNDEFINED;
        }

        return result;
    }

    /**
     * Set a loopbody type for a {@link AstNode}.
     * @param node AstNode.
     * @param result Type.
     */
    protected void setNodeIterType(final AstNode node, final ExpressionResult result) {
        this.nodeIterTypes.put(node, result);
    }

    /**
     * Get the resulting {@link ExpressionResult} from a method invocation.
     * @param calledType Type method is invoked on.
     * @param methodName Name of method to invoke.
     * @return Result of invocation.
     */
    protected ExpressionResult getMethodInvocationResult(final AbstractType calledType, final String methodName) {
        final AbstractType unsetType = this.typeKeeper.getType(SW_UNSET);
        return calledType.getMethods(methodName).stream()
            .map(Method::getCallResult)
            .map(this.typeReader::parseExpressionResultString)
            .reduce((result, element) -> new ExpressionResult(result, element, unsetType))
            .orElse(ExpressionResult.UNDEFINED);
    }

    protected void assignAtom(final AstNode node, final TypeString typeString) {
        final AbstractType type = this.typeReader.parseTypeString(typeString);
        this.assignAtom(node, type);
    }

    protected void assignAtom(final AstNode node, final AbstractType type) {
        final ExpressionResult result = new ExpressionResult(type);
        this.assignAtom(node, result);
    }

    protected void assignAtom(final AstNode node, final ExpressionResult result) {
        final AstNode atomNode = node.getParent();
        this.setNodeType(atomNode, result);
    }

    /**
     * Add a type for a {@link AstNode}. Combines type if a type is already known.
     * @param node AstNode.
     * @param result ExpressionResult.
     */
    protected void addNodeType(final AstNode node, final ExpressionResult result) {
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
     * Get method owner type.
     * @param node Node.
     * @return Method owner type.
     */
    protected AbstractType getMethodOwnerType(final AstNode node) {
        final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
        if (methodDefNode == null) {
            // This can happen in case of a procedure definition calling a method on _self.
            return UndefinedType.INSTANCE;
        }

        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(methodDefNode);
        final TypeString typeString = helper.getTypeString();
        return this.typeReader.parseTypeString(typeString);
    }

    protected String getCurrentPackage(final AstNode node) {
        final PackageNodeHelper helper = new PackageNodeHelper(node);
        return helper.getCurrentPackage();
    }

}
