package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapException;
import nl.ramsolutions.sw.magik.debugadapter.slap.StepType;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointModifyResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;

/** Test slap protocol. */
public class TestSlapProtocol implements ISlapProtocol {

  private long nextBreakpointId;
  private final List<Long> breakpoints = new ArrayList<>();

  public TestSlapProtocol() {}

  @Override
  public void connect() throws IOException, SlapException {
    // Does nothing.
  }

  @Override
  public long getVersion() {
    return -1;
  }

  @Override
  public void close() throws IOException {}

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public CompletableFuture<ISlapResponse> setBreakpoint(final String method, final int line) {
    long breakpointId = ++this.nextBreakpointId;
    this.breakpoints.add(breakpointId);

    final BreakpointSetResponse response = new BreakpointSetResponse(breakpointId);
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public CompletableFuture<ISlapResponse> deleteBreakpoint(final long breakpointId) {
    this.breakpoints.remove(breakpointId);

    final BreakpointModifyResponse response = new BreakpointModifyResponse();
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public CompletableFuture<ISlapResponse> enableBreakpoint(final long breakpointId) {
    final BreakpointModifyResponse response = new BreakpointModifyResponse();
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public CompletableFuture<ISlapResponse> disableBreakpoint(final long breakpointId) {
    final BreakpointModifyResponse response = new BreakpointModifyResponse();
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public CompletableFuture<ISlapResponse> getThreadList() throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> getThreadInfo(final long threadId) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> getThreadStack(final long threadId) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> getStackFrameLocals(final long threadId, final int level)
      throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> suspendThread(final long threadId) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> resumeThread(final long threadId) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> step(
      final long threadId, final StepType stepType, final int count) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> evaluate(
      final long threadId, final int level, final String expression) throws IOException {
    return null;
  }

  @Override
  public CompletableFuture<ISlapResponse> getSourceFile(final String method) throws IOException {
    return null;
  }
}
