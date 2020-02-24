package org.stevenlooman.sw.magik.lint;

import com.sonar.sslr.api.AstNode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.lint.output.MessageFormatReporter;
import org.stevenlooman.sw.magik.lint.output.Reporter;
import org.stevenlooman.sw.magik.parser.FileCharsetDeterminer;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MagikLint {

  static Logger logger = Logger.getLogger(MagikLint.class.getName());

  CommandLine commandLine;
  Configuration config;
  static final Options OPTIONS;

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(Option.builder()
        .longOpt("help")
        .desc("Show this help")
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("msg-template")
        .desc("Output pattern")
        .hasArg()
        .type(PatternOptionBuilder.STRING_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("rcfile")
        .desc("Configuration file")
        .hasArg()
        .type(PatternOptionBuilder.EXISTING_FILE_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("show-checks")
        .desc("Show checks and quit")
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("untabify")
        .desc("Expand tabs to N spaces")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("column-offset")
        .desc("Set column offset, positive or negative")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("watch")
        .desc("Watch the given directory/file for changes")
        .hasArg()
        .type(PatternOptionBuilder.EXISTING_FILE_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("max-infractions")
        .desc("Set max number of reporter infractions")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build());
    OPTIONS.addOption(Option.builder()
        .longOpt("debug")
        .desc("Enable showing of debug information")
        .build());
  }

  static final Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING = new HashMap<>();

  static {
    SEVERITY_EXIT_CODE_MAPPING.put("Major", 2);
    SEVERITY_EXIT_CODE_MAPPING.put("Minor", 4);
  }

  /**
   * Initialize logger from logging.properties.
   */
  private void initLogger() {
    InputStream stream = MagikLint.class.getClassLoader().getResourceAsStream("logging.properties");
    try {
      LogManager.getLogManager().readConfiguration(stream);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Constructor, parses command line and reads configuration.
   * @param args Command line arguments.
   * @throws ParseException -
   */
  MagikLint(String[] args) throws ParseException {
    commandLine = parseCommandline(args);

    if (commandLine.hasOption("debug")) {
      initLogger();
      logger.fine("enabled debugging information");
    }

    // read configuration
    if (commandLine.hasOption("rcfile")) {
      File rcfile = (File) commandLine.getParsedOptionValue("rcfile");
      Path path = rcfile.toPath();
      config = new Configuration(path);
    } else {
      Path path = ConfigurationLocator.locateConfiguration();
      if (path != null) {
        config = new Configuration(path);
      } else {
        config = new Configuration();
      }
    }
  }

  /**
   * Parse the command line.
   * @param args Command line arguments.
   * @return Parsed command line.
   * @throws ParseException -
   */
  private CommandLine parseCommandline(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(MagikLint.OPTIONS, args);
    return cmd;
  }

  /**
   * Get reporter. If the option `msg-template` is given, use a
   * MessageFormatReporter with the given template. Otherwise use
   * MessageFormatReporter.
   *
   * @return Reporter
   */
  public Reporter getReporter() throws ParseException {
    Long columnOffset = (Long) commandLine.getParsedOptionValue("column-offset");

    String template = MessageFormatReporter.DEFAULT_FORMAT;
    if (commandLine.hasOption("msg-template")) {
      template = commandLine.getOptionValue("msg-template");
    }
    return new MessageFormatReporter(System.out, template, columnOffset);
  }

  /**
   * Build context for a file, untabifying if needed.
   * @param path Path to file
   * @param untabify Untabify to N-spaces, if given
   * @return Visitor context for file.
   */
  private MagikVisitorContext buildContext(Path path, Long untabify) {
    Charset defaultCharset = Charset.forName("ISO_8859_1");
    Charset charset = FileCharsetDeterminer.determineCharset(path, defaultCharset);

    byte[] encoded = null;
    try {
      encoded = Files.readAllBytes(path);
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    String fileContents = new String(encoded, charset);
    if (untabify != null) {
      String spaces = String.format("%" + untabify + "s", "");
      fileContents = fileContents.replaceAll("\t", spaces);
    }

    MagikParser parser = new MagikParser(charset);
    AstNode root = parser.parseSafe(fileContents);

    return new MagikVisitorContext(path, fileContents, root);
  }

  /**
   * Run a single check on context.
   * @param context Context to run check on.
   * @param checkInfo CheckInfo (Check) to run.
   * @return Issues/infractions found.
   */
  private List<MagikIssue> runCheck(MagikVisitorContext context, CheckInfo checkInfo) {
    MagikCheck check = checkInfo.getCheck();
    synchronized (check) {
      List<MagikIssue> checkIssues = check.scanFileForIssues(context);
      return checkIssues;
    }
  }

  /**
   * Get the return code according to this infraction.
   *
   * @param checkInfraction Infraction to get return code for.
   * @return Return code for the infraction.
   * @throws FileNotFoundException Thrown in case CheckInfo cannot find data.
   */
  public static int getReturnCode(CheckInfraction checkInfraction) {
    CheckInfo checkInfo = checkInfraction.getCheckInfo();
    String checkSeverity = null;
    try {
      checkSeverity = checkInfo.getSeverity();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    }
    return SEVERITY_EXIT_CODE_MAPPING.getOrDefault(checkSeverity, 0);
  }

  /**
   * Show checks active and inactive checks.
   * @param checkInfos CheckInfos to show.
   */
  private void showChecks(Iterable<CheckInfo> checkInfos)
      throws IllegalAccessException, FileNotFoundException {
    for (CheckInfo checkInfo : checkInfos) {
      String name = checkInfo.getSqKey();
      if (checkInfo.isEnabled()) {
        System.out.println("Check: " + name + " (" + checkInfo.getTitle() + ")");
      } else {
        System.out.println("Check: " + name + " (disabled) (" + checkInfo.getTitle() + ")");
      }

      checkInfo.getParameters().forEach(parameterInfo -> {
        System.out.println("\t"
            + parameterInfo.getName() + ":\t"
            + parameterInfo.getValue() + " "
            + "(" + parameterInfo.getDescription() + ")");
      });
    }
  }

  /**
   * Check if a found issue/infraction is disabled via line or scope.
   * @param magikIssue Issue to check.
   * @param instructionsHandler Instruction handler to use.
   * @return true if issue is disabled at line.
   */
  private boolean isMagikIssueDisabled(
      MagikIssue magikIssue, InstructionsHandler instructionsHandler) {
    int line = magikIssue.line();
    int column = magikIssue.column();
    String checkKey = magikIssue.check().getCheckKeyKebabCase();

    Map<String, String> scopeInstructions =
        instructionsHandler.getInstructionsInScope(line, column);
    Map<String, String> lineInstructions =
        instructionsHandler.getInstructionsAtLine(line);
    String[] scopeDisableds = scopeInstructions.getOrDefault("disable", "").split(",");
    String[] lineDisableds = lineInstructions.getOrDefault("disable", "").split(",");
    return Arrays.asList(scopeDisableds).contains(checkKey)
           || Arrays.asList(lineDisableds).contains(checkKey);
  }

  /**
   * Run {{CheckInfo}}s on {{Path}}.
   * @param path File to run on.
   * @param checkInfos {{CheckInfo}}s to run.
   * @return List of {{CheckInfraction}}s for the given file.
   */
  private List<CheckInfraction> runChecksOnFile(
      Path path, Long untabify, Iterable<CheckInfo> checkInfos) {
    logger.finest("Thread: " + Thread.currentThread().getName() + ", checking file: " + path);

    MagikVisitorContext context = buildContext(path, untabify);
    InstructionsHandler instructionsHandler = new InstructionsHandler(context);
    List<CheckInfraction> infractions = new ArrayList<>();

    // run checks on files
    for (CheckInfo checkInfo : checkInfos) {
      if (!checkInfo.isEnabled()) {
        continue;
      }

      List<MagikIssue> magikIssues = runCheck(context, checkInfo);
      List<CheckInfraction> checkInfractions = magikIssues.stream()
          .filter(magikIssue -> !isMagikIssueDisabled(magikIssue, instructionsHandler))
          .map(magikIssue -> new CheckInfraction(path, checkInfo, magikIssue))
          .collect(Collectors.toList());

      infractions.addAll(checkInfractions);
    }
    return infractions;
  }

  /**
   * Watch the given directory/file for changes.
   * 
   * @param dir Path (directory) to watch.
   * @param checkInfos Checks to run.
   * @param untabify Replace tabs with N-spaces, if not null.
   * @param maxInfractions Maximum number of infractions to report.
   * @param reporter Reporter to use to report infractions.
   * @throws InterruptedException -
   * @throws IOException -
   */
  private void watch(
      Path dir,
      Iterable<CheckInfo> checkInfos,
      Long untabify,
      long maxInfractions,
      Reporter reporter)
      throws InterruptedException, IOException {
    MagikFileWatcher.ChangesListener listener = new MagikFileWatcher.ChangesListener() {
      @Override
      void onChanged(Collection<Path> paths) {
        try {
          System.out.println("File change detected. Scanning files...");
          runChecks(paths, checkInfos, untabify, Long.MAX_VALUE, reporter);
          System.out.println("Scanning complete. Watching for file changes.");
        } catch (IOException ex) {
          System.out.println("Caught exception: " + ex.getMessage());
        }
      }
    };

    // Initial file scan.
    logger.finest("Doing initial scan of: " + dir);
    Collection<Path> paths = MagikFileScanner.scanMagikFiles(dir);
    listener.onChanged(paths);

    // Continuous scanning for changes.
    logger.finest("Starting to watch: " + dir);
    MagikFileWatcher watcher = new MagikFileWatcher(dir, listener);
    watcher.run();
  }

  /**
   * Run checks on files.
   * @param paths {{Path}}s to check.
   * @param checkInfos {{CheckInfo}}s to check on each path.
   * @param untabify Replace tabs with N-spaces, if not null.
   * @param maxInfractions Maximum number of infractions to report.
   * @param reporter Reporter to use for reporting.
   * @return Exit code for process.
   */
  private int runChecks(
      Collection<Path> paths,
      Iterable<CheckInfo> checkInfos,
      Long untabify,
      long maxInfractions,
      Reporter reporter)
      throws IOException {
    Comparator<CheckInfraction> byPath =
        Comparator.comparing(ci -> ci.getPath());
    Comparator<CheckInfraction> byLine =
        Comparator.comparing(ci -> ci.getMagikIssue().line());
    Comparator<CheckInfraction> byColumn =
        Comparator.comparing(ci -> ci.getMagikIssue().column());
    int exitCode = paths.stream()
        .parallel()
        .map(path -> runChecksOnFile(path, untabify, checkInfos))
        .flatMap(infractions -> infractions.stream())
        .collect(Collectors.toList())
        .stream()
        .sorted(byPath.thenComparing(byLine).thenComparing(byColumn))
        .limit(maxInfractions)
        .map(infraction -> {
          reporter.reportIssue(infraction);
          return getReturnCode(infraction);
        })
        .reduce(0, (code, infractionCode) -> code |= infractionCode);
    return exitCode;
  }

  /**
   * Run the linter.
   * 
   * @return Exit code to return from process.
   * @throws IOException            -
   * @throws IllegalAccessException -
   * @throws InstantiationException -
   * @throws ParseException         -
   * @throws MagikLintException     -
   * @throws InterruptedException   -
   */
  public int run()
      throws IOException, IllegalAccessException, InstantiationException,
      ParseException, MagikLintException, InterruptedException {

    int exitCode = 0;
    if (commandLine.hasOption("show-checks")) {
      Iterable<CheckInfo> checkInfos = CheckInfo.getAllChecks(config);
      showChecks(checkInfos);
    } else if (commandLine.hasOption("help")
               || commandLine.getArgs().length == 0) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("magik-lint", MagikLint.OPTIONS);
    } else if (commandLine.hasOption("watch")) {
      String[] args = commandLine.getArgs();
      Path dir = MagikFileScanner.getSingleDirectoryFromArguments(args);

      Iterable<CheckInfo> checkInfos = CheckInfo.getAllChecks(config);

      Long untabify = null;
      if (commandLine.hasOption("untabify")) {
        untabify = (Long) commandLine.getParsedOptionValue("untabify");
      }

      long maxInfractions = Long.MAX_VALUE;
      if (commandLine.hasOption("max-infractions")) {
        maxInfractions = (Long)commandLine.getParsedOptionValue("max-infractions");
      }

      Reporter reporter = getReporter();
      watch(dir, checkInfos, untabify, maxInfractions, reporter);
    } else {
      String[] args = commandLine.getArgs();
      Collection<Path> paths = MagikFileScanner.getFilesFromArgs(args);

      Iterable<CheckInfo> checkInfos = CheckInfo.getAllChecks(config);

      Long untabify = null;
      if (commandLine.hasOption("untabify")) {
        untabify = (Long) commandLine.getParsedOptionValue("untabify");
      }

      long maxInfractions = Long.MAX_VALUE;
      if (commandLine.hasOption("max-infractions")) {
        maxInfractions = (Long)commandLine.getParsedOptionValue("max-infractions");
      }

      Reporter reporter = getReporter();
      exitCode = runChecks(paths, checkInfos, untabify, maxInfractions, reporter);
    }

    return exitCode;
  }

  /**
   * Main entry point.
   *
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    int returnCode = 0;
    try {
      MagikLint linter = new MagikLint(args);
      returnCode = linter.run();
    } catch (MagikLintException ex) {
      System.out.println("Error: " + ex.getMessage());
      returnCode = 32;
    } catch (ParseException ex) {
      System.out.println("Unable to parse command line: " + ex.getMessage());
      returnCode = 32;
    } catch (IOException | IllegalAccessException | InstantiationException ex) {
      System.out.println("Caught exception: " + ex.getMessage());
      ex.printStackTrace();
      returnCode = 32;
    } catch (InterruptedException ex) {
      // pass
    }

    System.exit(returnCode);
  }

}