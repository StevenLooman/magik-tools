package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.List;

/**
 * The number of arguments a callable accepts: at least {@code required}, at most {@code accepted},
 * or any number beyond {@code required} when it {@code gathers}.
 *
 * @param required Number of required parameters.
 * @param accepted Number of required and optional parameters.
 * @param gathers Whether a {@code _gather} parameter takes any further arguments.
 */
public record ArgumentRange(int required, int accepted, boolean gathers) {

  /**
   * Get the {@link ArgumentRange} of a parameter list.
   *
   * @param parameters Parameters of the callable.
   * @return Range of accepted argument counts.
   */
  public static ArgumentRange of(final List<ParameterDefinition> parameters) {
    final int required =
        (int) parameters.stream().filter(ArgumentRange::isRequiredParameter).count();
    final int accepted =
        (int) parameters.stream().filter(ArgumentRange::isPositionalParameter).count();
    final boolean gathers = parameters.stream().anyMatch(ArgumentRange::isGatherParameter);
    return new ArgumentRange(required, accepted, gathers);
  }

  private static boolean isRequiredParameter(final ParameterDefinition parameter) {
    return parameter.getModifier() == ParameterDefinition.Modifier.NONE;
  }

  private static boolean isPositionalParameter(final ParameterDefinition parameter) {
    return parameter.getModifier() != ParameterDefinition.Modifier.GATHER;
  }

  private static boolean isGatherParameter(final ParameterDefinition parameter) {
    return parameter.getModifier() == ParameterDefinition.Modifier.GATHER;
  }
}
