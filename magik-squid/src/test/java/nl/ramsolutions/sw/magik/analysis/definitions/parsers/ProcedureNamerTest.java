package nl.ramsolutions.sw.magik.analysis.definitions.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonar.sslr.api.AstNode;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import org.junit.jupiter.api.Test;

/** Test {@link ProcedureNamer}. */
class ProcedureNamerTest {

  private AstNode createProcedureNode(final String code) {
    final MagikFile magikFile = new MagikFile(MagikFile.DEFAULT_URI, code);
    final AstNode topNode = magikFile.getTopNode();
    return topNode.getFirstDescendant(MagikGrammar.PROCEDURE_DEFINITION);
  }

  @Test
  void testNameForTopLevelGlobalProcedure() {
    final String code =
        """
        _package user
        _global my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final TypeString expected = TypeString.ofIdentifier("my_proc", "user");
    assertThat(typeString).isEqualTo(expected);
  }

  @Test
  void testNameForTopLevelAssignedProcedure() {
    final String code =
        """
        _package user
        my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final TypeString expected = TypeString.ofIdentifier("my_proc", "user");
    assertThat(typeString).isEqualTo(expected);
  }

  @Test
  void testNameForArgumentProcedureIsAnonymous() {
    final String code =
        """
        _package user
        define_binary_operator_case(:|>|, integer, float, _proc(a, b) _endproc)
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForReturnedProcedureIsAnonymous() {
    final String code =
        """
        _package user
        _method object.make_proc
          _return _proc() _endproc
        _endmethod
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForTopLevelLocalProcedureIsAnonymous() {
    // A top level `_local` is file scoped: it is not reachable by that name from anywhere else.
    final String code =
        """
        _package user
        _local my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForNestedGlobalProcedureIsAnonymous() {
    final String code =
        """
        _package user
        _block
          _global my_proc << _proc() _endproc
        _endblock
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForChainedAssignmentIsAnonymous() {
    final String code =
        """
        _package user
        a << b << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForSlotAssignmentIsAnonymous() {
    final String code =
        """
        _package user
        _method object.init
          .callback << _proc() _endproc
        _endmethod
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForTopLevelLocalDeclaredThenAssignedProcedureIsAnonymous() {
    // `_local` declared on one line, assigned on the next — file scoped, not reachable by name.
    final String code =
        """
        _package user
        _local my_proc
        my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForTopLevelMultiVarLocalDeclaredProcedureIsAnonymous() {
    // `_local a, my_proc` declares my_proc non-globally; the assignment must stay anonymous.
    final String code =
        """
        _package user
        _local a, my_proc
        my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForTopLevelDeclaredAfterAssignmentProcedureIsAnonymous() {
    // Declaration order is irrelevant; a _local after the assignment still scopes it to the file.
    final String code =
        """
        _package user
        my_proc << _proc() _endproc
        _local my_proc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final boolean anonymous = typeString.isAnonymous();
    assertThat(anonymous).isTrue();
  }

  @Test
  void testNameForTopLevelGlobalDeclaredThenAssignedProcedure() {
    // `_global` declared on one line, assigned on the next — still globally reachable.
    final String code =
        """
        _package user
        _global my_proc
        my_proc << _proc() _endproc
        """;
    final AstNode procedureNode = this.createProcedureNode(code);
    final TypeString typeString = ProcedureNamer.getNameForProcedure(procedureNode);
    final TypeString expected = TypeString.ofIdentifier("my_proc", "user");
    assertThat(typeString).isEqualTo(expected);
  }
}
