package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;

/**
 * Parse/extract the method doc for a given method.
 * Looks for sections, which start with a single word and a ':',
 * not necessarily directly connected.
 *
 * <p>
 * Parameters can be retrieved.
 * </p>
 *
 * <pre>
 * E.g,.
 * ## Function  : Example
 * ## Parameters: PARAM1: Example param 1
 * ##             PARAM1: Example param 2
 * ## Returns   : boolean
 * </pre>
 */
public class MethodDocParser {

    private static final Pattern SECTION_START_PATTERN = Pattern.compile("^## (\\w+\\??)[ \t]*:(.*)");

    private Map<String, List<Token>> sections;
    private Map<String, List<Token>> parameters;

    /**
     * Constructor/initiator.
     * @param methodNode {{AstNode}} to analyze.
     */
    public MethodDocParser(final AstNode methodNode) {
        this.sections = new Hashtable<>();
        this.parameters = new Hashtable<>();

        final List<Token> tokens = MagikCommentExtractor.extractDocComments(methodNode)
            .collect(Collectors.toList());
        this.parseSections(tokens, this.sections);

        final List<Token> parameterTokens = this.sections.get("parameters");
        if (parameterTokens != null) {
            List<Token> fixedParameterTokens = this.stripSectionStart(parameterTokens);
            this.parseSections(fixedParameterTokens, this.parameters);
        }
    }

    private void parseSections(final List<Token> commentTokens, final Map<String, List<Token>> targetSections) {
        String currentKey = null;
        final List<Token> sectionTokens = new ArrayList<>();
        for (final Token token : commentTokens) {
            final String line = token.getOriginalValue();

            final Matcher matcher = MethodDocParser.SECTION_START_PATTERN.matcher(line);
            if (matcher.find()) {
                // New section found.
                if (currentKey != null) {
                    // New section started, store current section.
                    targetSections.put(currentKey, List.copyOf(sectionTokens));
                    sectionTokens.clear();
                }

                // Add token to current section.
                currentKey = matcher.group(1).toLowerCase();
                sectionTokens.add(token);
            } else if (currentKey != null) {
                // Add token to current section.
                sectionTokens.add(token);
            }
        }

        // Save anything left in collector.
        if (currentKey != null) {
            targetSections.put(currentKey, sectionTokens);
        }
    }

    /**
     * Strip the "## Parameters: " from the first token.
     * @param tokens Section tokens.
     * @return New tokens.
     */
    private List<Token> stripSectionStart(final List<Token> tokens) {
        if (tokens.isEmpty()) {
            return tokens;
        }

        // Strip section name from first token.
        final Token firstToken = tokens.get(0);
        final String firstTokenValue = firstToken.getOriginalValue();
        int stringOffset = "## ".length();
        final Matcher matcher = MethodDocParser.SECTION_START_PATTERN.matcher(firstTokenValue);
        if (matcher.find()) {
            stringOffset = matcher.start(2) + 1;
        }

        final int finalStringOffset = stringOffset;
        return tokens.stream()
            .sequential()
            .map(token -> {
                final String tokenValue = token.getValue();
                final String newTokenValue = "## " + tokenValue.substring(finalStringOffset);
                final int tokenOffset = tokenValue.length() - newTokenValue.length();
                return Token.builder(token)
                    .setColumn(token.getColumn() + tokenOffset)
                    .setValueAndOriginalValue(newTokenValue)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Test if method has any doc comments at all.
     * @return True if has doc comments, false otherwise.
     */
    public boolean hasDoc() {
        return !this.sections.isEmpty();
    }

    /**
     * Test if a section with {{name}} was found.
     * @param name Name of section.
     * @return True if present, false otherwise.
     */
    public boolean hasSection(final String name) {
        return this.sections.containsKey(name);
    }

    /**
     * Get the text for a section.
     * @param name Name of section.
     * @return Text of section.
     */
    @CheckForNull
    public String getSectionText(final String name) {
        if (!this.sections.containsKey(name)) {
            return null;
        }

        final List<Token> tokens = this.sections.get(name);
        return this.extractSectionText(tokens);
    }

    /**
     * Get all parameter descriptions.
     * @return Parameter descriptions.
     */
    public Map<String, String> getParameterTexts() {
        return this.parameters.keySet().stream()
            .collect(Collectors.toMap(
                key -> key,
                key -> {
                    final List<Token> paramTokens = this.parameters.get(key);
                    return this.extractSectionText(paramTokens);
                }));
    }

    private String extractSectionText(final List<Token> tokens) {
        int offset = "## ".length();
        final Token firstToken = tokens.get(0);
        final String firstTokenValue = firstToken.getOriginalValue();
        final Matcher matcher = MethodDocParser.SECTION_START_PATTERN.matcher(firstTokenValue);
        if (matcher.find()) {
            offset = matcher.start(2) + 1;
        }
        final int finalOffset = offset;

        return tokens.stream()
            .map(token -> {
                final String tokenValue = token.getValue();
                return tokenValue.substring(finalOffset);
            })
            .collect(Collectors.joining("\n"));
    }

}
