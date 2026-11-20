package nl.ramsolutions.sw.magik.debugadapter.evaluate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.debugadapter.Lsp4jConversion;
import nl.ramsolutions.sw.magik.debugadapter.TestSlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.junit.jupiter.api.Test;

/** Tests for {@link EvaluateManager}. */
class EvaluateManagerTest {

  /** Answers every evaluate with a fixed result, and records the calls. */
  private static final class RecordingSlapProtocol extends TestSlapProtocol {

    private final List<String> evaluations = new ArrayList<>();

    @Override
    public CompletableFuture<ISlapResponse> evaluate(
        final long threadId, final int level, final String expression) throws IOException {
      this.evaluations.add("evaluate(" + threadId + ", " + level + ", \"" + expression + "\")");
      final EvalResponse response = new EvalResponse("3");
      return CompletableFuture.completedFuture(response);
    }

    private List<String> getEvaluationsForTest() {
      return this.evaluations;
    }
  }

  @Test
  void testEvaluateInFrameAnswersTheAgentsResult() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final EvaluateManager manager = new EvaluateManager(slapProtocol);

    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(7, 2);
    final EvaluateContext context = new EvaluateContext(frameId, "x + 1", "watch");
    final String result = manager.evaluate(context);

    assertThat(result).isEqualTo("3");
    final List<String> evaluations = slapProtocol.getEvaluationsForTest();
    assertThat(evaluations).containsExactly("evaluate(7, 2, \"x + 1\")");
  }

  @Test
  void testEvaluateWithoutFrameFailsWithTheSelectFrameMessage() {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final EvaluateManager manager = new EvaluateManager(slapProtocol);

    final EvaluateContext context = new EvaluateContext(null, "1 + 1", null);
    final Throwable thrown = catchThrowable(() -> manager.evaluate(context));

    assertThat(thrown).isInstanceOf(ResponseErrorException.class);
    final String message = thrown.getMessage();
    assertThat(message)
        .isEqualTo("Select a stack frame of a stopped thread to evaluate an expression.");
    final List<String> evaluations = slapProtocol.getEvaluationsForTest();
    assertThat(evaluations).isEmpty();
  }

  @Test
  void testFirstModuleThatAnswersWins() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final EvaluateModule firstModule = context -> Optional.of("first");
    final EvaluateManager manager =
        new EvaluateManager(slapProtocol) {
          @Override
          protected List<EvaluateModule> createModules() {
            final List<EvaluateModule> basicModules = super.createModules();
            final List<EvaluateModule> modules = new ArrayList<>();
            modules.add(firstModule);
            modules.addAll(basicModules);
            return modules;
          }
        };

    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(7, 2);
    final EvaluateContext context = new EvaluateContext(frameId, "x + 1", null);
    final String result = manager.evaluate(context);

    assertThat(result).isEqualTo("first");
    final List<String> evaluations = slapProtocol.getEvaluationsForTest();
    assertThat(evaluations).isEmpty();
  }

  @Test
  void testModuleThatDeclinesFallsThroughToTheNext() throws Exception {
    final RecordingSlapProtocol slapProtocol = new RecordingSlapProtocol();
    final EvaluateModule decliningModule = context -> Optional.empty();
    final EvaluateManager manager =
        new EvaluateManager(slapProtocol) {
          @Override
          protected List<EvaluateModule> createModules() {
            final List<EvaluateModule> basicModules = super.createModules();
            final List<EvaluateModule> modules = new ArrayList<>();
            modules.add(decliningModule);
            modules.addAll(basicModules);
            return modules;
          }
        };

    final int frameId = Lsp4jConversion.threadIdLevelToFrameId(7, 2);
    final EvaluateContext context = new EvaluateContext(frameId, "x + 1", null);
    final String result = manager.evaluate(context);

    assertThat(result).isEqualTo("3");
  }
}
