package nl.ramsolutions.sw.checks.magiktyped;

import static nl.ramsolutions.sw.checks.magiktyped.MagikTypedCheckAssert.assertThat;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.checks.MagikTypedCheck;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.MethodDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;
import nl.ramsolutions.sw.magik.analysis.typing.TypeString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link MethodIsPublicTypedCheck}. */
class MethodIsPublicTypedCheckTest {

  @Test
  void testMethodIsPublic() {
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
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          object.m(object, object)
        _endblock
        """;
    final MagikTypedCheck check = new MethodIsPublicTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }

  @Test
  void testMethodIsPrivate() {
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
            Set.of(MethodDefinition.Modifier.PRIVATE),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final String code =
        """
        _block
          object.m(object, object)
        _endblock
        """;
    final MagikTypedCheck check = new MethodIsPublicTypedCheck();
    assertThat(check).reportsIssueCount(code, definitionKeeper, 1);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        _method rope.m()
          _self.m()
        _endmethod
        """,
        """
        _method rope.m()
          _super(object).m()
        _endmethod
        """
      })
  void testMethodIsPrivateOnSelfOrSuper(final String code) {
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    definitionKeeper.add(
        new ExemplarDefinition(
            null,
            null,
            null,
            null,
            null,
            ExemplarDefinition.Sort.SLOTTED,
            TypeString.ofIdentifier("rope", "sw"),
            Collections.emptyList(),
            List.of(TypeString.SW_OBJECT),
            null));
    definitionKeeper.add(
        new MethodDefinition(
            null,
            null,
            null,
            null,
            null,
            TypeString.SW_OBJECT,
            "m()",
            Set.of(MethodDefinition.Modifier.PRIVATE),
            Collections.emptyList(),
            null,
            null,
            ExpressionResultString.EMPTY,
            ExpressionResultString.EMPTY));

    final MagikTypedCheck check = new MethodIsPublicTypedCheck();
    assertThat(check).reportsNoIssues(code, definitionKeeper);
  }
}
