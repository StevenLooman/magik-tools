package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.definitions.ModuleDefinitionScanner;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.analysis.definitions.Definition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.ParameterDefinition;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.analysis.helpers.ParameterNodeHelper;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.TypeDocParser;

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
            loopResult);
        return List.of(methodDefinition);
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
