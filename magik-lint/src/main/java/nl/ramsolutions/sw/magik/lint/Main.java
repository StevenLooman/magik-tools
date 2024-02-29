package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.logging.LogManager;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.magik.lint.output.MessageFormatReporter;
import nl.ramsolutions.sw.magik.lint.output.NullReporter;
import nl.ramsolutions.sw.magik.lint.output.Reporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.UnrecognizedOptionException;

/** Main entry point for magik linter. */
public final class Main {

  private static final Options OPTIONS;
  private static final Option OPTION_MSG_TEMPLATE =
      Option.builder()
          .longOpt("msg-template")
          .desc("Output pattern")
          .hasArg()
          .type(PatternOptionBuilder.STRING_VALUE)
          .build();
  private static final Option OPTION_RCFILE =
      Option.builder()
          .longOpt("rcfile")
          .desc("Configuration file")
          .hasArg()
          .type(PatternOptionBuilder.FILE_VALUE)
          .build();
  private static final Option OPTION_SHOW_CHECKS =
      Option.builder().longOpt("show-checks").desc("Show checks and exit").build();
  private static final Option OPTION_COLUMN_OFFSET =
      Option.builder()
          .longOpt("column-offset")
          .desc("Set column offset, positive or negative")
          .hasArg()
          .type(PatternOptionBuilder.NUMBER_VALUE)
          .build();
  private static final Option OPTION_MAX_INFRACTIONS =
      Option.builder()
          .longOpt("max-infractions")
          .desc("Set max number of reporter infractions")
          .hasArg()
          .type(PatternOptionBuilder.NUMBER_VALUE)
          .build();
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Enable showing of debug information").build();
  private static final Option OPTION_VERSION =
      Option.builder().longOpt("version").desc("Show version and exit").build();
  private static final Option OPTION_HELP =
      Option.builder().longOpt("help").desc("Show this help and exit").build();
  private static final Option OPTION_APPLY_FIXES =
      Option.builder().longOpt("apply-fixes").desc("Apply fixes automatically").build();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_HELP);
    OPTIONS.addOption(OPTION_MSG_TEMPLATE);
    OPTIONS.addOption(OPTION_RCFILE);
    OPTIONS.addOption(OPTION_SHOW_CHECKS);
    OPTIONS.addOption(OPTION_COLUMN_OFFSET);
    OPTIONS.addOption(OPTION_MAX_INFRACTIONS);
    OPTIONS.addOption(OPTION_DEBUG);
    OPTIONS.addOption(OPTION_VERSION);
    OPTIONS.addOption(OPTION_APPLY_FIXES);
  }

  private static final Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING =
      Map.of(
          "Critical", 2,
          "Major", 4,
          "Minor", 8);

  private Main() {}

  private static PrintStream getOutStream() {
    return System.out; // NOSONAR
  }

  private static PrintStream getErrStream() {
    return System.err; // NOSONAR
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

  /** Initialize logger from logging.properties. */
  private static void initDebugLogger() throws IOException {
    final InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("debug-logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Create the reporter.
   *
   * @param configuration Configuration.
   * @return Reporter.
   */
  private static Reporter createReporter(final MagikLintConfiguration configuration) {
    final String configReporterFormat = configuration.getReporterFormat();
    final String format =
        configReporterFormat != null ? configReporterFormat : MessageFormatReporter.DEFAULT_FORMAT;

    final Long columnOffset = configuration.getColumnOffset();

    final PrintStream outStream = Main.getOutStream();
    return new MessageFormatReporter(outStream, format, columnOffset);
  }

  /**
   * Main entry point.
   *
   * @param args Arguments.
   * @throws IOException -
   * @throws ParseException -
   * @throws ReflectiveOperationException -
   */
  public static void main(final String[] args)
      throws ParseException, IOException, ReflectiveOperationException {
    final CommandLine commandLine;
    try {
      commandLine = Main.parseCommandline(args);
    } catch (UnrecognizedOptionException exception) {
      final PrintStream errStream = Main.getErrStream();
      errStream.println("Unrecognized option: " + exception.getMessage());

      System.exit(1);
      return; // Keep inferer happy.
    }

    if (commandLine.hasOption(OPTION_DEBUG)) {
      Main.initDebugLogger();
    }

    if (commandLine.hasOption(OPTION_VERSION)) {
      final String version = Main.class.getPackage().getImplementationVersion();
      final PrintStream errStream = Main.getErrStream();
      errStream.println("Version: " + version);
      System.exit(0);
    }

    // Read configuration.
    final MagikLintConfiguration config;
    if (commandLine.hasOption(OPTION_RCFILE)) {
      final File rcfile = (File) commandLine.getParsedOptionValue(OPTION_RCFILE);
      final Path path = rcfile.toPath();
      if (!Files.exists(path)) {
        final PrintStream errStream = Main.getErrStream();
        errStream.println("RC File does not exist: " + path);

        System.exit(1);
      }
      config = new MagikLintConfiguration(path);
    } else {
      final Path currentWorkingPath = Path.of("");
      final Path path = ConfigurationLocator.locateConfiguration(currentWorkingPath);
      config = path != null ? new MagikLintConfiguration(path) : new MagikLintConfiguration();
    }

    // Copy configuration from command line.
    Main.copyOptionsToConfig(commandLine, config);

    // Show checks.
    if (commandLine.hasOption(OPTION_SHOW_CHECKS)) {
      final Reporter reporter = new NullReporter();
      final MagikLint lint = new MagikLint(config, reporter);
      final PrintStream outStream = Main.getOutStream();
      final Writer writer = new PrintWriter(outStream);
      lint.showEnabledChecks(writer);
      lint.showDisabledChecks(writer);
      writer.flush();
      System.exit(0);
    }

    // Help.
    if (commandLine.hasOption(OPTION_HELP) || commandLine.getArgs().length == 0) {
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("magik-lint", Main.OPTIONS);

      System.exit(0);
    }

    // Apply fixes.
    final String[] leftOverArgs = commandLine.getArgs();
    final Collection<Path> paths = MagikFileScanner.getFilesFromArgs(leftOverArgs);
    if (commandLine.hasOption(OPTION_APPLY_FIXES)) {
      final MagikFixer fixer = new MagikFixer(config);
      fixer.run(paths);

      System.exit(0);
    }

    // Actual linting.
    final Reporter reporter = Main.createReporter(config);
    final MagikLint lint = new MagikLint(config, reporter);
    lint.run(paths);

    final int exitCode =
        reporter.reportedSeverities().stream()
            .map(Main.SEVERITY_EXIT_CODE_MAPPING::get)
            .reduce(0, (partial, sum) -> sum | partial);
    System.exit(exitCode);
  }

  private static void copyOptionsToConfig(
      final CommandLine commandLine, final MagikLintConfiguration config) {
    if (commandLine.hasOption(OPTION_MAX_INFRACTIONS)) {
      final String value = commandLine.getOptionValue(OPTION_MAX_INFRACTIONS);
      final Long maxInfractions = Long.parseLong(value);
      config.setMaxInfractions(maxInfractions);
    }

    if (commandLine.hasOption(OPTION_COLUMN_OFFSET)) {
      final String value = commandLine.getOptionValue(OPTION_COLUMN_OFFSET);
      final Long maxInfractions = Long.parseLong(value);
      config.setColumnOffset(maxInfractions);
    }

    if (commandLine.hasOption(OPTION_MSG_TEMPLATE)) {
      final String value = commandLine.getOptionValue(OPTION_MSG_TEMPLATE);
      config.setReporterFormat(value);
    }
  }
}
