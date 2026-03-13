package nl.ramsolutions.sw.magik.typedlint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.IgnoreHandler;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.MagikTypedCheckList;
import nl.ramsolutions.sw.checks.output.MessageFormatReporter;
import nl.ramsolutions.sw.checks.output.NullReporter;
import nl.ramsolutions.sw.checks.output.Reporter;
import nl.ramsolutions.sw.checks.output.SarifReporter;
import nl.ramsolutions.sw.magik.FileEvent;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.io.JsonDefinitionReader;
import nl.ramsolutions.sw.magik.analysis.indexer.MagikIndexer;
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
  private static final Option OPTION_TYPE_DATABASE =
      Option.builder()
          .longOpt("types-db")
          .desc("Path to types database (can be multiple)")
          .hasArg()
          .type(PatternOptionBuilder.FILE_VALUE)
          .get();
  private static final Option OPTION_PRE_INDEX_DIR =
      Option.builder()
          .longOpt("pre-index-dir")
          .desc("Pre index directory before checking (can be multiple)")
          .hasArg()
          .type(PatternOptionBuilder.FILE_VALUE)
          .get();
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Enable showing of debug information").get();
  private static final Option OPTION_VERSION =
      Option.builder().longOpt("version").desc("Show version and exit").get();
  private static final Option OPTION_HELP =
      Option.builder().longOpt("help").desc("Show this help and exit").get();
  private static final Option OPTION_FORMAT =
      Option.builder()
          .longOpt("format")
          .desc("Output format: text (default), sarif")
          .hasArg()
          .type(PatternOptionBuilder.STRING_VALUE)
          .get();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_HELP);
    OPTIONS.addOption(OPTION_MSG_TEMPLATE);
    OPTIONS.addOption(OPTION_RCFILE);
    OPTIONS.addOption(OPTION_SHOW_CHECKS);
    OPTIONS.addOption(OPTION_COLUMN_OFFSET);
    OPTIONS.addOption(OPTION_MAX_INFRACTIONS);
    OPTIONS.addOption(OPTION_TYPE_DATABASE);
    OPTIONS.addOption(OPTION_PRE_INDEX_DIR);
    OPTIONS.addOption(OPTION_DEBUG);
    OPTIONS.addOption(OPTION_VERSION);
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
   * @param properties Configuration.
   * @param outputFormat Output format ("text" or "sarif").
   * @return Reporter.
   */
  private static Reporter createReporter(
      final MagikToolsProperties properties, final String outputFormat) {
    final PrintStream outStream = Main.getOutStream();
    if ("sarif".equalsIgnoreCase(outputFormat)) {
      final List<Class<? extends Check>> checkClasses =
          MagikTypedCheckList.INSTANCE.getBaseChecks();
      final ChecksConfiguration checksConfig = new ChecksConfiguration(checkClasses, properties);
      final List<CheckHolder> checkHolders = checksConfig.getAllChecks();
      final String version = Main.class.getPackage().getImplementationVersion();
      return new SarifReporter(
          outStream, "magik-typed-lint", version != null ? version : "dev", checkHolders);
    }
    final String configReporterFormat =
        properties.getPropertyString(MagikTypedLint.KEY_MSG_TEMPLATE);
    final String format =
        configReporterFormat != null ? configReporterFormat : MessageFormatReporter.DEFAULT_FORMAT;
    final long columnOffset = properties.getPropertyLong(MagikTypedLint.KEY_COLUMN_OFFSET, 0L);
    return new MessageFormatReporter(outStream, format, columnOffset);
  }

  private static Collection<Path> getFilesFromArgs(final String[] args) throws IOException {
    final Collection<Path> paths = new ArrayList<>();

    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final SourceFileScanner scanner =
        new SourceFileScanner(ignoreHandler, SourceFileScanner.MAGIK_FILE_FILTER);
    for (final String arg : args) {
      final Path path = Path.of(arg);
      final List<Path> argPaths = scanner.getFiles(path).toList();
      paths.addAll(argPaths);
    }

    return paths;
  }

  private static void readTypeDatabases(
      final String[] typeDatabasePaths, final IDefinitionKeeper definitionKeeper)
      throws IOException {
    for (final String typeDatabasePath : typeDatabasePaths) {
      final Path path = Path.of(typeDatabasePath);
      JsonDefinitionReader.readTypes(path, definitionKeeper);
    }
  }

  private static void indexPaths(
      final String[] indexDirs,
      final MagikToolsProperties properties,
      final IDefinitionKeeper definitionKeeper)
      throws IOException {
    final IgnoreHandler ignoreHandler = new IgnoreHandler();
    final MagikIndexer magikIndexer = new MagikIndexer(definitionKeeper, properties, ignoreHandler);
    for (final String indexDir : indexDirs) {
      final Path path = Path.of(indexDir).toAbsolutePath();
      final URI uri = path.toUri();
      final FileEvent fileEvent = new FileEvent(uri, FileEvent.FileChangeType.CREATED);
      magikIndexer.handleFileEvent(fileEvent);
    }
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

    // Show checks.
    final IDefinitionKeeper definitionKeeper = new DefinitionKeeper();
    if (commandLine.hasOption(OPTION_SHOW_CHECKS)) {
      final Reporter reporter = new NullReporter();
      final MagikTypedLint lint = new MagikTypedLint(definitionKeeper, properties, reporter);
      final PrintStream outStream = Main.getOutStream();
      final Writer writer = new PrintWriter(outStream);
      lint.showEnabledChecks(writer);
      lint.showDisabledChecks(writer);
      writer.flush();
      System.exit(0);
    }

    // Help.
    if (commandLine.hasOption(OPTION_HELP) || commandLine.getArgs().length == 0) {
      final HelpFormatter helpFormatter = HelpFormatter.builder().setShowSince(false).get();
      helpFormatter.printHelp(
          "java -jar magik-typed-lint.jar", "magik-typed-lint", Main.OPTIONS, "", true);

      System.exit(0);
    }

    // Read type database(s).
    if (commandLine.hasOption(OPTION_TYPE_DATABASE)) {
      final String[] typeDatabasePaths = commandLine.getOptionValues(OPTION_TYPE_DATABASE);
      Main.readTypeDatabases(typeDatabasePaths, definitionKeeper);
    }

    // Pre-index directory/directories.
    if (commandLine.hasOption(OPTION_PRE_INDEX_DIR)) {
      final String[] indexDirs = commandLine.getOptionValues(OPTION_PRE_INDEX_DIR);
      Main.indexPaths(indexDirs, properties, definitionKeeper);
    }

    // Index files from command line.
    final String[] leftOverArgs = commandLine.getArgs();
    Main.indexPaths(leftOverArgs, properties, definitionKeeper);

    // Lint files from command line.
    final Collection<Path> paths = Main.getFilesFromArgs(leftOverArgs);
    final String outputFormat =
        commandLine.hasOption(OPTION_FORMAT) ? commandLine.getOptionValue(OPTION_FORMAT) : "text";
    final Reporter reporter = Main.createReporter(properties, outputFormat);
    final MagikTypedLint lint = new MagikTypedLint(definitionKeeper, properties, reporter);
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
      properties.setProperty(MagikTypedLint.KEY_MAX_INFRACTIONS, maxInfractions);
    }

    if (commandLine.hasOption(OPTION_COLUMN_OFFSET)) {
      final String value = commandLine.getOptionValue(OPTION_COLUMN_OFFSET);
      final Long maxInfractions = Long.parseLong(value);
      properties.setProperty(MagikTypedLint.KEY_COLUMN_OFFSET, maxInfractions);
    }

    if (commandLine.hasOption(OPTION_MSG_TEMPLATE)) {
      final String value = commandLine.getOptionValue(OPTION_MSG_TEMPLATE);
      properties.setProperty(MagikTypedLint.KEY_MSG_TEMPLATE, value);
    }

    if (commandLine.hasOption(OPTION_RCFILE)) {
      final String value = commandLine.getOptionValue(OPTION_RCFILE);
      properties.setProperty(MagikTypedLint.KEY_OVERRIDE_CONFIG, value);
    }
  }
}
