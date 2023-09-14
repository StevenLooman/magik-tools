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
class CommentInstructionReaderReaderTest {

    private static final CommentInstructionReader.InstructionType MLINT_LINE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createStatementInstructionType("mlint");
    private static final CommentInstructionReader.InstructionType MLINT_SCOPE_INSTRUCTION =
        CommentInstructionReader.InstructionType.createScopeInstructionType("mlint");

    @Test
    void testReadStatementInstruction() {
        final String code = ""
            + "_proc()\n"
            + "  print(10)  # mlint: disable=forbidden-call\n"
            + "_endproc";
        final MagikFile magikFile = new MagikFile("tests://unittest", code);

        final CommentInstructionReader instructionReader =
            new CommentInstructionReader(magikFile, Set.of(MLINT_LINE_INSTRUCTION));

        final String instructionAtLine =
            instructionReader.getInstructionsAtLine(2, MLINT_LINE_INSTRUCTION);
        assertThat(instructionAtLine).isEqualTo("disable=forbidden-call");

        // Cannot read instruction via scope.
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Scope scope = globalScope.getChildScopes().get(0);
        final Set<String> scopeInstructions = instructionReader.getScopeInstructions(scope, MLINT_SCOPE_INSTRUCTION);
        assertThat(scopeInstructions).isEmpty();
    }

    @Test
    void testReadScopeInstruction() {
        final String code = ""
            + "_proc()\n"
            + "  # mlint: disable=no-self-use\n"
            + "  print(10, 20)  # mlint: disable=forbidden-call\n"
            + "  _block\n"
            + "    show(:a, :b, :c)\n"
            + "  _endblock\n"
            + "_endproc";
        final MagikFile magikFile = new MagikFile("tests://unittest", code);

        final CommentInstructionReader instructionReader =
            new CommentInstructionReader(magikFile, Set.of(MLINT_LINE_INSTRUCTION, MLINT_SCOPE_INSTRUCTION));

        // Read no instruction in global scope.
        final GlobalScope globalScope = magikFile.getGlobalScope();
        final Set<String> globalScopeInstructions =
            instructionReader.getScopeInstructions(globalScope, MLINT_SCOPE_INSTRUCTION);
        assertThat(globalScopeInstructions).isEmpty();

        // Read the scope instruction in proc scope.
        final Scope procScope = globalScope.getChildScopes().get(0);
        final Set<String> procScopeInstructions =
            instructionReader.getScopeInstructions(procScope, MLINT_SCOPE_INSTRUCTION);
        assertThat(procScopeInstructions).containsOnly("disable=no-self-use");

        // Read the scope instruction in block scope.
        final Scope blockScope = procScope.getChildScopes().get(0);
        final Set<String> blockScopeInstructions =
            instructionReader.getScopeInstructions(blockScope, MLINT_SCOPE_INSTRUCTION);
        assertThat(blockScopeInstructions).isEmpty();

        // No line instruction.
        final String instructionAtLine1 = instructionReader.getInstructionsAtLine(1, MLINT_LINE_INSTRUCTION);
        assertThat(instructionAtLine1).isNull();

        // No line instruction; Line instruction does not match the scope instruction.
        final String instructionAtLine2 = instructionReader.getInstructionsAtLine(2, MLINT_LINE_INSTRUCTION);
        assertThat(instructionAtLine2).isNull();

        // Read line instruction.
        final String instructionAtLine3 = instructionReader.getInstructionsAtLine(3, MLINT_LINE_INSTRUCTION);
        assertThat(instructionAtLine3).isEqualTo("disable=forbidden-call");

        // No line instruction.
        final String instructionAtLine4 = instructionReader.getInstructionsAtLine(4, MLINT_LINE_INSTRUCTION);
        assertThat(instructionAtLine4).isNull();
    }

}
