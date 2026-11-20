package nl.ramsolutions.sw.magik.debugadapter.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.Lsp4jConversion;
import nl.ramsolutions.sw.magik.debugadapter.variables.DescribingSlapProtocol.Value;
import nl.ramsolutions.sw.magik.debugadapter.variables.VariableManager.MagikVariable;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArgumentsFilter;
import org.junit.jupiter.api.Test;

/** Tests for the {@link VariableExpander}s of a {@link VariableManager}. */
@SuppressWarnings("checkstyle:MagicNumber")
class VariableExpanderTest {

  /** Expander for values of one mixin, configurable in what it reports. */
  private static final class MixinExpander implements VariableExpander {

    private final String mixin;
    private final boolean counted;
    private final int namedVariables;
    private final boolean expandable;

    MixinExpander(
        final String mixin,
        final boolean counted,
        final int namedVariables,
        final boolean expandable) {
      this.mixin = mixin;
      this.counted = counted;
      this.namedVariables = namedVariables;
      this.expandable = expandable;
    }

    @Override
    public String createCondition(final String expression) {
      return expression + ".is_kind_of?(sw:" + this.mixin + ")";
    }

    @Override
    public String createCountExpression(final String expression) {
      return this.counted ? expression + ".size" : null;
    }

    @Override
    public int getNamedVariables() {
      return this.namedVariables;
    }

    @Override
    public boolean isExpandable() {
      return this.expandable;
    }

    @Override
    public List<MagikVariable> getVariables(
        final VariableManager manager,
        final MagikVariable variable,
        final VariablesArgumentsFilter filter,
        final int start,
        final int count) {
      final String expression = variable.getExpression();
      final MagikVariable raw = manager.addRawVariable(variable, "raw", "raw value", expression);
      return List.of(raw);
    }
  }

  /** Expander whose only child is the variable itself, bound to another expander and count. */
  private static final class BindingExpander implements VariableExpander {

    private final VariableExpander childExpander;
    private final int childCount;

    BindingExpander(final VariableExpander childExpander, final int childCount) {
      this.childExpander = childExpander;
      this.childCount = childCount;
    }

    @Override
    public String createCondition(final String expression) {
      return expression + ".is_kind_of?(sw:table_mixin)";
    }

    @Override
    public String createCountExpression(final String expression) {
      return expression + ".size";
    }

    @Override
    public List<MagikVariable> getVariables(
        final VariableManager manager,
        final MagikVariable variable,
        final VariablesArgumentsFilter filter,
        final int start,
        final int count) {
      final String expression = variable.getExpression();
      final MagikVariable bound =
          manager.addBoundVariable(
              variable, "bound", "bound value", expression, this.childExpander, this.childCount);
      return List.of(bound);
    }
  }

  private List<MagikVariable> getLocals(final VariableManager manager)
      throws IOException, InterruptedException, ExecutionException {
    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(20, 0);
    final Scope[] scopes = manager.getScopes(frameId);
    final Scope scope = scopes[0];
    final int reference = scope.getVariablesReference();
    return manager.getVariables(reference, null, 0, 0);
  }

