package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.checks.output.MessageFormatReporter;
import nl.ramsolutions.sw.checks.output.NullReporter;
import nl.ramsolutions.sw.checks.output.Reporter;
import nl.ramsolutions.sw.checks.output.ReporterContext;
import nl.ramsolutions.sw.checks.output.ReporterRegistry;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.cli.help.HelpFormatter;

/** Main entry point for magik linter. */
public final class Main {

  private static final Options OPTIONS;
  private static final Option OPTION_MSG_TEMPLATE =
      Option.builder()
          .longOpt("msg-template")
          .desc("Output pattern")
          .hasArg()
          .type(PatternOptionBuilder.STRING_VALUE)
          .get();
  private static final Option OPTION_RCFILE =
      Option.builder()
          .longOpt("rcfile")
          .desc("Configuration file")
          .hasArg()
          .type(PatternOptionBuilder.FILE_VALUE)
          .get();
  private static final Option OPTION_SHOW_CHECKS =
      Option.builder().longOpt("show-checks").desc("Show checks and exit").get();
  private static final Option OPTION_COLUMN_OFFSET =
      Option.builder()
          .longOpt("column-offset")
          .desc("Set column offset, positive or negative")
          .hasArg()
          .type(PatternOptionBuilder.NUMBER_VALUE)
          .get();
  private static final Option OPTION_MAX_INFRACTIONS =
      Option.builder()
          .longOpt("max-infractions")
          .desc("Set max number of reporter infractions")
          .hasArg()
          .type(PatternOptionBuilder.NUMBER_VALUE)
          .get();
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Enable showing of debug information").get();
  private static final Option OPTION_VERSION =
      Option.builder().longOpt("version").desc("Show version and exit").get();
  private static final Option OPTION_HELP =
      Option.builder().longOpt("help").desc("Show this help and exit").get();
  private static final Option OPTION_APPLY_FIXES =
      Option.builder().longOpt("apply-fixes").desc("Apply fixes automatically").get();
  private static final Option OPTION_FORMAT =
      Option.builder()
          .longOpt("format")
          .desc("Output format: text (default)")
          .hasArg()
          .type(PatternOptionBuilder.STRING_VALUE)
          .get();

