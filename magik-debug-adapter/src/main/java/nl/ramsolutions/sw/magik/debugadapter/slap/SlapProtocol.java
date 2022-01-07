package nl.ramsolutions.sw.magik.debugadapter.slap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.BreakpointEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.DisconnectedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.StepCompletedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadEndedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.events.ThreadStartedEvent;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointModifyResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.BreakpointSetResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ErrorResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.EvalResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ResumeThreadResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SourceFileResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StackFrameLocalsResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.StepResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.SuspendThreadResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadInfoResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadListResponse;
import nl.ramsolutions.sw.magik.debugadapter.slap.responses.ThreadStackResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slap protocol.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SlapProtocol implements ISlapProtocol {

    /**
     * Request future.
     */
    static class RequestFuture {
        private final RequestType requestType;
        private final CompletableFuture<ISlapResponse> future;

        RequestFuture(final RequestType requestType, final CompletableFuture<ISlapResponse> future) {
            this.requestType = requestType;
            this.future = future;
        }

        @Override
        public String toString() {
            return String.format(
                "%s@%s(%s)",
                this.getClass().getName(), Integer.toHexString(this.hashCode()),
                this.requestType);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SlapProtocol.class);

    private static final String DEBUG_CLIENT_ID = "DuckOnATricycle\0";
    private static final String DEBUG_AGENT_ID = "SwanOnAUnicycle\0";

    /**
     * State of protocol.
     */
    enum State {

        WAITING,
        WAITING_FOR_MULTIPLE_RESPONSES,

    }

    /**
     * Event listener for incoming events.
     */
    public interface SlapEventListener {
        /**
         * Handle an incoming {{SlapEvent}}.
         * @param event Incoming event.
         */
        void handleEvent(ISlapEvent event);
    }

    private final SlapEventListener listener;
    private final SocketChannel socketChannel;
    private final ByteBuffer inputBuffer = ByteBuffer.allocate(10240);
    private ByteOrder byteOrder = ByteOrder.nativeOrder();
    private State state;
    private RequestType multiResponseRequestType;
    private long version;
    private final List<RequestFuture> requestFutures =
            Collections.synchronizedList(new ArrayList<>());
    private final List<ISlapResponse> subResponses = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param host Hostname to connect to.
     * @param port Port to connect to.
     * @throws SlapException -
     */
    public SlapProtocol(final String host, final int port, final SlapEventListener listener)
            throws IOException, SlapException {
        this.listener = listener;

        this.state = State.WAITING;
        this.version = -1;

        // Connect socket.
        this.socketChannel = SocketChannel.open();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        this.socketChannel.connect(inetSocketAddress);

        this.doHandshake();
        this.startReceiverThread();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void startReceiverThread() {
        final SlapProtocol protocol = this;
        final Thread receiverThread = new Thread(() -> {
            while (true) {
                try {
                    if (!protocol.isConnected()) {
                        break;
                    }

                    protocol.handleData();
                } catch (Exception exception) {
                    LOGGER.error(exception.getMessage(), exception);
                }
            }
            final DisconnectedEvent event = new DisconnectedEvent();
            protocol.listener.handleEvent(event);
        });
        receiverThread.setName("slap-protocol-receiver");
        receiverThread.start();

        LOGGER.debug("Started received thread: {}", receiverThread);
    }

    private void doHandshake() throws IOException, SlapException {
        // Send our secret password.
        final ByteBuffer handshakeMessage = ByteBuffer.wrap(DEBUG_CLIENT_ID.getBytes(StandardCharsets.UTF_8));
        this.socketChannel.write(handshakeMessage);

        // Response.
        // 0-15: string DEBUG_AGENT_ID
        // 16-16: uint8 endianness
        // 17-21: uint32 version
        // 22-32: ???
        final ByteBuffer handshakeResponse = ByteBuffer.allocate(32);
        this.socketChannel.read(handshakeResponse);
        handshakeResponse.flip();

        // Test if bouncer answers with the right secret response.
        final byte[] strBuffer = new byte[16];
        handshakeResponse.get(strBuffer);
        final String debugAgentId = new String(strBuffer, StandardCharsets.UTF_8);
        if (!debugAgentId.equals(DEBUG_AGENT_ID)) {
            throw new SlapException("Unknown debug agent");
        }

        // Determine endianness.
        final boolean isLittleEndian = handshakeResponse.get() == 1;
        this.byteOrder = isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        handshakeResponse.order(this.byteOrder);
        this.inputBuffer.order(this.byteOrder);

        // Is this correct?
        this.version = ByteBufferUtils.readUInt32(handshakeResponse, 20);

        LOGGER.debug("Connected with MDA, version: {}", this.version);
    }

    public long getVersion() {
        return this.version;
    }

    /**
     * Close the connection to the debuggee.
     *
     * @throws IOException                    -
     */
    public void close() throws IOException {
        this.socketChannel.close();
        // Receiver thread will end on its own.
    }

    /**
     * Is connected with debuggee.
     * @return True if connected, false if not.
     */
    public boolean isConnected() {
        return this.socketChannel.isConnected();
    }

    // region: request sending
    /**
     * Send a request.
     * @param requestType Request type to send.
     * @return Response of request.
     * @throws IOException -
     */
    private CompletableFuture<ISlapResponse> sendRequest(final RequestType requestType) throws IOException {
        return this.sendRequest(requestType, 0, 0, new byte[0]);
    }

    /**
     * Send a request.
     * @param requestType Request type to send.
     * @param param0 Parameter 0.
     * @return Response of request.
     * @throws IOException -
     */
    private CompletableFuture<ISlapResponse> sendRequest(final RequestType requestType, final long param0)
             throws IOException {
        return this.sendRequest(requestType, param0, 0, new byte[0]);
    }

    /**
     * Send a request.
     * @param requestType Request type to send.
     * @param param0 Parameter 0.
     * @param param1 Parameter 1.
     * @return Response of request.
     * @throws IOException -
     */
    private CompletableFuture<ISlapResponse> sendRequest(
            final RequestType requestType, final long param0, final long param1)
            throws IOException {
        return this.sendRequest(requestType, param0, param1, new byte[0]);
    }

    /**
     * Send a request.
     * @param requestType Request type to send.
     * @param param0 Parameter 0.
     * @param param1 Parameter 1.
     * @param data Additional data to send.
     * @return Response of request.
     * @throws IOException -
     */
    private CompletableFuture<ISlapResponse> sendRequest(
            final RequestType requestType, final long param0, final long param1, final byte[] data)
            throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocate(16 + data.length);
        buffer.order(this.byteOrder);

        final int requestLength = buffer.limit();
        final int requestVal = requestType.getVal();
        ByteBufferUtils.writeUInt32(buffer, requestLength);
        ByteBufferUtils.writeUInt32(buffer, requestVal);
        ByteBufferUtils.writeUInt32(buffer, param0);
        ByteBufferUtils.writeUInt32(buffer, param1);
        buffer.put(data);
        buffer.flip();

        // Try to keep these future-bookkeeping and socket in order.
        synchronized (this) {
            LOGGER.trace(
                "Thread: {}, Sending, type: {}, param0: {}, param1: {}",
                Thread.currentThread().getName(), requestType, param0, param1);
            final CompletableFuture<ISlapResponse> future = this.addFutureRequest(requestType);
            this.socketChannel.write(buffer);
            LOGGER.trace(
                "Thread: {}, Sent, type: {}, param0: {}, param1: {}",
                Thread.currentThread().getName(), requestType, param0, param1);
            return future;
        }
    }
    // endregion

    // region: Input handling
    /**
     * Handle incoming data.
     * Call this regularly.
     */
    private void handleData() throws IOException {
        // Read from socket.
        final int read = this.socketChannel.read(this.inputBuffer);
        if (read == -1) {
            // Channel has reached end-of-stream.
            this.socketChannel.close();
            return;
        }
        this.inputBuffer.flip();

        final int limit = this.inputBuffer.limit();
        LOGGER.trace("Received data, byte count: {}", limit);
        if (limit < 4) {
            LOGGER.warn("Ignoring received data, byte count: {}", limit);
            // Ignore this data.
            this.inputBuffer.clear();
            return;
        }

        while (this.inputBuffer.hasRemaining()) {
            final int startPosition = this.inputBuffer.position();
            final int messageLength = (int) ByteBufferUtils.readUInt32(this.inputBuffer); // byte: 0-4
            if (this.inputBuffer.limit() < messageLength) {
                // Did not receive enough data (yet), wait for more data.
                break;
            }

            // Rewind, copy data, decode message.
            this.inputBuffer.position(startPosition);
            final byte[] message = new byte[messageLength];
            this.inputBuffer.get(message);
            final ByteBuffer roBuffer = ByteBuffer.wrap(message).asReadOnlyBuffer();
            roBuffer.order(this.inputBuffer.order());
            this.handleMessage(roBuffer);
        }

        if (this.inputBuffer.position() != this.inputBuffer.limit()) {
            // Sanity.
            LOGGER.warn("Euh...");
        }

        this.inputBuffer.compact();
    }

    /**
     * Handle a message.
     * @param buffer Buffer that contains the message.
     */
    private void handleMessage(final ByteBuffer buffer) {
        // Basic message layout:
        // 00-04: uint32, message length
        // 04-08: uint32, response type
        final int val = (int) ByteBufferUtils.readUInt32(buffer, 4);
        final ResponseType responseType = ResponseType.valueOf(val);
        switch (responseType) {
            case ERROR:
                this.handleErrorMessage(buffer);
                break;

            case EVENT:
                this.handleEventMessage(buffer);
                break;

            case REPLY:
                this.handleReplyMessage(buffer);
                break;

            default:
                break;
        }
    }

    /**
     * Handle an reply-message.
     * @param buffer Buffer that contains the message.
     */
    private void handleReplyMessage(final ByteBuffer buffer) {
        // Basic reply layout:
        // 00-04: uint32, message length
        // 04-08: uint32, response type
        // 08-12: uint32, request type
        // 12-16: uint32, 0xFFFFFFFF, if multi-message and stop-message
        RequestType requestType = RequestType.UNKOWN;
        boolean isEndPacket = false;
        if (this.state == State.WAITING) {
            final int val = (int) ByteBufferUtils.readUInt32(buffer, 8);
            requestType = RequestType.valueOf(val);
        } else if (this.state == State.WAITING_FOR_MULTIPLE_RESPONSES) {
            requestType = this.multiResponseRequestType;
            isEndPacket =
                (buffer.get(12) & 0xFF) == 0xFF
                && (buffer.get(13) & 0xFF) == 0xFF
                && (buffer.get(14) & 0xFF) == 0xFF
                && (buffer.get(15) & 0xFF) == 0xFF;
        }

        ISlapResponse response = null;
        switch (requestType) {
            case GET_THREAD_LIST:
                response = ThreadListResponse.decode(buffer);
                break;

            case GET_THREAD_INFO:
                response = ThreadInfoResponse.decode(buffer);
                break;

            case SUSPEND_THREAD:
                response = SuspendThreadResponse.decode(buffer);
                break;

            case RESUME_THREAD:
                response = ResumeThreadResponse.decode(buffer);
                break;

            case GET_THREAD_STACK:
                if (this.state == State.WAITING_FOR_MULTIPLE_RESPONSES) {
                    // Currently receiving multiple responses.
                    if (isEndPacket) {
                        // Build final message.
                        response = new ThreadStackResponse(this.subResponses);
                        this.subResponses.clear();
                        this.state = State.WAITING;
                        this.multiResponseRequestType = null;
                    } else {
                        final ISlapResponse subResponse = ThreadStackResponse.StackElement.decode(buffer);
                        this.subResponses.add(subResponse);
                    }
                } else {
                    // First packet sets state.
                    this.state = State.WAITING_FOR_MULTIPLE_RESPONSES;
                    this.multiResponseRequestType = RequestType.GET_THREAD_STACK;
                }
                break;

            case GET_FRAME_LOCALS:
                if (this.state == State.WAITING_FOR_MULTIPLE_RESPONSES) {
                    // Currently receiving multiple responses.
                    if (isEndPacket) {
                        // Build final message.
                        response = new StackFrameLocalsResponse(this.subResponses);
                        this.subResponses.clear();
                        this.state = State.WAITING;
                        this.multiResponseRequestType = null;
                    } else {
                        final ISlapResponse subResponse = StackFrameLocalsResponse.Local.decode(buffer);
                        this.subResponses.add(subResponse);
                    }
                } else {
                    // First packet sets state.
                    this.state = State.WAITING_FOR_MULTIPLE_RESPONSES;
                    this.multiResponseRequestType = RequestType.GET_FRAME_LOCALS;
                }
                break;

            case BREAKPOINT_SET:
                response = BreakpointSetResponse.decode(buffer);
                break;

            case BREAKPOINT_MODIFY:
                response = BreakpointModifyResponse.decode(buffer);
                break;

            case EVALUATE:
                response = EvalResponse.decode(buffer);
                break;

            case SOURCE_FILE:
                response = SourceFileResponse.decode(buffer);
                break;

            case STEP:
                response = StepResponse.decode(buffer);
                break;

            default:
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Unknown response, ByteBuffer: {}, contents:\n{}",
                        buffer, ByteBufferUtils.toHexDump(buffer));
                }
                throw new IllegalStateException("Unknown response");
        }

        LOGGER.trace(
            "Thread: {}, Received reply: type: {}, {}",
            Thread.currentThread().getName(), requestType, response);

        if (response != null) {
            // Complete future.
            this.handleFutureRequest(requestType, response);
        }
    }

    /**
     * Handle an event-message.
     * @param buffer Buffer that contains the message.
     */
    private void handleEventMessage(final ByteBuffer buffer) {
        // Basic event layout:
        // 00-04: uint32, message length
        // 04-08: uint32, response type
        // 08-12: uint32, event type
        final int val = (int) ByteBufferUtils.readUInt32(buffer, 8);
        final EventType eventType = EventType.valueOf(val);
        LOGGER.trace(
            "Thread: {}, Received event: event type: {}",
            Thread.currentThread().getName(), eventType);

        ISlapEvent event = null;
        switch (eventType) {
            case BREAKPOINT:
                event = BreakpointEvent.decode(buffer);
                break;

            case THREAD_STARTED:
                event = ThreadStartedEvent.decode(buffer);
                break;

            case THREAD_ENDED:
                event = ThreadEndedEvent.decode(buffer);
                break;

            case STEP_COMPLETED:
                event = StepCompletedEvent.decode(buffer);
                break;

            default:
                throw new IllegalStateException("Unknown message");
        }

        this.listener.handleEvent(event);
    }

    /**
     * Handle an error-message.
     * @param buffer Buffer that contains the message.
     */
    private void handleErrorMessage(final ByteBuffer buffer) {
        // Error response layout:
        // 00-04: uint32, message length
        // 04-08: uint32, response type
        // 08-12: uint32, request type
        // 12-16: uint32, error message
        final int val = (int) ByteBufferUtils.readUInt32(buffer, 8);
        final RequestType requestType = RequestType.valueOf(val);
        LOGGER.trace(
            "Thread: {}, Received error: request type: {}",
            Thread.currentThread().getName(), requestType);

        // Complete future.
        final ErrorResponse response = ErrorResponse.decode(buffer);
        this.handleErrorFutureRequest(requestType, response);
    }
    // endregion

    // region: Breakpoints
    /**
     * Set a breakpoint.
     *
     * @param method Method to set breakpoint in.
     * @param line     Line in method, offset from start of method (`_method` token).
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> setBreakpoint(final String method, final int line) throws IOException {
        LOGGER.debug("Set breakpoint, method: {}, line: {}", method, line);
        final byte[] methodBytes = method.getBytes(StandardCharsets.UTF_8);
        final byte[] data = new byte[4 + methodBytes.length];
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(this.byteOrder);

        ByteBufferUtils.writeString(buffer, method);

        return this.sendRequest(RequestType.BREAKPOINT_SET, 0, line, data);
    }

    /**
     * Delete a breakpoint.
     *
     * @param breakpointId Breakpoint to delete.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> deleteBreakpoint(final long breakpointId) throws IOException {
        LOGGER.debug("Delete breakpoint, breakpoint_id: {}", breakpointId);
        return this.sendRequest(
            RequestType.BREAKPOINT_MODIFY,
            breakpointId,
            ModifyBreakpoint.DELETE.getVal());
    }

    /**
     * Enable a breakpoint.
     * @param breakpointId Breakpoint to enable.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> enableBreakpoint(final long breakpointId) throws IOException {
        LOGGER.debug("Enable breakpoint, breakpoint_id: {}", breakpointId);
        return this.sendRequest(
            RequestType.BREAKPOINT_MODIFY,
            breakpointId,
            ModifyBreakpoint.ENABLE.getVal());
    }

    /**
     * Disable a breakpoint.
     * @param breakpointId Breakpoint to disable.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> disableBreakpoint(final long breakpointId) throws IOException {
        LOGGER.debug("Disable breakpoint, breakpoint_id: {}", breakpointId);
        return this.sendRequest(
            RequestType.BREAKPOINT_MODIFY,
            breakpointId,
            ModifyBreakpoint.DISABLE.getVal());
    }
    // endregion

    // region: Thread info
    /**
     * Get a list of threads.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> getThreadList() throws IOException {
        LOGGER.debug("Get thread list");
        return this.sendRequest(RequestType.GET_THREAD_LIST);
    }

    /**
     * Get information about a thread.
     * @param threadId Thread to get information from.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> getThreadInfo(final long threadId) throws IOException {
        LOGGER.debug("Get thread info, thread_id: {}", threadId);
        return this.sendRequest(RequestType.GET_THREAD_INFO, threadId);
    }

    /**
     * Get the stack of a thread.
     * @param threadId Thread to get stack from.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> getThreadStack(final long threadId) throws IOException {
        LOGGER.debug("Get thread stack, thread_id: {}", threadId);
        return this.sendRequest(RequestType.GET_THREAD_STACK, threadId);
    }

    /**
     * Get the locals from a stack frame.
     * @param threadId Thread to get locals from.
     * @param level Stack level.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> getStackFrameLocals(
            final long threadId, final int level)
            throws IOException {
        LOGGER.debug("Get stack frame locals, thread_id: {}, level: {}", threadId, level);
        return this.sendRequest(RequestType.GET_FRAME_LOCALS, threadId, level);
    }
    // endregion

    // region: Thread control
    /**
     * Suspend a thread.
     * @param threadId Thread to suspend.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> suspendThread(final long threadId) throws IOException {
        LOGGER.debug("Suspend thread, thread_id: {}", threadId);
        return this.sendRequest(RequestType.SUSPEND_THREAD, threadId);
    }

    /**
     * Resume a thread.
     * @param threadId Thread to resume.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> resumeThread(final long threadId) throws IOException {
        LOGGER.debug("Resume thread, thread_id: {}", threadId);
        return this.sendRequest(RequestType.RESUME_THREAD, threadId);
    }
    // endregion

    /**
     * Get the source file for method.
     * @param method Method to get source file for.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> getSourceFile(final String method) throws IOException {
        LOGGER.debug("Get source file, method: {}", method);
        final byte[] methodBytes = method.getBytes(StandardCharsets.UTF_8);
        final byte[] data = new byte[4 + methodBytes.length];
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(this.byteOrder);

        ByteBufferUtils.writeString(buffer, method);

        return this.sendRequest(RequestType.SOURCE_FILE, 0, 0, data);
    }

    /**
     * Perform a step.
     * @param threadId Thread to step.
     * @param stepType Type of step.
     * @param count Number of steps.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> step(
            final long threadId, final StepType stepType, final int count)
            throws IOException {
        LOGGER.debug("Step, thread_id: {}, step_type: {}, count: {}", threadId, stepType, count);
        final int param1 = (count << 16) | stepType.getVal() | StepType.UNTIL_MAGIK.getVal();
        return this.sendRequest(RequestType.STEP, threadId, param1);
    }

    /**
     * Evaluate {{code}}.
     * @param threadId Thread to evaluate in.
     * @param level Stack level to evaluate in.
     * @param expression Code to evaluate.
     * @return Response of request.
     * @throws IOException -
     */
    public CompletableFuture<ISlapResponse> evaluate(
            final long threadId, final int level, final String expression)
            throws IOException {
        LOGGER.debug(
            "Evaluate, thread_id: {}, level: {}, expression: '{}'", threadId, level, expression);
        final byte[] expressionBytes = expression.getBytes(StandardCharsets.UTF_8);
        final byte[] data = new byte[4 + expressionBytes.length];
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(this.byteOrder);

        ByteBufferUtils.writeString(buffer, expression);

        return this.sendRequest(RequestType.EVALUATE, threadId, level, data);
    }

    // region: Requests/Futures
    private CompletableFuture<ISlapResponse> addFutureRequest(final RequestType requestType) {
        final CompletableFuture<ISlapResponse> future = new CompletableFuture<>();
        final RequestFuture requestFuture = new RequestFuture(requestType, future);

        synchronized (this.requestFutures) {
            LOGGER.debug(
                    "Thread: {}, added request future: {}",
                    Thread.currentThread().getName(), requestFuture);
            this.requestFutures.add(requestFuture);
        }

        return future;
    }

    private void handleFutureRequest(final RequestType requestType, final ISlapResponse response) {
        RequestFuture requestFuture = null;

        LOGGER.debug(
            "Thread: {}, Request type: {}, response: {}, request futures: {}",
            Thread.currentThread().getName(), requestType, response, this.requestFutures);

        synchronized (this.requestFutures) {
            final Optional<RequestFuture> first = this.requestFutures.stream()
                .filter(pair -> pair.requestType == requestType)
                .findFirst();
            if (!first.isPresent()) {
                LOGGER.warn(
                    "Thread: {}, Trying to handle request, but not available, request type: {}",
                    Thread.currentThread().getName(), requestType);
                return;
            }

            requestFuture = first.get();
            this.requestFutures.remove(requestFuture);
        }

        requestFuture.future.complete(response);
    }

    private void handleErrorFutureRequest(final RequestType requestType, final ErrorResponse errorResponse) {
        RequestFuture requestFuture = null;

        LOGGER.debug(
            "Thread: {}, Request type: {}, error response: {}, request futures: {}",
            Thread.currentThread().getName(), requestType, errorResponse, this.requestFutures);

        synchronized (this.requestFutures) {
            final Optional<RequestFuture> findFirst = this.requestFutures.stream()
                .filter(pair -> pair.requestType == requestType)
                .findFirst();
            if (!findFirst.isPresent()) {
                LOGGER.warn(
                    "Thread: {}, Trying to handle request, but not available, request type: {}",
                    Thread.currentThread().getName(), requestType);
                return;
            }

            requestFuture = findFirst.get();
            this.requestFutures.remove(requestFuture);
        }

        final SlapErrorException exception = new SlapErrorException(errorResponse);
        requestFuture.future.completeExceptionally(exception);
    }
    // endregion

}
