package nl.ramsolutions.sw.magik.analysis.definitions;

import java.util.List;
import nl.ramsolutions.sw.IDefinition;
import nl.ramsolutions.sw.magik.analysis.typing.ExpressionResultString;

/** Callable/invokable definition. */
public interface ICallableDefinition extends IDefinition {

  /**
   * Get parameters.
   *
   * @return Parameters.
   */
  List<ParameterDefinition> getParameters();

  /**
   * Get return types.
   *
   * @return Return types.
   */
  ExpressionResultString getReturnTypes();

  /**
   * Get loop types.
   *
   * @return Loop types.
   */
  ExpressionResultString getLoopTypes();
}
