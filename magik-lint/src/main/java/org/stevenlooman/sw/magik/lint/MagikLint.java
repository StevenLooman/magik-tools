package org.stevenlooman.sw.magik.lint;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.sonar.sslr.api.AstNode;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.stevenlooman.sw.magik.MagikIssue;
import org.stevenlooman.sw.magik.MagikCheck;
import org.stevenlooman.sw.magik.MagikVisitorContext;
import org.stevenlooman.sw.magik.lint.output.MessageFormatReporter;
import org.stevenlooman.sw.magik.lint.output.Reporter;
import org.stevenlooman.sw.magik.parser.FileCharsetDeterminer;
import org.stevenlooman.sw.magik.parser.MagikParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MagikLint {

  CommandLine commandLine;
  Configuration config;

  final static Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING = Maps.newHashMap();
  static {
    SEVERITY_EXIT_CODE_MAPPING.put("Major", 2);
    SEVERITY_EXIT_CODE_MAPPING.put("Minor", 4);
  }

  MagikLint(String[] args) throws ParseException {
    commandLine = parseCommandline(args);

    // read configuration
    if (commandLine.hasOption("rcfile")) {
      String rcfile = commandLine.getOptionValue("rcfile");
      Path path = Paths.get(rcfile);
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
    Options options = new Options();
    options.addOption(Option.builder()
        .longOpt("msg-template")
        .desc("Output pattern")
        .hasArg()
        .build());
    options.addOption(Option.builder()
        .longOpt("rcfile")
        .desc("Configuration file")
        .hasArg()
        .build());
    options.addOption(Option.builder()
        .longOpt("show-checks")
        .desc("Show checks and quit")
        .build());

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);
    return cmd;
  }

  private List<Path> getFiles() throws IOException {
    List<Path> files = new ArrayList<>();

    for (String arg: commandLine.getArgList()) {
      Path path = Paths.get(arg);
      File file = path.toFile();
      if (file.isDirectory()) {
        addAllFilesInDirectory(path, files);
      } else {
        files.add(path);
      }
    }

    return files;
  }

  private void addAllFilesInDirectory(Path directory, List<Path> files) throws IOException {
    PathMatcher matcher = directory.getFileSystem().getPathMatcher("glob:**.magik");

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
      for (Path entry : stream) {
        File file = entry.toFile();
        if (file.isDirectory()) {
          addAllFilesInDirectory(entry, files);
        } else {
          if (matcher.matches(entry)) {
            files.add(entry);
          }
        }
      }
    }
  }


  private MagikVisitorContext buildContext(Path path) throws IOException {
    Charset charset = FileCharsetDeterminer.determineCharset(path, Charsets.ISO_8859_1);
    MagikParser parser = new MagikParser(charset);
    AstNode root = parser.parse(path);

    byte[] encoded = Files.readAllBytes(path);
    String fileContents = new String(encoded, charset);
    return new MagikVisitorContext(fileContents, root);
  }


  private Iterable<CheckInfo> getAllChecks() throws IllegalAccessException, InstantiationException, FileNotFoundException {
    return CheckInfo.getAllChecks(config);
  }

  private List<MagikIssue> runCheck(MagikVisitorContext context, CheckInfo checkInfo) {
    MagikCheck check = checkInfo.getCheck();
    List<MagikIssue> checkIssues = check.scanFileForIssues(context);
    return checkIssues;
  }

  public static int getReturnCode(CheckInfo checkInfo) throws FileNotFoundException {
    String checkSeverity = checkInfo.getSeverity();
    return SEVERITY_EXIT_CODE_MAPPING.getOrDefault(checkSeverity, 0);
  }

  private void showChecks(Iterable<CheckInfo> checkInfos) throws IllegalAccessException, FileNotFoundException {
    for (CheckInfo checkInfo: checkInfos) {
      String name = checkInfo.getName();
      if (checkInfo.isEnabled()) {
        System.out.println("Check: " + name + " (" + checkInfo.getTitle() + ")");
      } else {
        System.out.println("Check: " + name + " (disabled) (" + checkInfo.getTitle() + ")");
      }

      checkInfo.getParameters().forEach(parameterInfo -> {
        System.out.println("\t" + parameterInfo.getName() + ":\t" + parameterInfo.getValue() + " (" + parameterInfo.getDescription() + ")");
      });
    }
  }

  private int checkFiles(Iterable<CheckInfo> checkInfos) throws IOException {
    int returnCode = 0;

    Reporter output = getReporter();
    for (Path path: getFiles()) {
      MagikVisitorContext context = buildContext(path);

      // run checks, report issues
      for (CheckInfo checkInfo: checkInfos) {
        for (MagikIssue issue: runCheck(context, checkInfo)) {
          output.reportIssue(path, checkInfo, issue);

          int checkReturnCode = getReturnCode(checkInfo);
          returnCode = returnCode | checkReturnCode;
        }
      }
    }

    return returnCode;
  }

  public int run() throws IOException, IllegalAccessException, InstantiationException {
    Iterable<CheckInfo> checkInfos = getAllChecks();

    if (commandLine.hasOption("show-checks")) {
      showChecks(checkInfos);
      return 0;
    }

    // loop over files
    return checkFiles(checkInfos);
  }

  public static void main(String[] args) {
    int returnCode = 0;
    try {
      MagikLint linter = new MagikLint(args);
      returnCode = linter.run();
    } catch (ParseException ex) {
      System.out.println("Unable to parse command line: " + ex.getMessage());
      returnCode = 32;
    } catch (IOException ex) {
      System.out.println("Caught exception: " + ex.getMessage());
      ex.printStackTrace();
      returnCode = 32;
    } catch (IllegalAccessException ex) {
      ex.printStackTrace();
    } catch (InstantiationException ex) {
      ex.printStackTrace();
    }

    System.exit(returnCode);
  }

  public Reporter getReporter() {
    if (commandLine.hasOption("msg-template")) {
      String template = commandLine.getOptionValue("msg-template");
      return new MessageFormatReporter(template);
    }

    return new MessageFormatReporter();
  }

}