  static {
    ReporterRegistry.register(
        "text",
        (final MagikToolsProperties properties, final ReporterContext context) -> {
          final String configReporterFormat =
              properties.getPropertyString(MagikLint.KEY_MSG_TEMPLATE);
          final String format =
              configReporterFormat != null
                  ? configReporterFormat
                  : MessageFormatReporter.DEFAULT_FORMAT;
          final long columnOffset = properties.getPropertyLong(MagikLint.KEY_COLUMN_OFFSET, 0L);
          return new MessageFormatReporter(context.getOutStream(), format, columnOffset);
        });
    ReporterRegistry.register(
        "null",
        (final MagikToolsProperties properties, final ReporterContext context) ->
            new NullReporter());

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
    OPTIONS.addOption(OPTION_FORMAT);
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

  private static String getName() {
    return "magik-lint";
  }

  private static String getArtifactName() {
    return Main.getName() + ".jar";
  }

  private static String getVersion() {
    return Main.class.getPackage().getImplementationVersion();
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
   * Ensure the output format is known, otherwise report and exit.
   *
   * @param outputFormat Output format.
   */
  private static void ensureKnownFormat(final String outputFormat) {
    if (ReporterRegistry.hasFormat(outputFormat)) {
      return;
    }

    final PrintStream errStream = Main.getErrStream();
    final String validFormats =
        ReporterRegistry.getFormats().stream()
            .filter(format -> !"null".equals(format))
            .collect(Collectors.joining(", "));
    errStream.println(
        "Unknown output format: " + outputFormat + ". Valid formats: " + validFormats);

    System.exit(1);
  }

  /**
   * Create the reporter.
   *
   * @param commandLine Parsed command line.
   * @param properties Configuration.
   * @return Reporter.
   */
  private static Reporter createReporter(
      final CommandLine commandLine, final MagikToolsProperties properties) {
    final String outputFormat = commandLine.getOptionValue(OPTION_FORMAT, "text");
    final String normalizedFormat = outputFormat != null ? outputFormat : "text";
    final PrintStream outStream = Main.getOutStream();
    final String toolVersion = Main.getVersion() != null ? Main.getVersion() : "dev";
    final List<Class<? extends Check>> checkClasses = getAllCheckClasses();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checkClasses, properties);
    final List<CheckHolder> checkHolders = checksConfig.getAllChecks();
    final ReporterContext context =
        new ReporterContext(outStream, Main.getName(), toolVersion, checkHolders);
    return ReporterRegistry.createReporter(normalizedFormat, properties, context);
  }

  private static List<Class<? extends Check>> getAllCheckClasses() {
    return Stream.of(
            ProductDefCheckList.INSTANCE.getBaseChecks().stream(),
            ModuleDefCheckList.INSTANCE.getBaseChecks().stream(),
            MagikCheckList.INSTANCE.getBaseChecks().stream())
        .flatMap(stream -> stream)
        .sorted(Comparator.comparing(Class::getSimpleName))
        .toList();
  }

  private static Collection<Path> getFilesFromArgs(final String[] args) throws IOException {
    final Collection<Path> paths = new ArrayList<>();

    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final SourceFileScanner scanner =
        new SourceFileScanner(ignoreHandler, SourceFileScanner.ANY_MAGIK_RELATED_FILE_FILTER);
    for (final String arg : args) {
      final Path path = Path.of(arg);
      final List<Path> argPaths = scanner.getFiles(path).toList();
      paths.addAll(argPaths);
    }

    return paths;
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
    } catch (final UnrecognizedOptionException exception) {
      final PrintStream errStream = Main.getErrStream();
      final String artifactName = Main.getArtifactName();
      errStream.println(
          exception.getMessage()
              + "\nTry 'java -jar "
              + artifactName
              + " --help' for more information.");

      System.exit(1);
      return; // Keep inferer happy.
    }

    if (commandLine.hasOption(OPTION_DEBUG)) {
      Main.initDebugLogger();
    }

    if (commandLine.hasOption(OPTION_HELP)) {
      Main.showHelp();

      System.exit(0);
    }

    if (commandLine.hasOption(OPTION_VERSION)) {
      Main.showVersion();

      System.exit(0);
    }

    // Read configuration.
    final MagikToolsProperties properties;
    if (commandLine.hasOption(OPTION_RCFILE)) {
      final File rcfile = (File) commandLine.getParsedOptionValue(OPTION_RCFILE);
      final Path path = rcfile.toPath();
      if (!Files.exists(path)) {
        final PrintStream errStream = Main.getErrStream();
        errStream.println("RC File does not exist: " + path);

        System.exit(1);
      }
      properties = new MagikToolsProperties(path);
    } else {
      final Path currentWorkingPath = Path.of(".");
      final Path path = ConfigurationLocator.locateConfiguration(currentWorkingPath);
      properties =
          path != null ? new MagikToolsProperties(path) : MagikToolsProperties.DEFAULT_PROPERTIES;
    }

    // Copy configuration from command line.
    Main.copyOptionsToConfig(commandLine, properties);

    // Validate output format.
    Main.ensureKnownFormat(commandLine.getOptionValue(OPTION_FORMAT, "text"));

    // Show checks.
    if (commandLine.hasOption(OPTION_SHOW_CHECKS)) {
      final PrintStream outStream = Main.getOutStream();
      final String toolVersion = Main.getVersion() != null ? Main.getVersion() : "dev";
      final List<Class<? extends Check>> checkClasses = getAllCheckClasses();
      final ChecksConfiguration checksConfig = new ChecksConfiguration(checkClasses, properties);
      final List<CheckHolder> checkHolders = checksConfig.getAllChecks();
      final ReporterContext context =
          new ReporterContext(outStream, Main.getName(), toolVersion, checkHolders);
      final Reporter reporter = ReporterRegistry.createReporter("null", properties, context);
      final MagikLint lint = new MagikLint(properties, reporter);
      final Writer writer = new PrintWriter(outStream);
      lint.showEnabledChecks(writer);
      lint.showDisabledChecks(writer);
      writer.flush();
      System.exit(0);
    }

    // Apply fixes.
    final String[] leftOverArgs = commandLine.getArgs();
    final Collection<Path> paths = Main.getFilesFromArgs(leftOverArgs);
    if (commandLine.hasOption(OPTION_APPLY_FIXES)) {
      final PrintStream outStream = Main.getOutStream();
      final Writer writer = new PrintWriter(outStream);
      final MagikLintFixApplier fixer = new MagikLintFixApplier(properties, writer);
      fixer.run(paths);

      System.exit(0);
    }

    // Actual linting.
    final Reporter reporter = Main.createReporter(commandLine, properties);
    final MagikLint lint = new MagikLint(properties, reporter);
    lint.run(paths);
    reporter.finish();

    final int exitCode =
        reporter.reportedSeverities().stream()
            .map(Main.SEVERITY_EXIT_CODE_MAPPING::get)
            .reduce(0, (partial, sum) -> sum | partial);
    System.exit(exitCode);
  }

  private static void copyOptionsToConfig(
      final CommandLine commandLine, final MagikToolsProperties properties) {
    if (commandLine.hasOption(OPTION_MAX_INFRACTIONS)) {
      final String value = commandLine.getOptionValue(OPTION_MAX_INFRACTIONS);
      final Long maxInfractions = Long.parseLong(value);
      properties.setProperty(MagikLint.KEY_MAX_INFRACTIONS, maxInfractions);
    }

    if (commandLine.hasOption(OPTION_COLUMN_OFFSET)) {
      final String value = commandLine.getOptionValue(OPTION_COLUMN_OFFSET);
      final Long maxInfractions = Long.parseLong(value);
      properties.setProperty(MagikLint.KEY_COLUMN_OFFSET, maxInfractions);
    }

    if (commandLine.hasOption(OPTION_MSG_TEMPLATE)) {
      final String value = commandLine.getOptionValue(OPTION_MSG_TEMPLATE);
      properties.setProperty(MagikLint.KEY_MSG_TEMPLATE, value);
    }

    if (commandLine.hasOption(OPTION_RCFILE)) {
      final String value = commandLine.getOptionValue(OPTION_RCFILE);
      properties.setProperty(MagikLint.KEY_OVERRIDE_CONFIG, value);
    }
  }

  private static void showHelp() throws IOException {
    final HelpFormatter helpFormatter = HelpFormatter.builder().setShowSince(false).get();
    final String artifactName = Main.getArtifactName();
    final String name = Main.getName();
    helpFormatter.printHelp(
        "java -jar " + artifactName, name + "\s" + Main.getVersion(), Main.OPTIONS, "", true);
  }

  private static void showVersion() {
    final String version = Main.getVersion();
    final PrintStream outStream = Main.getOutStream();
    outStream.println(version);
  }
}
