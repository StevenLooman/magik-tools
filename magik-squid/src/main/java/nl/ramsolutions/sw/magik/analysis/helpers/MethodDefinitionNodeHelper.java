package nl.ramsolutions.sw.magik.analysis.helpers;

import com.sonar.sslr.api.AstNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikKeyword;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;

/**
 * Helper for METHOD_DEFINITION nodes.
 */
public class MethodDefinitionNodeHelper {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Node to encapsulate.
     */
    public MethodDefinitionNodeHelper(final AstNode node) {
        if (!node.is(MagikGrammar.METHOD_DEFINITION)) {
            throw new IllegalArgumentException();
        }

        this.node = node;
    }

    /**
     * Get name of method.
     * @return Method name.
     */
    public String getMethodName() {
        final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
        final List<AstNode> parameterNodes = parametersNode != null
            ? parametersNode.getChildren(MagikGrammar.PARAMETER)
            : Collections.emptyList();

        final AstNode methodNameNode = node.getFirstChild(MagikGrammar.METHOD_NAME);
        final StringBuilder builder = new StringBuilder();
        if (methodNameNode != null) {
            final String tokenValue = methodNameNode.getTokenValue();
            builder.append(tokenValue);
        }
        if (parametersNode != null) {
            if (this.anyChildTokenIs(parametersNode, MagikPunctuator.SQUARE_L)) {
                builder.append("[");
                final int commaCount = parameterNodes.size() - 1;
                final String repeatedCommas = ",".repeat(commaCount);
                builder.append(repeatedCommas);
                builder.append("]");
            }
            if (this.anyChildTokenIs(parametersNode, MagikPunctuator.PAREN_L)) {
                builder.append("()");
            }
        }
        if (this.anyChildTokenIs(node, MagikOperator.CHEVRON)) {
            builder.append(MagikOperator.CHEVRON.getValue());
        }
        if (this.anyChildTokenIs(node, MagikOperator.BOOT_CHEVRON)) {
            builder.append(MagikOperator.BOOT_CHEVRON.getValue());
        }

        return builder.toString();
    }

    /**
     * Get exemplar + method name.
     * @return Exemplar + method name.
     */
    public String getExemplarMethodName() {
        final TypeString exemplarName = this.getTypeString();
        final String methodName = this.getMethodName();
        if (methodName.startsWith("[")) {
            return exemplarName.getIdentifier() + methodName;
        }

        return exemplarName.getIdentifier() + "." + methodName;
    }

    /**
     * Get global reference to type the method is defined on.
     * @return TypeString to type.
     */
    public TypeString getTypeString() {
        final PackageNodeHelper packageHelper = new PackageNodeHelper(this.node);
        final String pakkage = packageHelper.getCurrentPackage();
        final AstNode exemplarNameNode = this.node.getFirstChild(MagikGrammar.EXEMPLAR_NAME);
        final String exemplarName = exemplarNameNode.getTokenValue();
        return TypeString.ofIdentifier(exemplarName, pakkage);
    }

    /**
     * Get package + exemplar + method name.
     * @return Package + exemplar + method name.
     */
    public String getFullExemplarMethodName() {
        final PackageNodeHelper packageHelper = new PackageNodeHelper(this.node);
        final String pakkageName = packageHelper.getCurrentPackage();
        return pakkageName + ":" + this.getExemplarMethodName();
    }

    /**
     * Get parameters + nodes.
     * @return Map with parameters + PARAMETER nodes.
     */
    public Map<String, AstNode> getParameterNodes() {
        return Stream.concat(
                this.node.getChildren(MagikGrammar.PARAMETERS).stream()
                    .flatMap(parametersNode -> parametersNode.getChildren(MagikGrammar.PARAMETER).stream()),
                this.node.getChildren(MagikGrammar.ASSIGNMENT_PARAMETER).stream())
            .collect(Collectors.toMap(
                parameterNode -> parameterNode.getFirstDescendant(MagikGrammar.IDENTIFIER).getTokenValue(),
                parameterNode -> parameterNode));
    }

    private Collection<AstNode> getMethodModifiers() {
        final AstNode modifiersNode = this.node.getFirstChild(MagikGrammar.METHOD_MODIFIERS);
        if (modifiersNode == null) {
            return Collections.emptySet();
        }
        return modifiersNode.getChildren();
    }

    /**
     * Test if method is an `_abstract` method.
     * @return
     */
    public boolean isAbstractMethod() {
        final String modifier = MagikKeyword.ABSTRACT.getValue();
        return this.getMethodModifiers().stream()
            .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
    }

    /**
     * Test if method is a `_private` method.
     * @return
     */
    public boolean isPrivateMethod() {
        final String modifier = MagikKeyword.PRIVATE.getValue();
        return this.getMethodModifiers().stream()
            .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
    }

    /**
     * Test if method is an `_iter` method.
     * @return
     */
    public boolean isIterMethod() {
        final String modifier = MagikKeyword.ITER.getValue();
        return this.getMethodModifiers().stream()
            .anyMatch(modifierNode -> modifierNode.getTokenValue().equalsIgnoreCase(modifier));
    }

    /**
     * Test if method returns anything.
     * @return
     */
    public boolean returnsAnything() {
        final List<AstNode> returnStatementNodes = node.getDescendants(MagikGrammar.RETURN_STATEMENT);
        final boolean hasReturn = returnStatementNodes.stream()
            .filter(statementNode -> statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == null)
            .anyMatch(statementNode -> statementNode.hasDescendant(MagikGrammar.TUPLE));

        final List<AstNode> emitStatementNodes =
            node.getFirstChild(MagikGrammar.BODY).getChildren(MagikGrammar.EMIT_STATEMENT);
        final boolean hasEmit = !emitStatementNodes.isEmpty();

        return hasReturn || hasEmit;
    }

    /**
     * Test if method has a loopbody statement.
     * @return
     */
    public boolean hasLoopbody() {
        return node.getDescendants(MagikGrammar.LOOPBODY).stream()
            .anyMatch(statementNode -> statementNode.getFirstAncestor(MagikGrammar.PROCEDURE_DEFINITION) == null);
    }

    private boolean anyChildTokenIs(final AstNode parentNode, final MagikOperator magikOperator) {
        return parentNode.getChildren().stream()
            .filter(childNode -> childNode.isNot(MagikGrammar.values()))
            .map(AstNode::getTokenValue)
            .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(magikOperator.getValue()));
    }

    private boolean anyChildTokenIs(final AstNode parentNode, final MagikPunctuator magikPunctuator) {
        return parentNode.getChildren().stream()
            .filter(childNode -> childNode.isNot(MagikGrammar.values()))
            .map(AstNode::getTokenValue)
            .anyMatch(tokenValue -> tokenValue.equalsIgnoreCase(magikPunctuator.getValue()));
    }

}
