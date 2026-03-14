package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.LogManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

/** Main entry point. */
public final class Main {

  private static final Options OPTIONS;
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Show debug messages").get();
  private static final Option OPTION_VERSION =
      Option.builder().longOpt("version").desc("Show version and exit").get();
  private static final Option OPTION_HELP =
      Option.builder().longOpt("help").desc("Show this help and exit").get();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_DEBUG);
    OPTIONS.addOption(OPTION_VERSION);
    OPTIONS.addOption(OPTION_HELP);
  }

  private Main() {}

  private static PrintStream getOutStream() {
    return System.out; // NOSONAR
  }

  private static InputStream getInStream() {
    return System.in; // NOSONAR
  }

  private static String getName() {
    return "magik-debug-adapter";
  }

  private static String getArtifactName() {
    return Main.getName() + ".jar";
  }

  private static String getVersion() {
    return Main.class.getPackage().getImplementationVersion();
  }

  /**
   * Initialize logger from logging.properties.
   *
   * @throws IOException -
   */
  private static void initLogger() throws IOException {
    try (InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
      LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
    }
  }

  /**
   * Initialize logger from debug-logging.properties.
   *
   * @throws IOException -
   */
  private static void initDebugLogger() throws IOException {
    try (InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("debug-logging.properties")) {
      LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
    }
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

    if (commandLine.hasOption(OPTION_HELP)) {
      Main.showHelp();

      System.exit(0);
    }

    if (commandLine.hasOption(OPTION_VERSION)) {
      Main.showVersion();

      System.exit(0);
    }

    final MagikDebugAdapter server = new MagikDebugAdapter();
    final InputStream inStream = Main.getInStream();
    final PrintStream outStream = Main.getOutStream();
    final Launcher<IDebugProtocolClient> launcher =
        DSPLauncher.createServerLauncher(server, inStream, outStream);

    final IDebugProtocolClient remoteProxy = launcher.getRemoteProxy();
    server.connect(remoteProxy);

    launcher.startListening();
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
