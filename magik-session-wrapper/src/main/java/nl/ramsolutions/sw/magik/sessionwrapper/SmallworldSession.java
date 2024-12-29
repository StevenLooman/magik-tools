package nl.ramsolutions.sw.magik.sessionwrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Running Smallworld session. */
class SmallworldSession {

  static final Pattern DEFAULT_PROMPT_PATTERN = Pattern.compile("^(Magik|MagikSF)> $");

  private static final Logger LOGGER = LoggerFactory.getLogger(SmallworldSession.class);
  private static final Charset SESSION_CHARSET = StandardCharsets.ISO_8859_1;

  private final Process process;
  private final Pattern promptPattern;
  private final Thread ioThread;
  private Object promptSeen;

  /**
   * Constructor.
   *
   * @param process The process of the Smallworld session.
   * @param promptPattern The prompt pattern to match prompts on.
   * @param outputWriter Output writer to write session output to.
   */
  SmallworldSession(
      final Process process, final Pattern promptPattern, final PrintWriter outputWriter) {
    this.process = process;
    this.promptPattern = promptPattern;

    this.ioThread = new Thread(() -> this.threadRunner(outputWriter));
    this.ioThread.start();
  }

  private void threadRunner(final PrintWriter outputWriter) {
    this.promptSeen = new Object();

    final BufferedReader sessionReader = this.getSessionReader();
    final CharBuffer charBuffer = CharBuffer.allocate(1024);
    try {
      while (true) {
        sessionReader.read(charBuffer);
        charBuffer.flip();

        final String sessionOutput = charBuffer.toString();
        charBuffer.clear();

        this.processSessionOutput(outputWriter, sessionOutput);
      }
    } catch (final IOException exception) {
      if (this.process.isAlive() || !exception.getMessage().equals("Stream closed")) {
        LOGGER.error("Session reader thread stopped, exception occurred", exception);
      }
    }

    if (!this.process.isAlive()) {
      LOGGER.debug(
          "Session reader thread stopped, process exited, exit value: {}",
          this.process.exitValue());
    } else {
      LOGGER.debug("Session reader thread stopped, process still alive");
    }
  }

  private void processSessionOutput(final PrintWriter outputWriter, final String str) {
    // If prompt is seen, don't print it, but do signal it was seen.
    if (this.promptPattern.matcher(str).matches()) {
      this.signalPromptSeen();

      return;
    }

    // Write session output to output writer.
    outputWriter.print(str);
    outputWriter.flush();
  }

  BufferedReader getSessionReader() {
    final InputStream inputStream = this.process.getInputStream();
    final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, SESSION_CHARSET);
    return new BufferedReader(inputStreamReader);
  }

  PrintWriter getSessionWriter() {
    final OutputStream outputStream = this.process.getOutputStream();
    return new PrintWriter(outputStream, true, SESSION_CHARSET);
  }

  long getPid() {
    return this.process.pid();
  }

  boolean isAlive() {
    return this.process.isAlive();
  }

  void destroy() {
    this.process.destroy();

    while (this.ioThread.isAlive()) {
      try {
        this.ioThread.join();
      } catch (final InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void signalPromptSeen() {
    synchronized (this.promptSeen) {
      this.promptSeen.notifyAll();
    }
  }

  void waitForPrompt() {
    // Wait for the io thread to start up.
    while (this.promptSeen == null) {
      try {
        Thread.sleep(50);
      } catch (final InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }

    // Wait for the prompt seen signal.
    while (true) {
      try {
        synchronized (this.promptSeen) {
          this.promptSeen.wait();
        }

        break;
      } catch (final InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
