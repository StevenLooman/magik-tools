package nl.ramsolutions.sw.magik.sessionwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.logging.LogManager;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.PrintAboveWriter;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Main entry point for magik session wrapper. */
public class Main {

  /** Initialize logger from debug-logging.properties. */
  private static void initDebugLogger() throws IOException {
    final ClassLoader classLoader = Main.class.getClassLoader();
    final InputStream stream = classLoader.getResourceAsStream("debug-logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Main entry point.
   *
   * @param args Arguments.
   * @throws IOException -
   */
  public static void main(final String[] args) throws IOException {
    Main.initDebugLogger();

    // TODO: Make history file configurable.
    final Path historyFile = Path.of(System.getProperty("user.home"), ".magik_history");
    // TODO: Make Smallworld core path configurable.
    final Path smallworldCorePath = Path.of("/opt/Smallworld5.3.0.0/core");
    // TODO: Make alias entry configurable.
    final String aliasEntry = "base";

    final Terminal terminal = TerminalBuilder.builder().system(true).build();
    final DefaultHistory history = new DefaultHistory();
    final LineReader lineReader =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .variable(LineReader.HISTORY_FILE, historyFile)
            .history(history)
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
            .variable(LineReader.INDENTATION, 2)
            .build();

    final PrintWriter wrapperWriter = new PrintWriter(new PrintAboveWriter(lineReader), true);

    final SmallworldSession session =
        new SmallworldSessionBuilder()
            .withSmallworldProductDir(smallworldCorePath)
            .withOutputWriter(wrapperWriter)
            .withJavaArgument("-Djava.awt.headless=true")
            .withAliasesEntry(aliasEntry)
            .build();

    try {
      final PrintWriter sessionWriter = session.getSessionWriter();
      while (session.isAlive()) {
        // Wait for prompt from Smallworld session.
        session.waitForPrompt();

        // Get input from user.
        final String userInput = lineReader.readLine("WrapperMagik> ");

        // Send input to Smallworld session.
        sessionWriter.println(userInput);
      }
    } catch (final EndOfFileException | UserInterruptException exception) {
      wrapperWriter.println("Exiting...");
    } finally {
      session.destroy();
      history.save();

      wrapperWriter.close();
      terminal.close();
    }
  }
}
