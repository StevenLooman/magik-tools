package nl.ramsolutions.sw.magik.debugadapter.variables;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.variables.VariableManager.MagikVariable;
import org.eclipse.lsp4j.debug.VariablesArgumentsFilter;

/**
 * Shows one kind of value in the variables view. The {@link VariableManager} asks its expanders in
 * order which of them claims a variable's value, in one evaluation per variable, and the claiming
 * expander lists the variable's children.
 */
interface VariableExpander {

  /**
   * Create the Magik condition that holds when this expander claims the value.
   *
   * @param expression Expression of the variable.
   * @return Magik condition.
   */
  String createCondition(String expression);

  /**
   * Create the Magik expression answering the number of indexed children.
   *
   * @param expression Expression of the variable.
   * @return Magik expression answering an integer, or {@code null} if the value has no indexed
   *     children.
   */
  String createCountExpression(String expression);

  /**
   * Get the number of named children, shown next to the indexed children.
   *
   * @return Number of named children.
   */
  default int getNamedVariables() {
    return 0;
  }

  /**
   * Test if the value can be expanded.
   *
   * @return {@code true} if the value has children.
   */
  default boolean isExpandable() {
    return true;
  }

  /**
   * Get the children of a variable this expander claimed.
   *
   * @param manager Variable manager, to evaluate in the variable's frame and add the children.
   * @param variable Variable to expand.
   * @param filter Kind of children to return, or {@code null} for both.
   * @param start Index of the first indexed child to return.
   * @param count Number of indexed children to return, or 0 for all.
   * @return Children.
   */
  List<MagikVariable> getVariables(
      VariableManager manager,
      MagikVariable variable,
      VariablesArgumentsFilter filter,
      int start,
      int count)
      throws IOException, InterruptedException, ExecutionException;
}
