package nl.ramsolutions.sw.magik.parser;

import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for InstructionReader.
 */
public class CommentInstructionReaderReaderTest {

    private static final CommentInstructionReader.InstructionType STATEMENT_INSTRUCTION_TYPE =
        CommentInstructionReader.InstructionType.createInstructionType("mlint");
    private static final CommentInstructionReader.InstructionType SCOPE_INSTRUCTION_TYPE =
        CommentInstructionReader.InstructionType.createScopeInstructionType("mlint");

    @Test
    void testReadInstruction() {
        final String code = ""
            + "_proc()\n"
            + "  print(10)  # mlint: disable=forbidden-call\n"
            + "_endproc";
        final MagikFile magikFile = new MagikFile("tests://unittest", code);

        final CommentInstructionReader instructionReader =
            new CommentInstructionReader(magikFile, Set.of(STATEMENT_INSTRUCTION_TYPE));

        final String instructionAtLine =
            instructionReader.getInstructionsAtLine(2, STATEMENT_INSTRUCTION_TYPE);
        assertThat(instructionAtLine).isEqualTo("disable=forbidden-call");

        // Cannot read instruction via scope.
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForLineColumn(1, 99);
        final Set<String> scopeInstructions = instructionReader.getScopeInstructions(scope, SCOPE_INSTRUCTION_TYPE);
        assertThat(scopeInstructions).isEmpty();
    }

    @Test
    void testReadScopeInstruction() {
        final String code = ""
            + "_proc()\n"
            + "  # mlint: disable=no-self-use\n"
            + "  print(10)  # mlint: disable=forbidden-call\n"
            + "_endproc";
        final MagikFile magikFile = new MagikFile("tests://unittest", code);

        final CommentInstructionReader instructionReader =
            new CommentInstructionReader(magikFile, Set.of(SCOPE_INSTRUCTION_TYPE));

        // Can read a scope/single line instruction at the specific line.
        final String instructionAtLine2 = instructionReader.getInstructionsAtLine(2, SCOPE_INSTRUCTION_TYPE);
        assertThat(instructionAtLine2).isEqualTo("disable=no-self-use");

        // Can read instruction via scope.
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope scope = globalScope.getScopeForLineColumn(1, 99);
        final Set<String> scopeInstructions = instructionReader.getScopeInstructions(scope, SCOPE_INSTRUCTION_TYPE);
        assertThat(scopeInstructions).containsOnly("disable=no-self-use");

        // Does not match the statement instruction.
        final String instructionAtLine3 = instructionReader.getInstructionsAtLine(3, STATEMENT_INSTRUCTION_TYPE);
        assertThat(instructionAtLine3).isNull();
    }

}
