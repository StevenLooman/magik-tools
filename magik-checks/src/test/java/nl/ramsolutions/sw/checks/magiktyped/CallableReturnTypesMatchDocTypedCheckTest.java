package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;

/** Tests for {@link CallableReturnTypesMatchDocTypedCheck}. */
class CallableReturnTypesMatchDocTypedCheckTest {

  @Test
  void testTypesMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testNoReturnAndNoDoc() {
    final String code =
        """
        _method a.b
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testIgnoreAbstractMethod() {
    final String code =
        """
        _abstract _method a.b
          ## @return {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testTypesDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMissingTypeDoc() {
    final String code =
        """
        _method a.b
          _return 1
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testUnexpectedReturnDoc() {
    final String code =
        """
        _method a.b
          ## @return {integer}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesMatch() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          ## @return {symbol}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMultipleReturnValuesMissingTypeDoc() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesTypeDiffer() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          ## @return {float}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleReturnValuesOrderMismatch() {
    final String code =
        """
        _method a.b
          ## @return {symbol}
          ## @return {integer}
          _return 1, :a
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }

  @Test
  void testNoReturnDocAndUndefinedMethodInvocationReturn() {
    final String code =
        """
        _method a.b
          _return object.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    // Deferred to UndefinedMethodCallResultTypedCheck; nothing actionable here.
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testReturnDocAndUndefinedMethodInvocationReturn() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          _return object.m()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMissingTypeDocForUndefinedReturnedVariable() {
    final String code =
        """
        _method a.b
          _local value << object.m()
          _return value
        _endmethod
        """;
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
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testMultipleUndefinedReturnEntriesDoNotExplode() {
    final String code =
        """
        _method a.b
          _local value << object.m()
          _return value, :done
        _endmethod
        """;
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
            ExpressionResultString.UNDEFINED,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 2);
  }

  @Test
  void testProcedureReturnTypesMatch() {
    final String code =
        """
        _proc()
          ## @return {integer}
          _return 1
        _endproc
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testProcedureReturnTypesDiffer() {
    final String code =
        """
        _proc()
          ## @return {float}
          _return 1
        _endproc
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testEmitReturnTypeMatches() {
    final String code =
        """
        _method a.b
          ## @return {integer}
          >> 1
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testEmitReturnTypeDiffer() {
    final String code =
        """
        _method a.b
          ## @return {float}
          >> 1
        _endmethod
        """;

    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @Test
  void testAbstractMethodWithVariadicReturnDoc() {
    // Abstract methods are exempt — variadic @return on an abstract method must
    // not trigger this check.
    final String code =
        """
        _abstract _method a.b
          ## @return {sw:integer...}
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testVariadicDocMatchesVariadicReasonedReturn() {
    // Method delegates to another method whose return is declared variadic.
    // Both doc and reasoned result end up as variadic(sw:integer). No mismatch.
    final String code =
        """
        _method a.b
          ## @return {sw:integer...}
          _return _self.scatter_ints()
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString aRef = TypeString.ofIdentifier("a", "user");
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            aRef,
            "scatter_ints()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.ofVariadic(TypeString.SW_INTEGER)),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testLeadingPlusVariadicDocWithScatterReturn() {
    final String code =
        """
        _method object.test
          ## @return {sw:symbol}
          ## @return {sw:integer...}
          _local aaa << sw:rope.new_with(1, 2)  # type: sw:rope<E=sw:integer>
          _return :aaa, _scatter aaa
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final TypeString ropeRef = TypeString.ofIdentifier("rope", "sw");
    final TypeString basicCollectionMixinRef =
        TypeString.ofIdentifier("basic_collection_mixin", "sw");
    definitionKeeper.add(
        new nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition.Sort.INTRINSIC,
            basicCollectionMixinRef,
            Collections.emptyList(),
            Collections.emptyList(),
            null));
    definitionKeeper.add(
        new nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition.Sort.SLOTTED,
            ropeRef,
            Collections.emptyList(),
            java.util.List.of(basicCollectionMixinRef),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            ropeRef,
            "new_with()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.SELF),
            ExpressionResultString.EMPTY));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            basicCollectionMixinRef,
            "for_scatter()",
            EnumSet.noneOf(MethodDefinition.Modifier.class),
            Collections.emptyList(),
            null,
            null,
            new ExpressionResultString(TypeString.ofVariadic(TypeString.ofGenericReference("E"))),
            ExpressionResultString.EMPTY));
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testVariadicDocVsLiteralReturnCount() {
    final String code =
        """
        _method a.b
          ## @return {sw:integer...}
          _return 1, 2, 3
        _endmethod
        """;
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    final MagikTypedCheck check = new CallableReturnTypesMatchDocTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
