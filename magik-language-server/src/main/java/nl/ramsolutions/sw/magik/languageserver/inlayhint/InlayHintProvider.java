package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.Position;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodInvocationNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.ITypeKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.types.AbstractType;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResult;
import nl.ramsolutions.sw.magik.analysis.typing.types.Method;
import nl.ramsolutions.sw.magik.analysis.typing.types.Parameter;
import nl.ramsolutions.sw.magik.analysis.typing.types.SelfType;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.analysis.typing.types.UndefinedType;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provider for inlay hints.
 */
public class InlayHintProvider {

    /**
     * Set capabilities for inlay hints.
     * @param capabilities Server capabilities.
     */
    public void setCapabilities(final ServerCapabilities capabilities) {
        capabilities.setInlayHintProvider(true);
    }

    /**
     * Provide inlay hints for the given file.
     * @param magikFile Magik file.
     * @param range Range in file.
     * @return List of inlay hints.
     */
    public List<InlayHint> provideInlayHints(final MagikTypedFile magikFile, final Range range) {
        // Get argument hints from method invocations.
        final AstNode topNode = magikFile.getTopNode();
        return topNode.getDescendants(MagikGrammar.METHOD_INVOCATION).stream()
            // TODO: Filter based on range.
            .map(node -> this.getMethodInvocationInlayHints(magikFile, node))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<InlayHint> getMethodInvocationInlayHints(
            final MagikTypedFile magikFile, final AstNode methodInvocationNode) {
        final AstNode argumentsNode = methodInvocationNode.getFirstDescendant(MagikGrammar.ARGUMENTS);
        if (argumentsNode == null) {
            return Collections.emptyList();
        }

        // Get type from method invocation.
        final AbstractType receiverType = this.getTypeInvokedOn(magikFile, methodInvocationNode);
        if (receiverType == null) {
            return Collections.emptyList();
        }

        // Get invoked method.
        final MethodInvocationNodeHelper helper = new MethodInvocationNodeHelper(methodInvocationNode);
        final String methodName = helper.getMethodName();
        final Collection<Method> methods = receiverType.getMethods(methodName);
        if (methods.isEmpty()) {
            return Collections.emptyList();
        }

        final Method method = methods.iterator().next();  // TODO: Over all methods?
        final List<Parameter> parameters = method.getParameters();

        // Get argument hints.
        final List<InlayHint> inlayHints = new ArrayList<>();
        final List<AstNode> argumentNodes = argumentsNode.getDescendants(MagikGrammar.ARGUMENT);
        for (int i = 0; i < argumentNodes.size(); ++i) {
            final AstNode argumentNode = argumentNodes.get(i);
            if (!this.isSimpleAtomArgument(argumentNode)
                || i >= parameters.size()) {
                continue;
            }

            final Parameter parameter = parameters.get(i);
            final InlayHint inlayHint = this.getArgumentInlayHint(argumentNode, parameter);
            inlayHints.add(inlayHint);
        }

        return inlayHints;
    }

    private boolean isSimpleAtomArgument(final AstNode argumentNode) {
        final AstNode expressionNode = argumentNode.getFirstDescendant(MagikGrammar.EXPRESSION);
        if (expressionNode == null) {
            return false;
        }

        final AstNode atomNode = expressionNode.getFirstChild(MagikGrammar.ATOM);
        if (atomNode == null) {
            return false;
        }

        return atomNode.getFirstChild(
            MagikGrammar.UNSET,
            MagikGrammar.TRUE,
            MagikGrammar.FALSE,
            MagikGrammar.MAYBE,
            MagikGrammar.NUMBER,
            MagikGrammar.SYMBOL,
            MagikGrammar.STRING) != null;
    }

    private InlayHint getArgumentInlayHint(final AstNode argumentNode, final Parameter parameter) {
        final AstNode atomNode = argumentNode.getFirstDescendant(MagikGrammar.ATOM);
        final Position position = Position.fromTokenStart(atomNode.getToken());
        return new InlayHint(
            Lsp4jConversion.positionToLsp4j(position),
            Either.forLeft(parameter.getName() + ":"));
    }

    /**
     * Get type method invoked on.
     * @param magikFile Magik file.
     * @param node METHOD_INVOCATION node.
     * @return Type method is invoked, or UNDEFINED_TYPE.
     */
    protected AbstractType getTypeInvokedOn(final MagikTypedFile magikFile, final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_INVOCATION)) {
            throw new IllegalStateException();
        }

        final AstNode previousSibling = node.getPreviousSibling();
        final LocalTypeReasoner reasoner = magikFile.getTypeReasoner();
        final ExpressionResult result = reasoner.getNodeType(previousSibling);
        final AbstractType type = result.get(0, UndefinedType.INSTANCE);
        if (type == SelfType.INSTANCE) {
            final AstNode methodDefNode = node.getFirstAncestor(MagikGrammar.METHOD_DEFINITION);
            final ITypeKeeper typeKeeper = magikFile.getTypeKeeper();
            return this.getTypeOfMethodDefinition(typeKeeper, methodDefNode);
        }

        return type;
    }

    /**
     * Get type of method definition.
     * @param node METHOD_DEFINITION node.
     * @return
     */
    protected AbstractType getTypeOfMethodDefinition(final ITypeKeeper typeKeeper, final AstNode node) {
        final MethodDefinitionNodeHelper methodDefHelper = new MethodDefinitionNodeHelper(node);
        final TypeString typeString = methodDefHelper.getTypeString();
        return typeKeeper.getType(typeString);
    }

}
