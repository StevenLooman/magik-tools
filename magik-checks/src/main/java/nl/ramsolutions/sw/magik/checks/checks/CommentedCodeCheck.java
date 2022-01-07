package nl.ramsolutions.sw.magik.checks.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.parser.MagikCommentExtractor;
import nl.ramsolutions.sw.magik.parser.MagikParser;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * Check for commented code.
 */
@Rule(key = CommentedCodeCheck.CHECK_KEY)
public class CommentedCodeCheck extends MagikCheck {

    @SuppressWarnings("checkstyle:JavadocVariable")
    public static final String CHECK_KEY = "CommentedCode";
    private static final String MESSAGE = "Remove commented code.";

    private static final int DEFAULT_MIN_LINES = 3;

    /**
     * Minimum number of lines before flagging.
     */
    @RuleProperty(
        key = "min lines",
        defaultValue = "" + DEFAULT_MIN_LINES,
        description = "Minimum number of commented lines",
        type = "INTEGER")
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public int minLines = DEFAULT_MIN_LINES;

    @Override
    protected void walkPreMagik(final AstNode node) {
        final Map<Token, List<Token>> commentBlocks = this.extractCommentBlocks(node);
        commentBlocks.entrySet().stream()
            .filter(entry -> {
                final List<Token> blockTokens = entry.getValue();
                final String block = blockTokens.stream()
                    .map(Token::getValue)
                    .collect(Collectors.joining("\n"));
                return blockTokens.size() >= minLines
                    && this.isCommentedCode(block);
            })
            .forEach(entry -> {
                final List<Token> tokens = entry.getValue();
                final Token startToken = tokens.get(0);
                final Token endToken = tokens.get(tokens.size() - 1);
                this.addIssue(
                    startToken.getLine(), startToken.getColumn(),
                    endToken.getLine(), endToken.getLine() + endToken.getOriginalValue().length(),
                    MESSAGE);
            });
    }

    private Map<Token, List<Token>> extractCommentBlocks(final AstNode node) {
        final List<Token> commentTokens = MagikCommentExtractor.extractLineComments(node)
            .collect(Collectors.toList());

        // Iterate over all comment tokens and combine blocks together.
        final Map<Token, List<Token>> commentBlocks = new HashMap<>();
        Token startToken = null;
        Token lastToken = null;
        List<Token> connectingTokens = new ArrayList<>();
        for (final Token token : commentTokens) {
            if (token.getValue().startsWith("##")) {
                continue;
            }

            if (startToken == null) {
                startToken = token;
            } else if (lastToken.getLine() != token.getLine() - 1
                    || lastToken.getColumn() != token.getColumn()) {
                // Block broken, either due to line not connecting or indent changed.

                // Save current block.
                commentBlocks.put(startToken, connectingTokens);

                // Starting new block.
                startToken = token;
                connectingTokens = new ArrayList<>();
            }

            connectingTokens.add(token);
            lastToken = token;
        }

        // Save the last part.
        if (startToken != null) {
            commentBlocks.put(startToken, connectingTokens);
        }
        return commentBlocks;
    }

    private boolean isCommentedCode(final String comment) {
        final String bareComment = comment.replaceAll("^#", "").replace("\n#", "\n");
        final MagikParser parser = new MagikParser();
        try {
            final AstNode magikNode = parser.parseSafe(bareComment);
            return magikNode.getChildren().stream()
                .anyMatch(node -> !node.is(MagikGrammar.SYNTAX_ERROR)
                                && !node.is(GenericTokenType.EOF));
        } catch (RecognitionException exception) {
            return false;
        }
    }

}
