package nl.ramsolutions.sw.magik.debugadapter.evaluate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;

/** Answers evaluate requests, by asking the modules in order. */
public class EvaluateManager {

  private final ISlapProtocol slapProtocol;
  private final List<EvaluateModule> modules;

  /**
   * Constructor.
   *
   * @param slapProtocol Protocol to the agent.
   */
  public EvaluateManager(final ISlapProtocol slapProtocol) {
    this.slapProtocol = slapProtocol;
    this.modules = this.createModules();
  }

  /**
   * Create the ordered evaluate modules. Override to add or remove evaluate modules.
   *
   * @return Ordered modules.
   */
  protected List<EvaluateModule> createModules() {
    return List.of(new FrameEvaluateModule(this.slapProtocol));
  }

  /**
   * Evaluate an expression. The first module that answers wins.
   *
   * @param context Request.
   * @return Result of the evaluation.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  public String evaluate(final EvaluateContext context)
      throws IOException, InterruptedException, ExecutionException {
    for (final EvaluateModule module : this.modules) {
      final Optional<String> result = module.tryEvaluate(context);
      if (result.isPresent()) {
        return result.get();
      }
    }

    throw new IllegalStateException("No evaluate module answered the request");
  }
}
