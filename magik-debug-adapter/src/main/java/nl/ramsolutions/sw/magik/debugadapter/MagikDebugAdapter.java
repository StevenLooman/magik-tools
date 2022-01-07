package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import nl.ramsolutions.sw.magik.debugadapter.BreakpointManager.MagikBreakpoint;
import nl.ramsolutions.sw.magik.debugadapter.VariableManager.MagikVariable;
import nl.ramsolutions.sw.magik.debugadapter.slap.ISlapEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapException;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapProtocol;
import nl.ramsolutions.sw.magik.debugadapter.slap.SlapProtocol.SlapEventListener;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.DisconnectedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadEndedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.FunctionBreakpoint;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Magik debug adapter.
 */
public class MagikDebugAdapter implements IDebugProtocolServer, SlapEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagikDebugAdapter.class);

    private IDebugProtocolClient debugClient;
    private SlapProtocol slapProtocol;
    private ThreadManager threadManager;
    private VariableManager variableManager;
    private BreakpointManager breakpointManager;

    /**
     * Connect to the debug client.
     *
     * @param newDebugClient Debug client to connect to.
     */
    public void connect(final IDebugProtocolClient newDebugClient) {
        this.debugClient = newDebugClient;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(final InitializeRequestArguments args) {
        final Capabilities capabilities = new Capabilities();
        capabilities.setSupportsFunctionBreakpoints(true);
        capabilities.setExceptionBreakpointFilters(BreakpointManager.EXCEPTION_BREAKPOINTS_FILTERS);
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> attach(final Map<String, Object> args) {
        final Map<String, Object> connect = (Map<String, Object>) args.get("connect");
        if (connect == null) {
            final OutputEventArguments outputArgs = new OutputEventArguments();
            outputArgs.setCategory(OutputEventArgumentsCategory.STDERR);
            outputArgs.setOutput("No \"connect\" object in configuration, aborting.");
            this.debugClient.output(outputArgs);

            final Throwable exception = new Exception("No \"connect\" object in configuration, aborting.");
            final CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(exception);
            return future;
        }

        final String host = (String) connect.get("host");
        final int port = (int) Math.floor((Double) connect.get("port"));

        return CompletableFuture.runAsync(() -> {
            try {
                // Connect to session.
                this.slapProtocol = new SlapProtocol(host, port, this);

                // Inform client we're initialized.
                this.debugClient.initialized();
            } catch (IOException | SlapException exception) {
                throw new CompletionException(exception);
            }

            this.breakpointManager = new BreakpointManager(this.slapProtocol, this.debugClient);
            this.threadManager = new ThreadManager(this.slapProtocol, this.debugClient);
            this.variableManager = new VariableManager(this.slapProtocol);
        });
    }

    @Override
    public CompletableFuture<Void> disconnect(final DisconnectArguments args) {
        return CompletableFuture.runAsync(() -> {
            try {
                this.slapProtocol.close();
            } catch (IOException exception) {
                LOGGER.error(exception.getMessage(), exception);
            }
            this.slapProtocol = null;

            this.threadManager = null;
            this.variableManager = null;
            this.breakpointManager = null;
        });
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        LOGGER.trace("threads");
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get threads.
                final List<Thread> threads = this.threadManager.threads();

                // Return response.
                final ThreadsResponse response = new ThreadsResponse();
                final Thread[] threadsArr = threads.stream().toArray(Thread[]::new);
                response.setThreads(threadsArr);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(final StackTraceArguments args) {
        LOGGER.trace("stack trace, thread id: {}", args.getThreadId());
        return CompletableFuture.supplyAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                // Get stack frames.
                final List<StackFrame> stackFrames = this.threadManager.stackTrace(threadId);

                // Return response.
                final StackTraceResponse response = new StackTraceResponse();
                final StackFrame[] stackFramesArr = stackFrames.stream().toArray(StackFrame[]::new);
                response.setStackFrames(stackFramesArr);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(final ScopesArguments args) {
        LOGGER.debug("scopes: frame id: {}", args.getFrameId());
        return CompletableFuture.supplyAsync(() -> {
            // Get scopes.
            final int frameId = args.getFrameId();
            final Scope[] scopes = this.variableManager.getScopes(frameId);

            // Return response.
            final ScopesResponse response = new ScopesResponse();
            response.setScopes(scopes);
            return response;
        });
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(final VariablesArguments args) {
        LOGGER.trace("variables: reference: {}", args.getVariablesReference());
        return CompletableFuture.supplyAsync(() -> {
            final int reference = args.getVariablesReference();
            try {
                final List<MagikVariable> magikVariables = this.variableManager.getVariables(reference);

                // Return response.
                final VariablesResponse response = new VariablesResponse();
                final Variable[] variables = Lsp4jConversion.toLsp4j(magikVariables);
                response.setVariables(variables);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(final ContinueArguments args) {
        LOGGER.trace("continue: thread id: {}", args.getThreadId());
        return CompletableFuture.supplyAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                this.threadManager.continue_(threadId);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }

            return new ContinueResponse();
        });
    }

    @Override
    public CompletableFuture<Void> pause(final PauseArguments args) {
        LOGGER.trace("pause: thread is: {}", args.getThreadId());
        return CompletableFuture.runAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                this.threadManager.pause(threadId);
                this.variableManager.clear();

                // We receive no event on suspending a thread, so fake it.
                final StoppedEventArguments eventArgs = new StoppedEventArguments();
                eventArgs.setThreadId(threadId);
                eventArgs.setReason(StoppedEventArgumentsReason.PAUSE);
                this.debugClient.stopped(eventArgs);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> next(final NextArguments args) {
        LOGGER.trace("next: thread id: {}", args.getThreadId());
        return CompletableFuture.runAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                this.threadManager.stepNext(threadId);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stepIn(final StepInArguments args) {
        LOGGER.trace("stepIn: {}", args.getThreadId());
        return CompletableFuture.runAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                this.threadManager.stepIn(threadId);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> stepOut(final StepOutArguments args) {
        LOGGER.trace("step out: thread id: {}", args.getThreadId());
        return CompletableFuture.runAsync(() -> {
            final int threadId = args.getThreadId();
            try {
                this.threadManager.stepOut(threadId);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(final SetBreakpointsArguments args) {
        LOGGER.trace("setBreakpoints");
        return CompletableFuture.supplyAsync(() -> {
            final Source source = args.getSource();

            // Do some manual fixing if needed.
            if (source.getName() == null
                && source.getPath() != null) {
                final Path path = Path.of(source.getPath());
                final String name = path.getFileName().toString();
                source.setName(name);
            }

            try {
                final SourceBreakpoint[] sourceBreakpoints = args.getBreakpoints();
                final List<MagikBreakpoint> magikBreakpoints =
                        this.breakpointManager.setBreakpoints(source, sourceBreakpoints);

                // Return response.
                final SetBreakpointsResponse response = new SetBreakpointsResponse();
                final Breakpoint[] breakpoints = Lsp4jConversion.toLsp4j(source, magikBreakpoints);
                response.setBreakpoints(breakpoints);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(
            final SetFunctionBreakpointsArguments args) {
        LOGGER.trace("setFunctionBreakpoints");
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Clear existing breakpoints.
                this.breakpointManager.clearFunctionBreakpoints();

                // Set new breakpoints.
                final FunctionBreakpoint[] functionBreakpoints = args.getBreakpoints();
                final List<MagikBreakpoint> magikBreakpoints =
                        this.breakpointManager.addFunctionBreakpoints(functionBreakpoints);

                // Return response.
                final SetFunctionBreakpointsResponse response = new SetFunctionBreakpointsResponse();
                final Breakpoint[] breakpoints = Lsp4jConversion.toLsp4j(null, magikBreakpoints);
                response.setBreakpoints(breakpoints);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> setExceptionBreakpoints(final SetExceptionBreakpointsArguments args) {
        LOGGER.trace("setExceptionBreakpoints");
        return CompletableFuture.runAsync(() -> {
            final String[] filters = args.getFilters();
            try {
                // Set (or update) condition breakpoint.
                this.breakpointManager.setConditionBreakpoint(filters);
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(final EvaluateArguments args) {
        LOGGER.trace(
            "evaluate, expression: '{}', frame id: {}",
            args.getExpression(), args.getFrameId());
        return CompletableFuture.supplyAsync(() -> {
            final Integer frameId = args.getFrameId();
            final String expression = args.getExpression();
            try {
                // Evaluate in thread/frame.
                final String result = this.threadManager.evaluate(frameId, expression);

                // Return response.
                final EvaluateResponse response = new EvaluateResponse();
                response.setResult(result);
                return response;
            } catch (InterruptedException exception) {
                java.lang.Thread.currentThread().interrupt();
                throw new CompletionException(exception.getMessage(), exception);
            } catch (IOException | ExecutionException exception) {
                throw new CompletionException(exception.getMessage(), exception);
            }
        });
    }

    @Override
    public void handleEvent(final ISlapEvent event) {
        LOGGER.trace("Got event: {}", event);

        // Fire up a new thread handling the event.
        // Doing slap requests from the slap-protocol-receiver thread will lock that thread.
        // I.e., doing the evaluate from within the breakpoint-event to check the condition
        // causes the slap-protocol-receiver thread to wait for an answer, while it should be
        // receiving data.
        final MagikDebugAdapter adapter = this;
        final java.lang.Thread eventHandlerThread = new java.lang.Thread(() -> adapter.processEvent(event));
        eventHandlerThread.setName("event-handler-thread");
        eventHandlerThread.start();
    }

    /**
     * Process an event.
     * @param event Event to process.
     */
    void processEvent(final ISlapEvent event) {
        if (event instanceof BreakpointEvent) {
            final BreakpointEvent breakpointEvent = (BreakpointEvent) event;
            this.breakpointManager.handleBreakpointEvent(breakpointEvent);
            this.threadManager.handleBreakpointEvent(breakpointEvent);
            this.variableManager.handleBreakpointEvent(breakpointEvent);
        } else if (event instanceof ThreadStartedEvent) {
            final ThreadStartedEvent threadStartedEvent = (ThreadStartedEvent) event;
            this.threadManager.handleThreadStartedEvent(threadStartedEvent);
        } else if (event instanceof ThreadEndedEvent) {
            final ThreadEndedEvent threadEndedEvent = (ThreadEndedEvent) event;
            this.threadManager.handleThreadEndedEvent(threadEndedEvent);
        } else if (event instanceof StepCompletedEvent) {
            final StepCompletedEvent stepCompletedEvent = (StepCompletedEvent) event;
            this.threadManager.handleStepCompletedEvent(stepCompletedEvent);
            this.variableManager.handleStepCompletedEvent(stepCompletedEvent);
        } else if (event instanceof DisconnectedEvent) {
            final TerminatedEventArguments args = new TerminatedEventArguments();
            this.debugClient.terminated(args);
        }
    }

}
