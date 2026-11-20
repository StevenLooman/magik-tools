package nl.ramsolutions.sw.magik.debugadapter;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapErrorException;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import nl.ramsolutions.sw.magik.debugadapter.source.AgentSourceModule;
import nl.ramsolutions.sw.magik.debugadapter.source.SourceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves the package-qualified method name and the source file of a stack frame. */
class FrameSourceResolver {

  static final String LANGUAGE_MAGIK = "Magik";
  static final String LOOPBODY = "<loopbody>";
  static final String UNNAMED_PROC = "<unnamed proc>()";

  private static final Logger LOGGER = LoggerFactory.getLogger(FrameSourceResolver.class);
  private static final String EVAL_EXEMPLAR_PACKAGE =
      "_self.define_method_target.meta_at(:exemplar_global).package.association_at(%s).package.name.write_string";

  private final ISlapProtocol slapProtocol;
  private final PathMapper pathMapper;
  private final List<SourceModule> modules;

  /**
   * Constructor.
   *
   * @param slapProtocol SLAP protocol.
   * @param pathMapper Path mapping.
   */
  FrameSourceResolver(final ISlapProtocol slapProtocol, final PathMapper pathMapper) {
    this.slapProtocol = slapProtocol;
    this.pathMapper = pathMapper;
    this.modules = this.createModules();
  }

  /**
   * Create the ordered source modules. Override to add or remove source modules.
   *
   * @return Ordered modules.
   */
  protected List<SourceModule> createModules() {
    return List.of(new AgentSourceModule(this.slapProtocol, this.pathMapper));
  }

  /**
   * Determine the source file of a stack frame.
   *
   * @param threadId Thread ID.
   * @param stackElement Stack frame, its name gets the package.
   * @return Mapped path to the source file, or null if it is unknown.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  @CheckForNull
  Path determinePath(final long threadId, final ThreadStackResponse.StackElement stackElement)
      throws IOException, InterruptedException, ExecutionException {
    final String methodName = this.qualifyMethodName(threadId, stackElement);
    if (methodName == null) {
      return null;
    }

    return this.determinePath(methodName);
  }

  /**
   * Determine the package-qualified name of the method of a stack frame, without spaces.
   *
   * @param threadId Thread ID.
   * @param stackElement Stack frame, its name gets the package.
   * @return Method name, or null for a loop body, an unnamed procedure or an unknown method.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  @CheckForNull
  String qualifyMethodName(final long threadId, final ThreadStackResponse.StackElement stackElement)
      throws IOException, InterruptedException, ExecutionException {
    String method = stackElement.getName();
    if (method.equals(UNNAMED_PROC) || method.equals(LOOPBODY)) {
      return null;
    }

    final int indexDot = method.indexOf('.');
    final int indexBracket = method.indexOf('[');
    final int index = indexDot != -1 ? indexDot + 1 : indexBracket;
    if (!method.contains(":") && index != -1) {
      // Do some extra work to determine package.
      final int level = stackElement.getLevel();
      final String exemplarName = ":|" + method.substring(0, index - 1) + "|";
      final String expr = EVAL_EXEMPLAR_PACKAGE.formatted(exemplarName);
      LOGGER.debug("Eval expression: '{}'", expr);
      try {
        final CompletableFuture<ISlapResponse> evalFuture =
            this.slapProtocol.evaluate(threadId, level, expr);
        final EvalResponse eval = (EvalResponse) evalFuture.get();
        method = eval.getResult() + ":" + method;
      } catch (final ExecutionException exception) {
        SlapErrorException.rethrowUnlessMethodNotFound(exception);
        return null;
      }

      // Bonus: update exemplar name with package.
      stackElement.setName(method);
    }

    // Clear any spaces (before `<<`/`^<<`). Lazy approach...
    return method.replace(" ", "");
  }

  /**
   * Determine the source file of a method.
   *
   * @param methodName Package-qualified method name.
   * @return Mapped path to the source file, or null if the method is unknown.
   * @throws IOException -
   * @throws InterruptedException -
   * @throws ExecutionException -
   */
  @CheckForNull
  Path determinePath(final String methodName)
      throws IOException, InterruptedException, ExecutionException {
    for (final SourceModule module : this.modules) {
      final Optional<Path> path = module.tryResolve(methodName);
      if (path.isPresent()) {
        return path.get();
      }
    }

    return null;
  }
}
