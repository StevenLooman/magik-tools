package nl.ramsolutions.sw.magik.sessionwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.LogManager;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.PrintAboveWriter;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main entry point for magik session wrapper. */
public class Main {

  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final Options OPTIONS;
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Show debug messages").build();
  private static final Option OPTION_HISTORY_FILE =
      Option.builder()
          .longOpt("history-file")
          .desc("Path to history file")
          .hasArg()
          .type(PatternOptionBuilder.FILE_VALUE)
          .build();
  private static final Option OPTION_DO_NOT_WAIT_FOR_PROMPT =
      Option.builder()
          .longOpt("do-not-wait-for-prompt")
          .desc("Do not wait for Magik prompt")
          .build();
  private static final Option OPTION_PROMPT_PATTERN =
      Option.builder()
          .longOpt("prompt-pattern")
          .desc("Prompt pattern (regex)")
          .hasArg()
          .type(PatternOptionBuilder.STRING_VALUE)
          .build();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_DEBUG);
    OPTIONS.addOption(OPTION_HISTORY_FILE);
    OPTIONS.addOption(OPTION_DO_NOT_WAIT_FOR_PROMPT);
    OPTIONS.addOption(OPTION_PROMPT_PATTERN);
  }

  private Main() {}

  /**
   * Initialize logger from logging.properties.
   *
   * @throws IOException -
   */
  private static void initLogger() throws IOException {
    final InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Initialize logger from debug-logging.properties.
   *
   * @throws IOException -
   */
  private static void initDebugLogger() throws IOException {
    final InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("debug-logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Parse the command line.
   *
   * @param args Command line arguments.
   * @return Parsed command line.
   * @throws ParseException -
   */
  private static CommandLine parseCommandline(final String[] args) throws ParseException {
    final CommandLineParser parser = new DefaultParser();
    return parser.parse(Main.OPTIONS, args);
  }

  /**
   * Main entry point.
   *
   * @param args Arguments.
   * @throws IOException -
   * @throws ParseException -
   */
  public static void main(final String[] args) throws IOException, ParseException {
    final CommandLine commandLine = Main.parseCommandline(args);
    if (commandLine.hasOption(OPTION_DEBUG)) {
      Main.initDebugLogger();
    } else {
      Main.initLogger();
    }

    // History file.
    final String historyFilePath;
    if (commandLine.hasOption(OPTION_HISTORY_FILE)) {
      historyFilePath = commandLine.getOptionValue(OPTION_HISTORY_FILE);
    } else {
      historyFilePath = Path.of(System.getProperty("user.home"), ".magik_history").toString();
    }
    System.setProperty("jline.history", historyFilePath);

    // History file.
    final boolean waitForPrompt = !commandLine.hasOption(OPTION_DO_NOT_WAIT_FOR_PROMPT);

    // Prompt pattern.
    final Pattern promptPattern;
    if (commandLine.hasOption(OPTION_DO_NOT_WAIT_FOR_PROMPT)) {
      final String promptPatternStr = commandLine.getOptionValue(OPTION_PROMPT_PATTERN);
      promptPattern = Pattern.compile(promptPatternStr);
    } else {
      promptPattern = SmallworldSession.DEFAULT_PROMPT_PATTERN;
    }

    final Terminal terminal = TerminalBuilder.builder().system(true).build();
    final DefaultHistory history = new DefaultHistory();
    final Parser parser = new MagikJlineParser();
    final Completer completer = new MagikJlineCompleter();
    final Highlighter highlighter = new MagikJlineHighlighter();
    final LineReader lineReader =
        LineReaderBuilder.builder()
            .terminal(terminal)
            .history(history)
            .parser(parser)
            .completer(completer)
            .highlighter(highlighter)
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
            .variable(LineReader.INDENTATION, 2)
            .variable(LineReader.HISTORY_FILE, historyFilePath)
            .build();

    final PrintAboveWriter printAboveWriter = new PrintAboveWriter(lineReader);
    final PrintWriter wrapperWriter = new PrintWriter(printAboveWriter, true);

    final List<String> runAliasCommand = commandLine.getArgList();
    final SmallworldSession session =
        new SmallworldSessionLauncher(runAliasCommand, wrapperWriter, promptPattern).launch();

    try {
      final PrintWriter sessionWriter = session.getSessionWriter();
      while (session.isAlive()) {
        // Wait for prompt from Smallworld session.
        if (waitForPrompt) {
          session.waitForPrompt();
        }

        // Get input from user.
        final String userInput = lineReader.readLine("WrapperMagik> ");

        // Send input to Smallworld session.
        sessionWriter.println(userInput);
      }
    } catch (final EndOfFileException | UserInterruptException exception) {
      LOGGER.debug("Exiting...");
    } finally {
      history.save();

      session.destroy();
      wrapperWriter.close();
      terminal.close();
    }
  }
}
