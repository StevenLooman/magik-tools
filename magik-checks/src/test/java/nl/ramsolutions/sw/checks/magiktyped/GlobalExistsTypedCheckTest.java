package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import org.junit.jupiter.api.Test;

/** Test {@link GlobalExistsTypedCheck}. */
class GlobalExistsTypedCheckTest {

  @Test
  void testKnownGlobal() {
    final String code = "float.m";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUnknownGlobal() {
    final String code = "abc.m";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testGlobalDeclaredInMethod() {
    // Compiling `_global abc` creates `abc` when no such global exists yet.
    final String code =
        """
        _method a.m()
          _global abc
          _return abc
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testGlobalDeclaredInOtherMethodOfFile() {
    final String code =
        """
        _method a.m1()
          _global abc
          _return abc
        _endmethod

        _method a.m2()
          _return abc
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testUnknownGlobalInGlobalValue() {
    final String code = "_global g << abc";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testNameAssignedInTheBodyIsNoGlobal() {
    // A name assigned anywhere in a method is a local of the method, also where it is read first.
    final String code =
        """
        _method a.m(p)
          _local r << abc
          _if p _then abc << 1 _endif
          _return r, abc
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();

    final MagikTypedCheck check = new GlobalExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
