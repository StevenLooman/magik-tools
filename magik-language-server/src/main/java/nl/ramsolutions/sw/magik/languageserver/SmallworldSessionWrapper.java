package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal wrapper for Smallworld session.
 * Allows you to start and stop a session. Interfaces with the Smallworld
 * session via its terminal.
 * Provides useful shortcuts like alt-p and alt-n, for previous and next command, respectively.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SmallworldSessionWrapper {

    /**
     * Session handler.
     */
    public interface ISmallworldSessionWrapperHandler {

        /**
         * Process is started with pid.
         * @param pid PID of the process.
         */
        void processStarted(int pid);

        /**
         * Handle data from the active session.
         *
         * @param data Data from active session to handle.
         */
        void handleStdOut(byte[] data);

        /**
         * Handle data from the active session.
         *
         * @param data Data from active session to handle.
         */
        void handleStdErr(byte[] data);

        /**
         * Smallworld process was ended, with exitValue.
         * @param exitValue Exit value from process.
         */
        void onExit(int exitValue);

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SmallworldSessionWrapper.class);
    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

    private Process process;
    private final Path productDir;
    private final Path aliasesPath;
    private final String aliasesEntry;
    private Thread ioThread;
    private final ISmallworldSessionWrapperHandler handler;

    /**
     * Constructor.
     *
     * @param productDir        Path to Smallworld product dir.
     * @param aliasesPath     Path to aliases file
     * @param aliasesEntry    Entry to start.
     * @param handler Handler to handle output from session.
     */
    public SmallworldSessionWrapper(
            final Path productDir,
            final Path aliasesPath,
            final String aliasesEntry,
            final ISmallworldSessionWrapperHandler handler) {
        this.productDir = productDir;
        this.aliasesPath = aliasesPath;
        this.aliasesEntry = aliasesEntry;
        this.handler = handler;
    }

    /**
     * Start the session.
     * @throws IOException -
     */
    public void startSession() throws IOException {
        LOGGER.debug("Starting session");

        final String command = this.productDir.resolve("bin/share/runalias").toString();
        final ProcessBuilder processBuilder =
                new ProcessBuilder(command, "-a", this.aliasesPath.toString(), this.aliasesEntry);

        // Set environment vars.
        final Map<String, String> environment = processBuilder.environment();
        environment.put("SMALLWORLD_GIS", this.productDir.toAbsolutePath().toString());

        this.process = processBuilder.start();
        int pid = -1;    // Java 8 does not provide this PID.
        LOGGER.debug("Started process, PID: {}", pid);
        this.handler.processStarted(pid);

        // Start IO handler thread.
        this.ioThread = new Thread() {
            @Override
            public void run() {
                runIoPump();
            }
        };
        this.ioThread.start();
    }

    private void runIoPump() {
        boolean running = true;
        while (running) {
            running = this.process.isAlive();

            boolean pumpedIo = false;
            try {
                pumpedIo = pumpIo();

                if (!pumpedIo) {
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException exception) {
                exception.printStackTrace();
                running = false;
            }
        }

        if (!this.process.isAlive()) {
            int exitValue = this.process.exitValue();
            LOGGER.debug("Started stopped, exitValue: {}", exitValue);
            this.handler.onExit(exitValue);

            this.process = null;
        }

        // The thread will end after this.
        this.ioThread = null;
    }

    private boolean pumpIo() throws IOException {
        boolean pumpedIo = false;
        InputStream stdoutStream = this.process.getInputStream();
        InputStream stderrStream = this.process.getErrorStream();

        byte[] buffer = new byte[1024];
        if (stdoutStream.available() != 0) {
            pumpedIo = true;
            int read = stdoutStream.read(buffer);
            if (read > 0) {
                byte[] bufferCopy = Arrays.copyOfRange(buffer, 0, read);
                this.handler.handleStdOut(bufferCopy);
            }
        }

        if (stderrStream.available() != 0) {
            pumpedIo = true;
            int read = stdoutStream.read(buffer);
            if (read > 0) {
                byte[] bufferCopy = Arrays.copyOfRange(buffer, 0, read);
                this.handler.handleStdErr(bufferCopy);
            }
        }
        return pumpedIo;
    }

    /**
     * Get if sesesion is still active.
     * @return true is session is still active, false otherwise.
     */
    public boolean sessionActive() {
        return this.process != null
                     && this.process.isAlive();
    }

    /**
     * Stop the session, by killing the process.
     */
    public void stopSession() {
        if (this.process == null
                || !this.process.isAlive()) {
            throw new IllegalStateException("Process is not active");
        }

        LOGGER.debug("Destroying process");
        this.process.destroy();
        this.process = null;
    }

    /**
     * Write data to the session.
     * @param data Data to write.
     * @throws IOException -
     */
    public void writeData(byte[] data) throws IOException {
        LOGGER.trace("Appending $ to sent data");

        byte[] buffer = new byte[data.length + 2];
        System.arraycopy(data, 0, buffer, 0, data.length);
        buffer[data.length + 0] = '$';
        buffer[data.length + 1] = '\n';

        if (LOGGER.isTraceEnabled()) {
            String str = new String(buffer, CHARSET)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t")
                    .replace("\f", "\\f")
                    .replace("\r", "\\r");
            LOGGER.trace("Sending data: \"{}\"", str);
        }

        OutputStream stdinStream = this.process.getOutputStream();
        stdinStream.write(buffer);
        stdinStream.flush();
    }

    /**
     * Main entry point.
     * @param args Given arguments.
     * @throws IOException -
     */
    public static void main(String[] args) throws IOException {
        InputStream stream = Main.class.getClassLoader().getResourceAsStream("logging.properties");
        LogManager.getLogManager().readConfiguration(stream);

        Path productDir = Path.of("/home/steven/opt/SW522/core");
        Path aliasesPath = productDir.resolve(Path.of("config/gis_aliases"));
        String aliasesEntry = "base";

        ISmallworldSessionWrapperHandler handler = new ISmallworldSessionWrapperHandler() {
            @Override
            public void handleStdOut(byte[] buffer) {
                System.out.print(new String(buffer, CHARSET));
            }

            @Override
            public void handleStdErr(byte[] buffer) {
                System.out.print(new String(buffer, CHARSET));
            }

            @Override
            public void onExit(int exitValue) {
                System.out.println("Exited: " + exitValue);
            }

            @Override
            public void processStarted(int pid) {
                System.out.println("Process started: " + pid);
            }
        };

        SmallworldSessionWrapper wrapper = new SmallworldSessionWrapper(productDir, aliasesPath, aliasesEntry, handler);
        wrapper.startSession();

        while (wrapper.sessionActive()) {
            byte[] buffer = new byte[1024];
            int read = System.in.read(buffer);
            if (read > 0) {
                byte[] bufferCopy = Arrays.copyOfRange(buffer, 0, read);
                wrapper.writeData(bufferCopy);
            }
        }
    }

}
