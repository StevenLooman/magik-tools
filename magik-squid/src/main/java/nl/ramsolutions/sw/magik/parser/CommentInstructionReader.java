package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;

/**
 * Comment Instruction reader.
 *
 * Any line number is 0-based, as is in {@link MagikFile}.
 */
public class CommentInstructionReader {

    /**
     * Instruction.
     */
    public static final class Instruction {

        /**
         * Sort of instruction.
         */
        @SuppressWarnings("checkstyle:JavadocVariable")
        public enum Sort {
            STATEMENT,
            SCOPE
        }

        private final String name;
        private final Sort sort;
        private final Pattern pattern;

        /**
         * Constructor.
         * @param name Name.
         * @param sort Sort.
         */
        public Instruction(final String name, final Sort sort) {
            this.name = name;
            this.sort = sort;
            this.pattern = sort == Sort.SCOPE
                ? Pattern.compile("^\\s*#\\s*" + Pattern.quote(name) + ":\\s*(.*)$")
                : Pattern.compile("^\\s*[^\\s].*#\\s*" + Pattern.quote(name) + ":\\s*(.*)$");
        }

        public String getName() {
            return this.name;
        }

        public Sort getSort() {
            return this.sort;
        }

        public Pattern getPattern() {
            return this.pattern;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.sort, this.pattern);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null) {
                return false;
            }

            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final Instruction other = (Instruction) obj;
            return Objects.equals(this.name, other.name)
                && Objects.equals(this.sort, other.sort)
                && Objects.equals(this.pattern, other.pattern);
        }
    }

    private final MagikFile magikFile;
    private final Set<Instruction> instructions;
    private final Map<Integer, Map<Instruction, String>> lineInstructions = new HashMap<>();
    private boolean isRead;

    /**
     * Constructor.
     * @param magikFile Magik file.
     * @param instructions Instrunction to read.
     */
    public CommentInstructionReader(final MagikFile magikFile, final Set<Instruction> instructions) {
        this.magikFile = magikFile;
        this.instructions = instructions;
    }

    /**
     * Get instruction for node.
     * Simply takes line number from node and gets instruction(s) at line.
     * @param node Node.
     * @param instruction Instruction.
     * @return Instruction, if any.
     */
    @CheckForNull
    public String getInstructionForNode(final AstNode node, final Instruction instruction) {
        final int lineNo = node.getTokenLine() - 1;  // 1-based to 0-based.
        return this.getInstructionsAtLine(lineNo, instruction);
    }

    /**
     * Get instructions at line.
     * @param lineNo Line number.
     * @param instruction Instruction.
     * @return Instruction, if any.
     */
    @CheckForNull
    public String getInstructionsAtLine(final int lineNo, final Instruction instruction) {
        this.ensureRead();

        if (!this.lineInstructions.containsKey(lineNo)) {
            return null;
        }

        return this.lineInstructions.get(lineNo).entrySet().stream()
            .filter(entry -> entry.getKey() == instruction)
            .map(Map.Entry::getValue)
            .findAny()
            .orElse(null);
    }

    /**
     * Get instructions for this scope. This scope only and not any of its child scopes.
     * @param scope Scope to get instructions from.
     * @param instruction Instruction.
     * @return Instructions in scope.
     */
    public Set<String> getScopeInstructions(final Scope scope, final Instruction instruction) {
        if (instruction.getSort() != Instruction.Sort.SCOPE) {
            throw new IllegalStateException("Excepted Scope instruction");
        }

        final int fromLine = scope.getStartLine();
        final int toLine = scope.getEndLine();
        return IntStream.range(fromLine, toLine)
            .filter(line ->
                // Filter any lines where a child scope lives.
                scope.getChildScopes().stream()
                    .noneMatch(childScope -> line >= childScope.getStartLine()
                                          && line < childScope.getEndLine()))
            .mapToObj(line -> this.getInstructionsAtLine(line - 1, instruction))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private void ensureRead() {
        if (this.isRead) {
            return;
        }

        final String[] lines = this.magikFile.getSourceLines();
        if (lines == null) {
            return;
        }

        this.instructions.forEach(instruction -> {
            final Pattern pattern = instruction.getPattern();
            for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
                final String line = lines[lineNo];
                final Matcher matcher = pattern.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                final Map<Instruction, String> instructionsAtLine =
                    this.lineInstructions.computeIfAbsent(lineNo, k -> new HashMap<>());
                final String readInstruction = matcher.group(1);
                instructionsAtLine.put(instruction, readInstruction);
            }
        });

        this.isRead = true;
    }

    /**
     * Parse a instruction(s) with the form: `a=b; c=d`.
     * An instruction is a key/value pair bound by a `=` character.
     * Instructions are separated with a `;`.
     * @param str Instructions to parse
     * @return Map of parsed key/value pairs.
     */
    public static Map<String, String> parseInstructions(final String str) {
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

}
