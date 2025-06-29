package nl.ramsolutions.sw.magik.formatting;

import java.lang.reflect.Constructor;
import java.util.Map;

/** Factory class for creating indent strategies. */
public class IndentStrategyFactory {

  private static final Map<String, Class<? extends IndentStrategy>> INDENT_STRATEGIES =
      Map.of(
          TabbedIndentStrategy.NAME, TabbedIndentStrategy.class,
          NullIndentStrategy.NAME, NullIndentStrategy.class);

  private IndentStrategyFactory() {}

  /**
   * Create an indent strategy based on the given options.
   *
   * @param indentStrategyName The name of the indent strategy to create.
   * @return An instance of {@link IndentStrategy}.
   */
  public static IndentStrategy createIndentStrategy(
      final String indentStrategyName, final FormattingOptions options) {
    final Class<? extends IndentStrategy> strategyClass =
        IndentStrategyFactory.INDENT_STRATEGIES.get(indentStrategyName);
    Constructor<? extends IndentStrategy> constructor;
    try {
      constructor = strategyClass.getDeclaredConstructor(FormattingOptions.class);
      return constructor.newInstance(options);
    } catch (final ReflectiveOperationException e) {
      throw new IllegalArgumentException("Invalid indent strategy: " + indentStrategyName, e);
    }
  }
}
