package nl.ramsolutions.sw.magik.analysis.typing;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.api.MagikGrammar;

/**
 * Read type providing/overriding from lines.
 */
public final class TypeAnnotationHandler {

    private static final Pattern TYPE_PATTERN_EOL = Pattern.compile(".*# *type: *(.*)");
    private static final Pattern ITER_TYPE_PATTERN_EOL = Pattern.compile(".*# *iter-type: *(.*)");

    private TypeAnnotationHandler() {
    }

    /**
     * Get the type annotaiton for a given EXPRESSION node.
     * This finds a comment in the form of "# type: exemplar".
     * @param expressionNode {{EXPRESSION}} node.
     * @return Type annotation.
     */
    @CheckForNull
    public static String typeAnnotationForExpression(final AstNode expressionNode) {
        final String comment = TypeAnnotationHandler.getCommentForNode(expressionNode);
        if (comment == null) {
            return null;
        }

        return TypeAnnotationHandler.extractPattern(comment, TypeAnnotationHandler.TYPE_PATTERN_EOL);
    }

    /**
     * Get the type annotaiton for a given EXPRESSION node.
     * This finds a comment in the form of "# iter-type: exemplar".
     * @param expressionNode {{EXPRESSION}} node.
     * @return Type annotation.
     */
    @CheckForNull
    public static String iterTypeAnnotationForExpression(final AstNode expressionNode) {
        final String comment = TypeAnnotationHandler.getCommentForNode(expressionNode);
        if (comment == null) {
            return null;
        }

        return TypeAnnotationHandler.extractPattern(comment, TypeAnnotationHandler.ITER_TYPE_PATTERN_EOL);
    }

    @CheckForNull
    private static String extractPattern(final String comment, final Pattern pattern) {
        final Matcher matcher = pattern.matcher(comment);
        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    @CheckForNull
    private static String getCommentForNode(final AstNode expressionNode) {
        // Try to speed up getting comments: limit number of nodes to extract comments from.
        final AstNode node = expressionNode.getFirstAncestor(
            MagikGrammar.METHOD_DEFINITION,
            MagikGrammar.PROCEDURE_DEFINITION,
            MagikGrammar.MAGIK);
        final int line = expressionNode.getTokenLine();
        return node.getTokens().stream()
            .flatMap(token -> token.getTrivia().stream())
            .filter(Trivia::isComment)
            .map(Trivia::getToken)
            .filter(token -> token.getLine() == line)
            .map(Token::getValue)
            .findAny()
            .orElse(null);
    }

}