  @Test
  void testFirstExpanderWhoseConditionHoldsClaimsTheValue()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value rope = new Value(Set.of("ro_indexed_collection_mixin"), 3);
    slapProtocol.addLocal("a_rope", rope);
    final Value table = new Value(Set.of("table_mixin", "ro_indexed_collection_mixin"), 5);
    slapProtocol.addLocal("b_table", table);
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 0, true);
    final MixinExpander indexedExpander =
        new MixinExpander("ro_indexed_collection_mixin", true, 0, true);
    final List<VariableExpander> expanders = List.of(tableExpander, indexedExpander);
    final VariableManager manager = new VariableManager(slapProtocol, expanders);

    final List<MagikVariable> locals = this.getLocals(manager);

    final MagikVariable ropeVariable = locals.get(0);
    final VariableExpander ropeExpander = ropeVariable.getExpander();
    assertThat(ropeExpander).isSameAs(indexedExpander);
    final int ropeSize = ropeVariable.getIndexedVariables();
    assertThat(ropeSize).isEqualTo(3);
    final MagikVariable tableVariable = locals.get(1);
    final VariableExpander claimingExpander = tableVariable.getExpander();
    assertThat(claimingExpander).isSameAs(tableExpander);
    final int tableSize = tableVariable.getIndexedVariables();
    assertThat(tableSize).isEqualTo(5);
  }

  @Test
  void testEachVariableIsDescribedInOneEvaluation()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value rope = new Value(Set.of("ro_indexed_collection_mixin"), 3);
    slapProtocol.addLocal("a_rope", rope);
    final Value plain = new Value(Set.of(), 0);
    slapProtocol.addLocal("b_plain", plain);
    slapProtocol.addLocal("c_plain", plain);
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 0, true);
    final MixinExpander leafExpander = new MixinExpander("leaf_mixin", false, 0, false);
    final MixinExpander indexedExpander =
        new MixinExpander("ro_indexed_collection_mixin", true, 0, true);
    final List<VariableExpander> expanders = List.of(tableExpander, leafExpander, indexedExpander);
    final VariableManager manager = new VariableManager(slapProtocol, expanders);

    this.getLocals(manager);

    final List<String> expressions = slapProtocol.getExpressions();
    assertThat(expressions).hasSize(3);
  }

  @Test
  void testUnclaimedValueIsShownByItsSlots()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value plain = new Value(Set.of("slotted_format_mixin"), 0);
    slapProtocol.addLocal("plain", plain);
    slapProtocol.setSlotNames("b,a");
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 0, true);
    final List<VariableExpander> expanders = List.of(tableExpander);
    final VariableManager manager = new VariableManager(slapProtocol, expanders);
    final List<MagikVariable> locals = this.getLocals(manager);
    final MagikVariable plainVariable = locals.get(0);
    final int indexedVariables = plainVariable.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(-1);

    final int reference = plainVariable.getId();
    final List<MagikVariable> slots = manager.getVariables(reference, null, 0, 0);

    final List<String> names = slots.stream().map(MagikVariable::getName).toList();
    assertThat(names).containsExactly("a", "b");
  }

  @Test
  void testFailedDescriptionFallsBackToTheFormat()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol =
        new DescribingSlapProtocol() {
          @Override
          String answer(final String expression) {
            return expression.contains(" _then >> (") ? null : super.answer(expression);
          }
        };
    final Value table = new Value(Set.of("table_mixin"), 5);
    slapProtocol.addLocal("table", table);
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 0, true);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(tableExpander));

    final List<MagikVariable> locals = this.getLocals(manager);

    final MagikVariable tableVariable = locals.get(0);
    final VariableExpander expander = tableVariable.getExpander();
    assertThat(expander).isInstanceOf(ObjectExpander.class);
    final int indexedVariables = tableVariable.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(-1);
  }

  @Test
  void testExpanderWithoutCountGivesNoIndexedVariables()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value text = new Value(Set.of("leaf_mixin"), 3);
    slapProtocol.addLocal("text", text);
    final MixinExpander leafExpander = new MixinExpander("leaf_mixin", false, 0, false);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(leafExpander));

    final List<MagikVariable> locals = this.getLocals(manager);

    final MagikVariable textVariable = locals.get(0);
    final int indexedVariables = textVariable.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(-1);
  }

  @Test
  void testConversionGivesAValueThatCannotExpandNoReference()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value text = new Value(Set.of("leaf_mixin"), 3);
    slapProtocol.addLocal("text", text);
    final MixinExpander leafExpander = new MixinExpander("leaf_mixin", false, 0, false);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(leafExpander));
    final List<MagikVariable> locals = this.getLocals(manager);

    final Variable[] converted = Lsp4jConversion.toLsp4j(locals);

    final Variable textVariable = converted[0];
    final int reference = textVariable.getVariablesReference();
    assertThat(reference).isZero();
    final Integer indexedVariables = textVariable.getIndexedVariables();
    assertThat(indexedVariables).isNull();
  }

  @Test
  void testConversionReportsNamedVariables()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value table = new Value(Set.of("table_mixin"), 4);
    slapProtocol.addLocal("table", table);
    final Value plain = new Value(Set.of(), 0);
    slapProtocol.addLocal("x_plain", plain);
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 2, true);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(tableExpander));
    final List<MagikVariable> locals = this.getLocals(manager);

    final Variable[] converted = Lsp4jConversion.toLsp4j(locals);

    final Variable tableVariable = converted[0];
    final Integer namedVariables = tableVariable.getNamedVariables();
    assertThat(namedVariables).isEqualTo(2);
    final Integer indexedVariables = tableVariable.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(4);
    final Variable plainVariable = converted[1];
    final Integer plainNamed = plainVariable.getNamedVariables();
    assertThat(plainNamed).isNull();
    final int plainReference = plainVariable.getVariablesReference();
    assertThat(plainReference).isPositive();
  }

  @Test
  void testRawChildIsShownByItsFormatWithoutDescription()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value table = new Value(Set.of("table_mixin"), 4);
    slapProtocol.addLocal("table", table);
    final MixinExpander tableExpander = new MixinExpander("table_mixin", true, 0, true);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(tableExpander));
    final List<MagikVariable> locals = this.getLocals(manager);
    final MagikVariable tableVariable = locals.get(0);
    final int reference = tableVariable.getId();
    final List<String> expressionsBefore = List.copyOf(slapProtocol.getExpressions());

    final List<MagikVariable> children = manager.getVariables(reference, null, 0, 0);

    final MagikVariable raw = children.get(0);
    final VariableExpander rawExpander = raw.getExpander();
    assertThat(rawExpander).isInstanceOf(ObjectExpander.class);
    final String rawExpression = raw.getExpression();
    assertThat(rawExpression).isEqualTo("table");
    final List<String> expressionsAfter = slapProtocol.getExpressions();
    assertThat(expressionsAfter).isEqualTo(expressionsBefore);
  }

  @Test
  void testBoundChildHasTheGivenExpanderAndCountWithoutDescription()
      throws IOException, InterruptedException, ExecutionException {
    final DescribingSlapProtocol slapProtocol = new DescribingSlapProtocol();
    final Value table = new Value(Set.of("table_mixin"), 4);
    slapProtocol.addLocal("table", table);
    final MixinExpander childExpander =
        new MixinExpander("ro_indexed_collection_mixin", true, 0, true);
    final BindingExpander bindingExpander = new BindingExpander(childExpander, 7);
    final VariableManager manager = new VariableManager(slapProtocol, List.of(bindingExpander));
    final List<MagikVariable> locals = this.getLocals(manager);
    final MagikVariable tableVariable = locals.get(0);
    final int reference = tableVariable.getId();
    final List<String> expressionsBefore = List.copyOf(slapProtocol.getExpressions());

    final List<MagikVariable> children = manager.getVariables(reference, null, 0, 0);

    final MagikVariable bound = children.get(0);
    final VariableExpander boundExpander = bound.getExpander();
    assertThat(boundExpander).isSameAs(childExpander);
    final int indexedVariables = bound.getIndexedVariables();
    assertThat(indexedVariables).isEqualTo(7);
    final List<String> expressionsAfter = slapProtocol.getExpressions();
    assertThat(expressionsAfter).isEqualTo(expressionsBefore);
  }
}
