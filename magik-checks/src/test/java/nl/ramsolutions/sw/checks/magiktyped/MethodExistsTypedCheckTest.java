package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.InheritanceDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.PackageDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Test {@link MethodExistsTypedCheck}. */
class MethodExistsTypedCheckTest {

  @Test
  void testMethodUnknown() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMethodKnown() {
    final String code =
        """
        _block
          object.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodOnExemplarShadowingSameNameInUsedPackage() {
    // A reference to an exemplar defined in the current package must resolve to that single
    // exemplar, even when a same-named (e.g. stale) exemplar exists in a used package. Otherwise
    // the reference resolves to a union and the check false-positives on the unrelated member.
    final String code =
        """
        _package rs
        _block
          foo.m()
        _endblock""";
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(new PackageDefinition(null, null, null, null, null, "rs", List.of("sw")));

    // m() responds via the parent chain (sw:object).
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SW_OBJECT),
            ExpressionResultString.EMPTY));

    // The real exemplar in package rs, inheriting from sw:object.
    final TypeString rsFoo = TypeString.ofIdentifier("foo", "rs");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.SLOTTED, rsFoo, null));
    definitionKeeper.add(
        new InheritanceDefinition(null, null, null, null, null, rsFoo, TypeString.SW_OBJECT));

    // A same-named phantom in the used package sw, with no useful parents.
    final TypeString swFoo = TypeString.ofIdentifier("foo", "sw");
    definitionKeeper.add(
        new ExemplarDefinition(
            null, null, null, null, null, ExemplarDefinition.Sort.UNDEFINED, swFoo, null));

    final MagikTypedCheck check = new MethodExistsTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
