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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MagikLint {

  CommandLine commandLine;
  Configuration config;
  static final Options options;

  static
  {
    options = new Options();
    options.addOption(Option.builder()
        .longOpt("help")
        .desc("Show this help")
        .build());
    options.addOption(Option.builder()
        .longOpt("msg-template")
        .desc("Output pattern")
        .hasArg()
        .type(PatternOptionBuilder.STRING_VALUE)
        .build());
    options.addOption(Option.builder()
        .longOpt("rcfile")
        .desc("Configuration file")
        .hasArg()
        .type(PatternOptionBuilder.EXISTING_FILE_VALUE)
        .build());
    options.addOption(Option.builder()
        .longOpt("show-checks")
        .desc("Show checks and quit")
        .build());
    options.addOption(Option.builder()
        .longOpt("untabify")
        .desc("Expand tabs to N spaces")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build());
    options.addOption(Option.builder()
        .longOpt("column-offset")
        .desc("Set column offset, positive or negative")
        .hasArg()
        .type(PatternOptionBuilder.NUMBER_VALUE)
        .build());
    options.addOption(Option.builder()
        .longOpt("watch")
        .desc("Watch the given directory/file for changes")
        .build());
  }

  static final Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING = new HashMap<>();

  static {
    SEVERITY_EXIT_CODE_MAPPING.put("Major", 2);
    SEVERITY_EXIT_CODE_MAPPING.put("Minor", 4);
  }

  MagikLint(String[] args) throws ParseException {
    commandLine = parseCommandline(args);

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

  private CommandLine parseCommandline(String[] args) throws ParseException {
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(MagikLint.options, args);
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

  private MagikVisitorContext buildContext(Path path) throws IOException, ParseException {
    Charset defaultCharset = Charset.forName("ISO_8859_1");
    Charset charset = FileCharsetDeterminer.determineCharset(path, defaultCharset);
    byte[] encoded = Files.readAllBytes(path);
    String fileContents = new String(encoded, charset);
    if (commandLine.getOptionValue("untabify") != null) {
      Long untabify = (Long) commandLine.getParsedOptionValue("untabify");
      String spaces = String.format("%" + untabify + "s", "");
      fileContents = fileContents.replaceAll("\t", spaces);
    }

    MagikParser parser = new MagikParser(charset);
    AstNode root = parser.parse(fileContents);

    return new MagikVisitorContext(path, fileContents, root);
  }

  private Iterable<CheckInfo> getAllChecks()
      throws IllegalAccessException, InstantiationException, FileNotFoundException {
    return CheckInfo.getAllChecks(config);
  }

  private List<MagikIssue> runCheck(MagikVisitorContext context, CheckInfo checkInfo) {
    MagikCheck check = checkInfo.getCheck();
    List<MagikIssue> checkIssues = check.scanFileForIssues(context);
    return checkIssues;
  }

  /**
   * Get the return code according to this infraction.
   *
   * @param checkInfraction Infraction to get return code for.
   * @return Return code for the infraction.
   * @throws FileNotFoundException Thrown in case CheckInfo cannot find data.
   */
  public static int getReturnCode(CheckInfraction checkInfraction) throws FileNotFoundException {
    CheckInfo checkInfo = checkInfraction.getCheckInfo();
    String checkSeverity = checkInfo.getSeverity();
    return SEVERITY_EXIT_CODE_MAPPING.getOrDefault(checkSeverity, 0);
  }

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
   * Check all files.
   *
   * @param checkInfos Checks to run.
   * @param paths      Paths to check.
   * @return Exit code for process.
   * @throws IOException    Unable to read file
   * @throws ParseException Unable to parse command line
   */
  private int checkFiles(Iterable<CheckInfo> checkInfos, Iterable<Path> paths)
      throws IOException, ParseException {
    int returnCode = 0;

    Comparator<CheckInfraction> byPath = Comparator.comparing(ci -> ci.getPath().toString());
    Comparator<CheckInfraction> byLine = Comparator.comparing(ci -> ci.getMagikIssue().line());
    Comparator<CheckInfraction> byColumn = Comparator.comparing(ci -> ci.getMagikIssue().column());

    Reporter output = getReporter();
    for (Path path : paths) {
      MagikVisitorContext context = buildContext(path);
      List<CheckInfraction> fileInfractions = new ArrayList<>();

      // run checks, report issues
      for (CheckInfo checkInfo : checkInfos) {
        if (!checkInfo.isEnabled()) {
          continue;
        }

        List<MagikIssue> magikIssues = runCheck(context, checkInfo);
        List<CheckInfraction> checkInfractions = magikIssues.stream()
            .map(magikIssue -> new CheckInfraction(path, checkInfo, magikIssue))
            .collect(Collectors.toList());

        fileInfractions.addAll(checkInfractions);
      }

      fileInfractions.sort(
          byPath
          .thenComparing(byLine)
          .thenComparing(byColumn));
      for (CheckInfraction checkInfraction : fileInfractions) {
        output.reportIssue(checkInfraction);

        int checkReturnCode = getReturnCode(checkInfraction);
        returnCode = returnCode | checkReturnCode;
      }

    }

    return returnCode;
  }

  /**
   * Watch the given directory/file for changes.
   * 
   * @param checkInfos Checks to run.
   * @param path       Path (directory) to watch.
   * @throws MagikLintException   -
   * @throws InterruptedException -
   * @throws ParseException       -
   */
  private void watch(Iterable<CheckInfo> checkInfos, Path dir)
      throws IOException, MagikLintException, ParseException, InterruptedException {
    MagikFileWatcher.ChangesListener listener = new MagikFileWatcher.ChangesListener() {
      @Override
      void onChanged(Collection<Path> paths) {
        try {
          System.out.println("File change detected. Scanning files...");
          checkFiles(checkInfos, paths);
          System.out.println("Scanning complete. Watching for file changes.");
        } catch (IOException | ParseException ex) {
          System.out.println("Caught exception: " + ex.getMessage());
        }
      }
    };

    // Initial file scan.
    Collection<Path> paths = MagikFileScanner.scanMagikFiles(dir);
    listener.onChanged(paths);

    // Continuous scanning for changes.
    MagikFileWatcher watcher = new MagikFileWatcher(dir, listener);
    watcher.run();
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
    Iterable<CheckInfo> checkInfos = getAllChecks();

    int exitCode = 0;
    if (commandLine.hasOption("help")
        || commandLine.getArgs().length == 0) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("magik-lint", MagikLint.options);
    } else if (commandLine.hasOption("show-checks")) {
      showChecks(checkInfos);
    } else if (commandLine.hasOption("watch")) {
      String[] args = commandLine.getArgs();
      Path dir = MagikFileScanner.getSingleDirectoryFromArguments(args);
      watch(checkInfos, dir);
    } else {
      // loop over files
      String[] args = commandLine.getArgs();
      Iterable<Path> paths = MagikFileScanner.getFilesFromArgs(args);
      exitCode = checkFiles(checkInfos, paths);
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