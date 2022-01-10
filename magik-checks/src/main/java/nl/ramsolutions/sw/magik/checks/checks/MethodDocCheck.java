package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.parser.MethodDocParser;
import org.sonar.check.Rule;

/**
 * Check method doc.
 */
@DisabledByDefault
@Rule(key = MethodDocCheck.CHECK_KEY)
public class MethodDocCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "MethodDoc";

    private static final String MESSAGE = "No or invalid method doc: %s.";

    @Override
    protected void walkPreMethodDefinition(final AstNode node) {
        final MethodDocParser docParser = new MethodDocParser(node);

        // ensure sections exist
        final List<String> mandatorySections = List.of("function", "returns");
        for (final String section : mandatorySections) {
            if (!docParser.hasSection(section)) {
                final String message = String.format(MESSAGE, section);
                this.addIssue(node, message);
            }
        }

        // compare parameters of node and doc
        final Set<String> methodParameters = this.getMethodParameters(node);
        final Map<String, String> docParameters = docParser.getParameterTexts();
        for (final String methodParameter : methodParameters) {
            if (!docParameters.containsKey(methodParameter)) {
                final String message = String.format(MESSAGE, "Parameter: " + methodParameter);
                this.addIssue(node, message);
            }
        }
        for (final String docParameter : docParameters.keySet()) {
            if (!methodParameters.contains(docParameter)) {
                final String message = String.format(MESSAGE, "Parameter: " + docParameter);
                this.addIssue(node, message);
            }
        }
    }

    private Set<String> getMethodParameters(final AstNode node) {
        final Set<String> parameters = new HashSet<>();

        // Parameters.
        final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
        if (parametersNode != null) {
            final List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
            final List<String> names = parameterNodes.stream()
                .map(parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER))
                .map(AstNode::getTokenValue)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
            parameters.addAll(names);
        }

        // Assignment parameter.
        final AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
        if (assignmentParameterNode != null) {
            final AstNode paramaterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
            final AstNode identifierNode = paramaterNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String name = identifierNode.getTokenValue().toLowerCase();
            parameters.add(name);
        }

        return parameters;
    }

}
