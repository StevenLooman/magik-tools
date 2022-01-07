package nl.ramsolutions.sw.magik.analysis.definitions;

import com.sonar.sslr.api.AstNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Method definition parser.
 */
public class MethodDefinitionParser {

    private final AstNode node;

    /**
     * Constructor.
     * @param node Method definition node.
     */
    public MethodDefinitionParser(final AstNode node) {
        if (node.isNot(MagikGrammar.METHOD_DEFINITION)) {
            throw new IllegalArgumentException();
        }

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
            return List.of();
        }

        // Figure pakkage.
        final String pakkage = this.getCurrentPakkage();

        // Figure exemplar name & method name.
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(this.node);
        final String exemplarName = helper.getExemplarName();
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
        final AstNode parametersNode = this.node.getFirstChild(MagikGrammar.PARAMETERS);
        final List<ParameterDefinition> parameters = this.createParameterDefinitions(parametersNode);

        final AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
        final ParameterDefinition assignmentParamter =
            this.createAssignmentParameterDefinition(assignmentParameterNode);

        final MethodDefinition methodDefinition = new MethodDefinition(
            this.node, pakkage, exemplarName, methodName, modifiers, parameters, assignmentParamter);
        return List.of(methodDefinition);
    }

    private String getCurrentPakkage() {
        final PackageNodeHelper helper = new PackageNodeHelper(this.node);
        return helper.getCurrentPackage();
    }

    private List<ParameterDefinition> createParameterDefinitions(final @Nullable AstNode parametersNode) {
        if (parametersNode == null) {
            return Collections.emptyList();
        }

        final List<ParameterDefinition> parameterDefinitions = new ArrayList<>();
        for (final AstNode parameterNode : parametersNode.getChildren(MagikGrammar.PARAMETER)) {
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

            final ParameterDefinition parameterDefinition =
                new ParameterDefinition(parameterNode, identifier, modifier);
            parameterDefinitions.add(parameterDefinition);
        }

        return parameterDefinitions;
    }

    @CheckForNull
    private ParameterDefinition createAssignmentParameterDefinition(final @Nullable AstNode assignmentParameterNode) {
        if (assignmentParameterNode == null) {
            return null;
        }

        final AstNode parameterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
        final String identifier = parameterNode.getTokenValue();
        return new ParameterDefinition(parameterNode, identifier, ParameterDefinition.Modifier.NONE);
    }

}
