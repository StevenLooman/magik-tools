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

  private static final String SESSION_OUTPUT_THREAD_NAME = "SessionOutputThread";
  private static final Pattern GLOBAL_DOES_NOT_EXIST_PATTERN =
      Pattern.compile("Global .* does not exist: create it\\? \\(Y\\) ");
  private static final Pattern PLEASE_ANSWER_Y_OR_N =
      Pattern.compile("Please answer y or n \\(Y\\) ");
  static final Pattern DEFAULT_PROMPT_PATTERN = Pattern.compile("^(Magik|MagikSF)> $");

  private static final Logger LOGGER = LoggerFactory.getLogger(SmallworldSession.class);
  private static final Charset SESSION_CHARSET = StandardCharsets.ISO_8859_1;

  private final Process process;
  private final Pattern promptPattern;
  private final Thread ioThread;
  private final Thread mainThread = Thread.currentThread();
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

    this.ioThread =
        new Thread(() -> this.sessionOutputThread(outputWriter), SESSION_OUTPUT_THREAD_NAME);
    this.ioThread.start();

    this.process
        .onExit()
        .thenAccept(
            p -> {
              LOGGER.debug("Smallworld session exited with code: {}", p);

              // Interrupt the main thread.
              this.mainThread.interrupt();
            });
  }

  private void sessionOutputThread(final PrintWriter outputWriter) {
    this.promptSeen = new Object();

    final BufferedReader sessionReader = this.getSessionReader();
    final CharBuffer charBuffer = CharBuffer.allocate(1024);
    try {
      while (this.process.isAlive()) {
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

    LOGGER.debug("Session output reader thread ended");
  }

  private void processSessionOutput(final PrintWriter outputWriter, final String str) {
    // If prompt is seen, don't print it, but do signal it was seen.
    if (this.promptPattern.matcher(str).matches()) {
      this.signalPromptSeen();

      return;
    }

    // Work around `Global ... does not exist: create it?` questions on prompt,
    // not ending with newline.
    // NOTE: This is a hack, and only works in the case of undefined globals, but not for:
    // - terminal.prompt_for_input()
    // - terminal.prompt_y_or_n()
    // - terminal.prompt_brief_choice()
    // - terminal.prompt_long_choice()
    final String strFixed;
    if (this.matchesSessionQuestion(str)) {
      strFixed = str + "\n";

      this.signalPromptSeen();
    } else {
      strFixed = str;
    }

    // Write session output to output writer.
    outputWriter.print(strFixed);
    outputWriter.flush();
  }

  private boolean matchesSessionQuestion(final String str) {
    return GLOBAL_DOES_NOT_EXIST_PATTERN.matcher(str).matches()
        || PLEASE_ANSWER_Y_OR_N.matcher(str).matches();
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

  boolean waitForPrompt() {
    // Wait for the io thread to start.
    while (this.promptSeen == null) {
      try {
        Thread.sleep(50);
      } catch (final InterruptedException exception) {
        return true;
      }
    }

    // Wait for the prompt seen signal.
    LOGGER.debug("Waiting for prompt...");
    while (this.process.isAlive()) {
      try {
        synchronized (this.promptSeen) {
          this.promptSeen.wait();
        }

        LOGGER.debug("Got prompt signal");
        break;
      } catch (final InterruptedException exception) {
        return true;
      }
    }

    return false;
  }
}
