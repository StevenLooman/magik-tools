package nl.ramsolutions.sw.magik.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import org.junit.jupiter.api.Test;

/** Tests for CommentInstructionReader. */
class CommentInstructionReaderTest {

  private static final URI DEFAULT_URI = URI.create("tests://unittest");

  private static final String NAME_MLINT = "mlint";
  private static final CommentInstructionReader.Instruction MLINT_STATEMENT_INSTRUCTION =
      new CommentInstructionReader.Instruction(
          NAME_MLINT, CommentInstructionReader.Instruction.Sort.STATEMENT);
  private static final CommentInstructionReader.Instruction MLINT_SCOPE_INSTRUCTION =
      new CommentInstructionReader.Instruction(
          NAME_MLINT, CommentInstructionReader.Instruction.Sort.SCOPE);

  @Test
  void testReadStatementInstruction() {
    final String code =
        """
        _proc()
          print(10)  # mlint: disable=forbidden-call
        _endproc""";
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final CommentInstructionReader instructionReader =
        new CommentInstructionReader(magikFile, Set.of(MLINT_STATEMENT_INSTRUCTION));

    final String instructionAtLine1 =
        instructionReader.getInstructionsAtLine(1, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine1).isEqualTo("disable=forbidden-call");

    // Cannot read instruction via scope.
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Scope scope = globalScope.getChildScopes().get(0);
    final Set<String> scopeInstructions =
        instructionReader.getScopeInstructions(scope, MLINT_SCOPE_INSTRUCTION);
    assertThat(scopeInstructions).isEmpty();
  }

  @Test
  void testReadScopeInstruction() {
    final String code =
        """
        # mlint: disable=file-method-count
        _proc()
          # mlint: disable=no-self-use
          print(10, 20)  # mlint: disable=forbidden-call
          _block
            show(:a, :b, :c)
          _endblock
        _endproc""";
    final MagikFile magikFile = new MagikFile(DEFAULT_URI, code);

    final CommentInstructionReader instructionReader =
        new CommentInstructionReader(magikFile, Set.of(MLINT_SCOPE_INSTRUCTION));

    // Read no instruction in global scope.
    final GlobalScope globalScope = magikFile.getGlobalScope();
    final Set<String> globalScopeInstructions =
        instructionReader.getScopeInstructions(globalScope, MLINT_SCOPE_INSTRUCTION);
    assertThat(globalScopeInstructions).containsOnly("disable=file-method-count");

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
    final String instructionAtLine0 =
        instructionReader.getInstructionsAtLine(0, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine0).isNull();

    // No line instruction.
    final String instructionAtLine1 =
        instructionReader.getInstructionsAtLine(1, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine1).isNull();

    // No line instruction; Line instruction does not match the scope instruction.
    final String instructionAtLine2 =
        instructionReader.getInstructionsAtLine(2, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine2).isNull();

    // No line instruction; Statement instruction does not match scope instruction.
    final String instructionAtLine3 =
        instructionReader.getInstructionsAtLine(3, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine3).isNull();

    // No line instruction.
    final String instructionAtLine4 =
        instructionReader.getInstructionsAtLine(4, MLINT_STATEMENT_INSTRUCTION);
    assertThat(instructionAtLine4).isNull();
  }
}
