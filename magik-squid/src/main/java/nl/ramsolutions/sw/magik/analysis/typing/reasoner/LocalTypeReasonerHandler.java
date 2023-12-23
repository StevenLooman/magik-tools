package nl.ramsolutions.sw.magik.analysis.typing.reasoner;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeReader;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Base class for handlers.
 */
@SuppressWarnings("visibilitymodifier")
abstract class LocalTypeReasonerHandler {

    protected final LocalTypeReasonerState state;
    protected final ITypeKeeper typeKeeper;
    protected final TypeReader typeReader;

    /**
     * Constructor.
     * @param state Reasoner state.
     */
    LocalTypeReasonerHandler(final LocalTypeReasonerState state) {
        this.state = state;

        this.typeKeeper = state.getMagikFile().getTypeKeeper();
        this.typeReader = new TypeReader(this.typeKeeper);
    }

    /**
     * Get the resulting {@link ExpressionResult} from a method invocation.
     * @param calledType Type method is invoked on.
     * @param methodName Name of method to invoke.
     * @return Result of invocation.
     */
    protected ExpressionResult getMethodInvocationResult(final AbstractType calledType, final String methodName) {
        final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
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
        this.state.setNodeType(atomNode, result);
    }

    /**
     * Add a type for a {@link AstNode}. Combines type if a type is already known.
     * @param node AstNode.
     * @param result ExpressionResult.
     */
    protected void addNodeType(final AstNode node, final ExpressionResult result) {
        if (this.state.hasNodeType(node)) {
            // Combine types.
            final AbstractType unsetType = this.typeKeeper.getType(TypeString.SW_UNSET);
            final ExpressionResult existingResult = this.state.getNodeType(node);
            final ExpressionResult combinedResult = new ExpressionResult(existingResult, result, unsetType);
            this.state.setNodeType(node, combinedResult);
        } else {
            this.state.setNodeType(node, result);
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

    protected GlobalScope getGlobalScope() {
        // TODO: Or should there be a method LocalTypeReasonerState.getGlobalScope()?
        return this.state.getMagikFile().getGlobalScope();
    }

}
