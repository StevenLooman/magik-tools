package nl.ramsolutions.sw.magik.parser;

import com.sonar.sslr.api.AstNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.CheckForNull;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;

/**
 * Comment Instruction reader.
 */
public class CommentInstructionReader {

    /**
     * Instruction type.
     */
    public static final class InstructionType {

        private final String type;
        private final boolean isScopeInstruction;
        private final Pattern pattern;

        private InstructionType(final String type, final boolean isScopeInstruction) {
            this.type = type;
            this.isScopeInstruction = isScopeInstruction;
            this.pattern = isScopeInstruction
                ? Pattern.compile("^\\s*#[ \t]*" + Pattern.quote(type) + ": ?(.*)")
                : Pattern.compile(".*#[ \t]*" + Pattern.quote(type) + ": ?(.*)");
        }

        public String getType() {
            return this.type;
        }

        public boolean isScopeInstruction() {
            return this.isScopeInstruction;
        }

        public Pattern getPattern() {
            return this.pattern;
        }

        /**
         * Create a new `InstructionType`.
         * @param type Name of type.
         * @return New `InstructionType`.
         */
        public static InstructionType createInstructionType(final String type) {
            return new InstructionType(type, false);
        }

        /**
         * Create a new `InstructionType` which only matches in a scope/on its own line.
         * To be used, for example, to specify instructions which appyl to the whole scope.
         * @param type Name of type.
         * @return New `InstructionType`.
         */
        public static InstructionType createScopeInstructionType(final String type) {
            return new InstructionType(type, true);
        }

    }

    private final MagikFile magikFile;
    private final Set<InstructionType> instructionTypes;
    private final Map<Integer, Map<InstructionType, String>> instructions = new HashMap<>();
    private boolean isRead;

    /**
     * Constructor.
     * @param magikFile Magik file.
     * @param instructionTypes Instrunction types to read.
     */
    public CommentInstructionReader(final MagikFile magikFile, final Set<InstructionType> instructionTypes) {
        this.magikFile = magikFile;
        this.instructionTypes = instructionTypes;
    }

    /**
     * Get instruction for node.
     * Simply takes line number from node and gets instruction(s) at line.
     * @param node Node.
     * @param instructionType Instruction type.
     * @return Instruction, if any.
     */
    @CheckForNull
    public String getInstructionForNode(final AstNode node, final InstructionType instructionType) {
        final int lineNo = node.getTokenLine();
        return this.getInstructionsAtLine(lineNo, instructionType);
    }

    /**
     * Get instructions at line.
     * @param lineNo Line number.
     * @param instructionType Instruction type.
     * @return Instruction, if any.
     */
    @CheckForNull
    public String getInstructionsAtLine(final int lineNo, final InstructionType instructionType) {
        this.ensureRead();

        if (!this.instructions.containsKey(lineNo)) {
            return null;
        }

        return this.instructions.get(lineNo).entrySet().stream()
            .filter(entry -> entry.getKey() == instructionType)
            .map(Map.Entry::getValue)
            .findAny()
            .orElse(null);
    }

    /**
     * Get instructions for this scope.
     * @param scope Scope to get instructions from.
     * @param instructionType Instruction type.
     * @return Instructions in scope.
     */
    public Set<String> getScopeInstructions(final Scope scope, final InstructionType instructionType) {
        if (!instructionType.isScopeInstruction()) {
            throw new IllegalStateException("Excepted scope instruction");
        }

        final int fromLine = scope.getStartLine();
        final int toLine = scope.getEndLine();
        return IntStream.range(fromLine, toLine)
            .mapToObj(line -> this.getInstructionsAtLine(line, instructionType))
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

        this.instructionTypes.forEach(instructionType -> {
            final Pattern pattern = instructionType.getPattern();
            for (int lineNo = 0; lineNo < lines.length; ++lineNo) {
                final String line = lines[lineNo];
                final Matcher matcher = pattern.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                final Map<InstructionType, String> instructionsAtLine =
                    this.instructions.computeIfAbsent(lineNo + 1, k -> new HashMap<>());
                final String instruction = matcher.group(1);
                instructionsAtLine.put(instructionType, instruction);
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
