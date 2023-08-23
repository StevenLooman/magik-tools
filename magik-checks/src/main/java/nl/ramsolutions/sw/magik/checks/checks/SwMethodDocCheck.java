package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.analysis.helpers.MethodDefinitionNodeHelper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.DisabledByDefault;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check if method docs are valid, according to SW style.
 */
@DisabledByDefault
@Rule(key = SwMethodDocCheck.CHECK_KEY)
public class SwMethodDocCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "SwMethodDoc";
    private static final String MESSAGE = "No or invalid method doc: %s.";

    private static final boolean DEFAULT_ALLOW_BLANK_METHOD_DOC = false;
    private static final String PARAMETER_REGEXP = "[ \t]?([\\p{Lu}\\d_?]+)[^\\p{Lu}\\d_?]?";
    private static final Pattern PARAMETER_PATTERN = Pattern.compile(PARAMETER_REGEXP);

    /**
     * Allow blank method doc.
     */
    @RuleProperty(
        key = "allow blank method doc",
        defaultValue = "" + DEFAULT_ALLOW_BLANK_METHOD_DOC,
        description = "Allow blank method doc",
        type = "BOOLEAN")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public boolean allowBlankMethodDoc = DEFAULT_ALLOW_BLANK_METHOD_DOC;

    @Override
    protected void walkPreMethodDefinition(final AstNode node) {
        final MethodDefinitionNodeHelper helper = new MethodDefinitionNodeHelper(node);
        final AstNode methodNameNode = helper.getMethodNameNode();
        final String methodDoc = this.extractDoc(node);
        if (methodDoc.isBlank()
            && !allowBlankMethodDoc) {
            final String message = String.format(MESSAGE, "all");
            this.addIssue(methodNameNode, message);
            return;
        }

        final Set<String> methodParameters = this.getMethodParameters(node);
        final Set<String> docParameters = this.getDocParameters(node);
        methodParameters.removeAll(docParameters);
        for (final String missing : methodParameters) {
            final String message = String.format(MESSAGE, missing);
            this.addIssue(methodNameNode, message);
        }
    }

    private Set<String> getMethodParameters(final AstNode node) {
        final Set<String> parameters = new HashSet<>();

        // parameters
        final AstNode parametersNode = node.getFirstChild(MagikGrammar.PARAMETERS);
        if (parametersNode != null) {
            final List<AstNode> parameterNodes = parametersNode.getChildren(MagikGrammar.PARAMETER);
            final List<String> names = parameterNodes.stream()
                .map(parameterNode -> parameterNode.getFirstChild(MagikGrammar.IDENTIFIER))
                .map(AstNode::getTokenValue)
                .map(String::toUpperCase)
                .collect(Collectors.toList());
            parameters.addAll(names);
        }

        // assignment parameter
        final AstNode assignmentParameterNode = node.getFirstChild(MagikGrammar.ASSIGNMENT_PARAMETER);
        if (assignmentParameterNode != null) {
            final AstNode parameterNode = assignmentParameterNode.getFirstChild(MagikGrammar.PARAMETER);
            final AstNode identifierNode = parameterNode.getFirstChild(MagikGrammar.IDENTIFIER);
            final String name = identifierNode.getTokenValue().toUpperCase();
            parameters.add(name);
        }

        return parameters;
    }

    private String extractDoc(final AstNode node) {
        return MagikCommentExtractor.extractDocComments(node)
            .map(Token::getValue)
            .map(comment -> comment.substring("##".length()))
            .collect(Collectors.joining("\n"));
    }

    private Set<String> getDocParameters(final AstNode node) {
        final String methodDoc = this.extractDoc(node);
        final Set<String> uppercased = new HashSet<>();

        final Matcher matcher = PARAMETER_PATTERN.matcher(methodDoc);
        while (matcher.find()) {
            final String name = matcher.group(1);
            uppercased.add(name);
        }

        return uppercased;
    }

}
