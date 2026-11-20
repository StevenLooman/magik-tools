package nl.ramsolutions.sw.magik.debugadapter.variables;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.variables.VariableManager.MagikVariable;
import org.eclipse.lsp4j.debug.VariablesArgumentsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shows any value by its format: the slots of a slotted object and the raw storage of an indexed
 * object. The {@link VariableManager} falls back to it for a value no other expander claims.
 */
class ObjectExpander implements VariableExpander {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectExpander.class);

  @Override
  public String createCondition(final String expression) {
    return "_true";
  }

  @Override
  public String createCountExpression(final String expression) {
    return null;
  }

  @Override
  public List<MagikVariable> getVariables(
      final VariableManager manager,
      final MagikVariable variable,
      final VariablesArgumentsFilter filter,
      final int start,
      final int count)
      throws IOException, InterruptedException, ExecutionException {
    final String expression = variable.getExpression();

    // Determine type:
    // - sw:enumerated_format_mixin
    // - sw:indexed_format_mixin
    // - sw:slotted_format_mixin
    final String typeExpression =
        ""
            + "_if "
            + expression
            + ".is_kind_of?(sw:enumerated_format_mixin) "
            + "_then >> :enumerated_format_mixin "
            + "_elif "
            + expression
            + ".is_kind_of?(sw:indexed_format_mixin) "
            + "_then >> :indexed_format_mixin "
            + "_elif "
            + expression
            + ".is_kind_of?(sw:slotted_format_mixin) "
            + "_then >> :slotted_format_mixin "
            + "_else >> expression.class_name "
            + "_endif";
    final String type = manager.evaluate(variable, typeExpression);
    switch (type) {
      case ":enumerated_format_mixin":
        return this.variablesFromEnumerated(variable);

      case ":indexed_format_mixin":
        return this.variablesFromIndexed(manager, variable);

      case ":slotted_format_mixin":
        final List<MagikVariable> slots = this.variablesFromSlotted(manager, variable);
        return ObjectExpander.sortByName(slots);

      default:
        LOGGER.warn("Unknown type for expression: {}, class: {}", expression, type);
        return Collections.emptyList();
    }
  }

  /**
   * Sort variables by name.
   *
   * @param magikVariables Variables to sort.
   * @return Sorted variables.
   */
  static List<MagikVariable> sortByName(final List<MagikVariable> magikVariables) {
    final Comparator<MagikVariable> byName = Comparator.comparing(MagikVariable::getName);
    return magikVariables.stream().sorted(byName).toList();
  }

  private List<MagikVariable> variablesFromSlotted(
      final VariableManager manager, final MagikVariable variable)
      throws InterruptedException, ExecutionException, IOException {
    final String expression = variable.getExpression();

    // Get slots.
    final String slotsExpression =
        ""
            + expression
            + ".sys!all_slot_names()"
            + ".map(_proc(sn) _return sn.subseq(sn.index_of(%!) + 1) _endproc)"
            + ".join_as_strings(%,)";
    final String slotNames = manager.evaluate(variable, slotsExpression);

    // For each slot, get contents
    final List<MagikVariable> magikVariables = new ArrayList<>();
    for (final String slotName : slotNames.split(",")) {
      if ("".equals(slotName)) {
        continue;
      }

      final String slotValueExpression = expression + ".sys!slot(:" + slotName + ")";
      final String slotValue = manager.evaluate(variable, slotValueExpression + ".print_string");
      final MagikVariable slotVariable =
          manager.addVariable(variable, slotName, slotValue, slotValueExpression);
      magikVariables.add(slotVariable);
    }
    return magikVariables;
  }

  private List<MagikVariable> variablesFromIndexed(
      final VariableManager manager, final MagikVariable variable)
      throws InterruptedException, ExecutionException, IOException {
    final String expression = variable.getExpression();

    // Get size.
    final String sizeExpression = expression + ".sys!size";
    final String sizeResult = manager.evaluate(variable, sizeExpression);
    final int size = Integer.parseInt(sizeResult);

    // Get contents.
    final List<MagikVariable> magikVariables = new ArrayList<>();
    for (int i = 0; i < size; ++i) {
      final String itemValueExpression = expression + ".sys!at0(" + i + ")";
      final String itemValue = manager.evaluate(variable, itemValueExpression + ".print_string");
      final String name = Integer.toString(i);
      final MagikVariable slotVariable =
          manager.addVariable(variable, name, itemValue, itemValueExpression);
      magikVariables.add(slotVariable);
    }

    return magikVariables;
  }

  @SuppressWarnings("java:S1172")
  private List<MagikVariable> variablesFromEnumerated(final MagikVariable variable) {
    return Collections.emptyList();
  }
}
