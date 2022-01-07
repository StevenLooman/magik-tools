package nl.ramsolutions.sw.magik.analysis;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Read key/value pairs in the form of <pre>owner: a=b;c=d</pre> instructions at lines.
 */
public class KeyValueAtLineInstructionExtractor {

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(";?\\s*(\\w+)\\s*=\\s*(\\w+)");

    private final AstNode node;
    private final Pattern capturePattern;
    private final Map<Integer, Map<String, String>> instructions;

    /**
     * Constructor.
     * @param node AstNode to extract from.
     * @param owner Owner to get instructions from.
     */
    public KeyValueAtLineInstructionExtractor(final AstNode node, final String owner) {
        this.node = node;
        this.capturePattern = Pattern.compile("#\\s*" + owner + ":\\s*((?:;?\\s*\\w+\\s*=\\s*\\w+)*)");
        this.instructions = extractInstructions();
    }

    private Map<Integer, Map<String, String>> extractInstructions() {
        return this.node.getTokens().stream()
            .flatMap(token -> token.getTrivia().stream())
            .filter(Trivia::isComment)
            .flatMap(trivia -> trivia.getTokens().stream())
            .filter(token -> this.capturePattern.matcher(token.getValue()).matches())
            .collect(Collectors.toMap(
                Token::getLine,
                token -> {
                    // Extract whole instructions part.
                    final String tokenValue = token.getValue();
                    final Matcher matcherWhole = this.capturePattern.matcher(tokenValue);
                    matcherWhole.find();
                    final String instructionsWhole = matcherWhole.group(1);

                    // Extract individual instructions.
                    final Map<String, String> lineInstructions = new HashMap<>();
                    final Matcher matcherInstruction = KEY_VALUE_PATTERN.matcher(instructionsWhole);
                    while (matcherInstruction.find()) {
                        final String key = matcherInstruction.group(1);
                        final String value = matcherInstruction.group(2);
                        lineInstructions.put(key, value);
                    }
                    return lineInstructions;
                }));
    }

    /**
     * Get the instructions at line of {{AstNode}}.
     * @param searchNode Line of {{AstNode}} to get instructions from.
     * @return Map with key/value instructions at node.
     */
    public Map<String, String> getInstructions(final AstNode searchNode) {
        final Token token = searchNode.getToken();
        if (token == null) {
            return Collections.emptyMap();
        }

        final int line = token.getLine();
        return this.getInstructions(line);
    }

    /**
     * Get the instructions at line.
     * @param line Line to get instruction from.
     * @return Map with key/value instructions at line.
     */
    public Map<String, String> getInstructions(final int line) {
        return this.instructions.getOrDefault(line, Collections.emptyMap());
    }

}
