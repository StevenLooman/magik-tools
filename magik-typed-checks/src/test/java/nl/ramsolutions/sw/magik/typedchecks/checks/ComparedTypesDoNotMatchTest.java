package nl.ramsolutions.sw.magik.typedchecks.checks;

import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.ExemplarDefinition;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.types.TypeString;
import nl.ramsolutions.sw.magik.checks.MagikIssue;
import nl.ramsolutions.sw.magik.typedchecks.MagikTypedCheck;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ComparedTypesDoNotMatchTypedCheck}.
 */
class ComparedTypesDoNotMatchTest extends MagikTypedCheckTestBase {

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.m(param1)\n"
            + "  _if param1 _is _unset\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  _if param1.is_class_of?(sw:float)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  _if param1.is_kind_of?(sw:float)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
    })
    void testDoesNotCheckUndefined(final String code) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.m(_optional param1)\n"
            + "  ## @param {sw:integer} param1\n"  // Actually, sw:integer|sw:unset, due to being optional.
            + "  _if param1 _is _unset\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1, param2)\n"
            + "  ## @param {sw:integer|sw:float} param1\n"
            + "  ## @param {sw:symbol|sw:float} param2\n"
            + "  _if param1 _is param2\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  _if param1.is_class_of?(sw:integer)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1, param2)\n"
            + "  ## @param {sw:integer|sw:float} param1\n"
            + "  ## @param {sw:integer|sw:float} param2\n"
            + "  _if param1.is_class_of?(param2)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  ## @param {user:child} param1\n"
            + "  _if param1.is_kind_of?(user:parent)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
    })
    void testTypeMatchable(final String code) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("parent", "user"),
                Collections.emptyList(),
                Collections.emptyList()));
        definitionKeeper.add(
            new ExemplarDefinition(
                null,
                null,
                null,
                null,
                ExemplarDefinition.Sort.SLOTTED,
                TypeString.ofIdentifier("child", "user"),
                Collections.emptyList(),
                List.of(
                    TypeString.ofIdentifier("parent", "user"))));
        final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ""
            + "_method a.m(param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  _if param1 _is _unset\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1, param2)\n"
            + "  ## @param {sw:integer|sw:float} param1\n"
            + "  ## @param {sw:symbol|sw:char16_vector} param2\n"
            + "  _if param1 _is param2\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  _if param1.is_class_of?(sw:character)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
        ""
            + "_method a.m(param1)\n"
            + "  ## @param {sw:integer} param1\n"
            + "  _if param1.is_kind_of?(sw:symbol)\n"
            + "  _then\n"
            + "  _endif\n"
            + "_endmethod\n",
    })
    void testTypeNotMatchable(final String code) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
        final MagikTypedCheck check = new ComparedTypesDoNotMatchTypedCheck();
        final List<MagikIssue> checkResults = this.runCheck(code, definitionKeeper, check);
        assertThat(checkResults).hasSize(1);
    }

}
