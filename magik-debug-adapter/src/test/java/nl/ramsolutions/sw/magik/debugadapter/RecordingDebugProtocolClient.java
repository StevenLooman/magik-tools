package nl.ramsolutions.sw.magik.debugadapter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

/** Debug protocol client which records the events it receives. */
class RecordingDebugProtocolClient implements IDebugProtocolClient {

  private final List<StoppedEventArguments> stoppedEvents = new CopyOnWriteArrayList<>();
  private final List<ThreadEventArguments> threadEvents = new CopyOnWriteArrayList<>();
  private volatile Runnable initializedHook = () -> {};

  List<StoppedEventArguments> getStoppedEvents() {
    return this.stoppedEvents;
  }

  List<ThreadEventArguments> getThreadEvents() {
    return this.threadEvents;
  }

  /**
   * Set the code to run when the `initialized` event arrives.
   *
   * @param hook Code to run.
   */
  void setInitializedHook(final Runnable hook) {
    this.initializedHook = hook;
  }

  @Override
  public void initialized() {
    this.initializedHook.run();
  }

  @Override
  public void stopped(final StoppedEventArguments args) {
    this.stoppedEvents.add(args);
  }

  @Override
  public void thread(final ThreadEventArguments args) {
    this.threadEvents.add(args);
  }
}
