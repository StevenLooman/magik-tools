package nl.ramsolutions.sw.magik.debugadapter.evaluate;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.Lsp4jConversion;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

/** Evaluates an expression in a stack frame, through the agent. */
public class FrameEvaluateModule implements EvaluateModule {

  private static final String EVALUATE_WITHOUT_FRAME_MESSAGE =
      "Select a stack frame of a stopped thread to evaluate an expression.";

  private final ISlapProtocol slapProtocol;

  /**
   * Constructor.
   *
   * @param slapProtocol Protocol to the agent.
   */
  public FrameEvaluateModule(final ISlapProtocol slapProtocol) {
    this.slapProtocol = slapProtocol;
  }

  /**
   * Create the error for an evaluate request without a stack frame.
   *
   * @return Error to fail the request with.
   */
  public static ResponseErrorException createMissingFrameException() {
    final ResponseError error = new ResponseError();
    error.setCode(ResponseErrorCode.InvalidParams);
    error.setMessage(EVALUATE_WITHOUT_FRAME_MESSAGE);
    return new ResponseErrorException(error);
  }

  @Override
  public Optional<String> tryEvaluate(final EvaluateContext context)
      throws IOException, InterruptedException, ExecutionException {
    final Integer frameId = context.frameId();
    if (frameId == null) {
      throw FrameEvaluateModule.createMissingFrameException();
    }

    final long threadId = Lsp4jConversion.frameIdToThreadId(frameId);
    final int level = Lsp4jConversion.frameIdToLevel(frameId);
    final String expression = context.expression();
    final CompletableFuture<ISlapResponse> evaluateFuture =
        this.slapProtocol.evaluate(threadId, level, expression);
    final EvalResponse response = (EvalResponse) evaluateFuture.get();
    final String result = response.getResult();
    return Optional.of(result);
  }
}
