package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import com.sonar.sslr.api.AstNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Set;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.helpers.PackageNodeHelper;
import nl.ramsolutions.sw.magik.analysis.scope.GlobalScope;
import nl.ramsolutions.sw.magik.analysis.scope.Scope;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.parser.CommentInstructionReader;
import nl.ramsolutions.sw.magik.parser.TypeStringParser;

/**
 * Reads the {@code # self: <type>} instruction of a {@code _proc}: the type {@code _self} takes
 * inside the procedure, stated on its own line in the procedure's body.
 */
public final class ProcedureSelfInstruction {

  /** The {@code # self:} scope instruction. */
  public static final CommentInstructionReader.Instruction SELF_INSTRUCTION =
      new CommentInstructionReader.Instruction(
          "self", CommentInstructionReader.Instruction.Sort.SCOPE);

  private ProcedureSelfInstruction() {}

  /**
   * Read the annotated type of {@code _self} for a procedure.
   *
   * @param magikFile File the procedure is in.
   * @param procedureNode PROCEDURE_DEFINITION node.
   * @return Annotated type, or null when the procedure is not (validly) annotated.
   */
  @CheckForNull
  public static TypeString read(final MagikFile magikFile, final AstNode procedureNode) {
    final CommentInstructionReader instructionReader =
        new CommentInstructionReader(magikFile, Set.of(ProcedureSelfInstruction.SELF_INSTRUCTION));
    final GlobalScope globalScope = magikFile.getGlobalScope();
    return ProcedureSelfInstruction.read(instructionReader, globalScope, procedureNode);
  }

  /**
   * Read the annotated type of {@code _self} for a procedure, with a reusable reader.
   *
   * @param instructionReader Reader for {@link #SELF_INSTRUCTION}.
   * @param globalScope Global scope of the file the procedure is in.
   * @param procedureNode PROCEDURE_DEFINITION node.
   * @return Annotated type, or null when the procedure is not (validly) annotated.
   */
  @CheckForNull
  public static TypeString read(
      final CommentInstructionReader instructionReader,
      final GlobalScope globalScope,
      final AstNode procedureNode) {
    final AstNode bodyNode = procedureNode.getFirstChild(MagikGrammar.BODY);
    if (bodyNode == null) {
      return null;
    }

    final Scope scope = globalScope.getScopeForNode(bodyNode);
    if (scope == null) {
      return null;
    }

    // Two differing annotations on one proc have no single answer.
    final Set<String> instructions =
        instructionReader.getScopeInstructions(scope, ProcedureSelfInstruction.SELF_INSTRUCTION);
    if (instructions.size() != 1) {
      return null;
    }

    final String instruction = instructions.iterator().next();
    final String typeStr = instruction.strip();
    final PackageNodeHelper helper = new PackageNodeHelper(procedureNode);
    final String currentPackage = helper.getCurrentPackage();
    final TypeString selfType = TypeStringParser.parseTypeString(typeStr, currentPackage);
    return selfType.isUndefined() ? null : selfType;
  }
}
