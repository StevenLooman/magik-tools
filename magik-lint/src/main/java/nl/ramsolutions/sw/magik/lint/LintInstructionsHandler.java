package nl.ramsolutions.sw.magik.lint;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;

/**
 * Read mlint: a=b;c=d instructions at lines and scopes.
 */
public class LintInstructionsHandler {

    private static final Pattern MLINT_PATTERN_EOL = Pattern.compile(".*# ?mlint: ?(.*)");
    private static final Pattern MLINT_PATTERN_SINGLE = Pattern.compile("^\\s*# ?mlint: ?(.*)");

    private final MagikFile magikFile;
    private final Map<Scope, Map<String, String>> scopeInstructions = new HashMap<>();
    private final Map<Integer, Map<String, String>> lineInstructions = new HashMap<>();

    /**
     * Constructor.
     * @param magikFile {{MagikVisitorContext}} to use.
     */
    public LintInstructionsHandler(final MagikFile magikFile) {
        this.magikFile = magikFile;
        this.parseInstructionsInScopes();
        this.parseInstructionsFromLines();
    }

    // #region: parsing
    /**
     * Extract instructions-string (at the end) from {{str}}.
     * @param str String to extract from.
     * @param single True if it should be the only thing in the string.
     * @return Unparsed instructions-string.
     */
    @CheckForNull
    private String extractInstructionsInStr(final String str, final boolean single) {
        final Pattern pattern = single
            ? MLINT_PATTERN_SINGLE
            : MLINT_PATTERN_EOL;
        final Matcher matcher = pattern.matcher(str);
        if (!matcher.find()) {
            return null;
        }

        return matcher.group(1);
    }

    /**
     * Parse all mlint instructions.
     * Mlint instructions are key=value pairs, each pair being ;-separated.
     * @param str String to parse.
     * @return Map with parsed instructions.
     */
    private Map<String, String> parseMLintInstructions(final String str) {
        final Map<String, String> instructions = new HashMap<>();

        Arrays.stream(str.split(";"))
            .map(String::trim)
            .forEach(item -> {
                final String[] parts = item.split("=");
                if (parts.length != 2) {
                    return;
                }

                final String key = parts[0].trim();
                final String value = parts[1].trim();
                instructions.put(key, value);
            });

        return instructions;
    }
    // #endregion

    // #region: scopes
    /**
     * Get instructions in {{Scope}} and any ancestor {{Scope}}s.
     * @param line Line in file
     * @param column Column in file
     * @return Map with instructions for the Scope at line/column
     */
    public Map<String, String> getInstructionsInScope(final int line, final int column) {
        final Map<String, String> instructions = new HashMap<>();
        final GlobalScope globalScope = this.magikFile.getGlobalScope();
        if (globalScope == null) {
            return instructions;
        }

        // ensure we can find a Scope
        final Scope fromScope = globalScope.getScopeForLineColumn(line, column);
        if (fromScope == null) {
            return instructions;
        }

        // iterate over all (ancestor) scopes, see if the check is disabled in any scope
        final List<Scope> scopes = fromScope.getSelfAndAncestorScopes();
        // Reverse such that if a narrower scope overrides a broader scope instruction.
        Collections.reverse(scopes);
        for (final Scope scope : scopes) {
            final Map<String, String> scopeInstuctions = this.scopeInstructions.get(scope);
            if (scopeInstuctions != null) {
                instructions.putAll(scopeInstuctions);
            }
        }

        return instructions;
    }

    private void parseInstructionsInScopes() {
        final String[] lines = this.magikFile.getSourceLines();
        final GlobalScope globalScope = this.magikFile.getGlobalScope();
        for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
            final String line = lines[lineNo];
            final String str = this.extractInstructionsInStr(line, true);
            if (str == null) {
                continue;
            }

            final Map<String, String> instructions = this.parseMLintInstructions(str);
            final Scope scope = globalScope.getScopeForLineColumn(lineNo + 1, 0);
            if (scope == null) {
                continue;
            }

            final Map<String, String> instructionsInScope = this.scopeInstructions.get(scope);
            if (instructionsInScope == null) {
                this.scopeInstructions.put(scope, instructions);
            } else {
                instructionsInScope.putAll(instructions);
            }
        }
    }
    // #endregion

    // #region: lines
    /**
     * Get instructions at (the end of) {{line}}.
     * @param line Line number to extract from.
     * @return Instructions at {{line}}.
     */
    public Map<String, String> getInstructionsAtLine(int line) {
        final Map<String, String> instructions = this.lineInstructions.get(line);
        if (instructions == null) {
            return new HashMap<>();
        }
        return instructions;
    }

    /**
     * Read all instructions from all lines.
     */
    private void parseInstructionsFromLines() {
        final String[] lines = this.magikFile.getSourceLines();
        if (lines == null) {
            return;
        }

        for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
            final String line = lines[lineNo];
            final String str = this.extractInstructionsInStr(line, false);
            if (str == null) {
                continue;
            }

            final Map<String, String> instructions = this.parseMLintInstructions(str);
            this.lineInstructions.put(lineNo + 1, instructions);
        }
    }
    // #endregion

}
