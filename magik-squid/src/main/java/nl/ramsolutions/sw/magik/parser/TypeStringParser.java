package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Parser;
import com.sonar.sslr.impl.ast.AstWalker;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import nl.ramsolutions.sw.magik.analysis.typing.types.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.api.TypeStringGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/**
 * Parses {@link TypeString}s/{@link ExpressionResultString}s.
 */
public final class TypeStringParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TypeStringParser.class);

    private TypeStringParser() {
    }

    /**
     * Parse a single {@link TypeString}.
     * @param typeStr String to parse.
     * @return Parsed {@link TypeString}.
     */
    public static TypeString parseTypeString(final @Nullable String typeStr) {
        return TypeStringParser.parseTypeString(typeStr, TypeString.DEFAULT_PACKAGE);
    }

    /**
     * Parse a single {@link TypeString}.
     * @param typeStr String to parse.
     * @param currentPakkage Current package.
     * @return Parsed {@link TypeString}.
     */
    public static TypeString parseTypeString(final @Nullable String typeStr, final String currentPakkage) {
        if (typeStr == null) {
            return TypeString.UNDEFINED;
        }

        final Parser<LexerlessGrammar> parser = new ParserAdapter<>(
            StandardCharsets.ISO_8859_1, TypeStringGrammar.create(TypeStringGrammar.TYPE_STRING));
        final AstNode node = parser.parse(typeStr);
        return TypeStringParser.typeStringNodeToTypeString(node, currentPakkage);
    }

    /**
     * Get parse result.
     * @param typeDocNode
     * @return
     */
    public static AstNode getParsedNodeForTypeString(final AstNode typeDocNode) {
        final Token typeDocToken = typeDocNode.getToken();
        final String typeStr = typeDocToken.getOriginalValue();
        final Parser<LexerlessGrammar> parser = new ParserAdapter<>(
            StandardCharsets.ISO_8859_1, TypeStringGrammar.create(TypeStringGrammar.TYPE_STRING));
        final AstNode node = parser.parse(typeStr);

        // Update token location for easier handling in other parts.
        final int lineOffset = typeDocToken.getLine();
        final int columnOffset = typeDocToken.getColumn();
        node.getTokens().forEach(token -> {
            try {
                final int currentLine = token.getLine();
                final int newLine = currentLine - 1 + lineOffset;
                final Field lineField = Token.class.getDeclaredField("line");
                lineField.setAccessible(true);
                lineField.set(token, newLine);

                final int currentColumn = token.getColumn();
                final int newColumn = currentColumn + columnOffset;
                final Field columnField = Token.class.getDeclaredField("column");
                columnField.setAccessible(true);
                columnField.set(token, newColumn);
            } catch (ReflectiveOperationException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
        });

        return node;
    }

    /**
     * Parse an {@link ExpressionResultString}.
     * @param expressionResultStr String to parse.
     * @param currentPakkage Current pakkage.
     * @return Parsed {@link ExpressionResultString}.
     */
    public static ExpressionResultString parseExpressionResultString(
            final @Nullable String expressionResultStr,
            final String currentPakkage) {
        if (expressionResultStr == null) {
            return ExpressionResultString.UNDEFINED;
        }

        final Parser<LexerlessGrammar> parser = new ParserAdapter<>(
            StandardCharsets.ISO_8859_1, TypeStringGrammar.create(TypeStringGrammar.EXPRESSION_RESULT_STRING));
        final AstNode node = parser.parse(expressionResultStr);
        if (node.hasDescendant(TypeStringGrammar.EXPRESSION_RESULT_STRING_UNDEFINED)) {
            return ExpressionResultString.UNDEFINED;
        }

        return node.getChildren(TypeStringGrammar.TYPE_STRING).stream()
            .map(typeStringNode -> TypeStringParser.typeStringNodeToTypeString(typeStringNode, currentPakkage))
            .collect(ExpressionResultString.COLLECTOR);
    }

    private static TypeString typeStringNodeToTypeString(final AstNode node, final String currentPakkage) {
        final TypeStringBuilderVisitor visitor = new TypeStringBuilderVisitor(currentPakkage);
        final AstWalker walker = new AstWalker(visitor);
        walker.walkAndVisit(node);
        return visitor.getTypeString();
    }

}
