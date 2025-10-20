package nl.ramsolutions.sw.sslr;

import java.io.IOException;
import nl.ramsolutions.sw.sslr.magik.MagikConfigurationModel;
import nl.ramsolutions.sw.sslr.moduledef.SwModuleDefConfigurationModel;
import nl.ramsolutions.sw.sslr.productdef.SwProductDefConfigurationModel;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.cli.help.HelpFormatter;
import org.sonar.sslr.toolkit.ConfigurationModel;
import org.sonar.sslr.toolkit.Toolkit;

/** Main entry point. */
public final class Main {

  private static final Options OPTIONS;
  private static final Option OPTION_GRAMMAR =
      Option.builder("g")
          .longOpt("grammar")
          .desc("Grammar, one of: magik, sw-product-def, sw-module-def")
          .hasArg()
          .required()
          .type(PatternOptionBuilder.STRING_VALUE)
          .get();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_GRAMMAR);
  }

  private Main() {}

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
   * Start Toolkit with a Magik parser.
   *
   * @param args Arguments
   * @throws ParseException -
   * @throws IOException -
   */
  public static void main(final String[] args) throws ParseException, IOException {
    final CommandLine commandLine;
    try {
      commandLine = Main.parseCommandline(args);
    } catch (final MissingOptionException | UnrecognizedOptionException exception) {
      System.err.println("Missing required option: " + exception.getMessage());

      Main.showHelp();

      System.exit(1);
      return; // Keep inferer happy.
    }

    final String grammar = commandLine.getOptionValue(OPTION_GRAMMAR);
    final ConfigurationModel configurationModel;
    if ("magik".equalsIgnoreCase(grammar)) {
      configurationModel = new MagikConfigurationModel();
    } else if ("sw-product-def".equalsIgnoreCase(grammar)) {
      configurationModel = new SwProductDefConfigurationModel();
    } else if ("sw-module-def".equalsIgnoreCase(grammar)) {
      configurationModel = new SwModuleDefConfigurationModel();
    } else {
      // This should never happen because of the earlier check.
      System.err.println("Unknown grammar: " + grammar);

      Main.showHelp();

      System.exit(1);
      return; // Keep inferer happy.
    }

    final String title = "SSLR Toolkit: " + grammar;
    final Toolkit toolkit = new Toolkit(title, configurationModel);
    toolkit.run();
  }

  private static void showHelp() throws IOException {
    final HelpFormatter helpFormatter = HelpFormatter.builder().setShowSince(false).get();
    helpFormatter.printHelp("java -jar sslr-toolkit.jar", "sslr-toolkit", OPTIONS, "", true);
  }
}
