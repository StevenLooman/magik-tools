package nl.ramsolutions.sw.magik.lint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.logging.LogManager;
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

/**
 * Main entry point for magik linter.
 */
public final class Main {

    private static final Options OPTIONS;
    private static final String OPTION_MSG_TEMPLATE = "msg-template";
    private static final String OPTION_RCFILE = "rcfile";
    private static final String OPTION_SHOW_CHECKS = "show-checks";
    private static final String OPTION_COLUMN_OFFSET = "column-offset";
    private static final String OPTION_MAX_INFRACTIONS = "max-infractions";
    private static final String OPTION_UNTABIFY = "untabify";
    private static final String OPTION_DEBUG = "debug";
    private static final String OPTION_HELP = "help";

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_HELP)
            .desc("Show this help")
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_MSG_TEMPLATE)
            .desc("Output pattern")
            .hasArg()
            .type(PatternOptionBuilder.STRING_VALUE)
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_RCFILE)
            .desc("Configuration file")
            .hasArg()
            .type(PatternOptionBuilder.EXISTING_FILE_VALUE)
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_SHOW_CHECKS)
            .desc("Show checks and quit")
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_UNTABIFY)
            .desc("Expand tabs to N spaces")
            .hasArg()
            .type(PatternOptionBuilder.NUMBER_VALUE)
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_COLUMN_OFFSET)
            .desc("Set column offset, positive or negative")
            .hasArg()
            .type(PatternOptionBuilder.NUMBER_VALUE)
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_MAX_INFRACTIONS)
            .desc("Set max number of reporter infractions")
            .hasArg()
            .type(PatternOptionBuilder.NUMBER_VALUE)
            .build());
        OPTIONS.addOption(Option.builder()
            .longOpt(OPTION_DEBUG)
            .desc("Enable showing of debug information")
            .build());
    }

    private static final Map<String, Integer> SEVERITY_EXIT_CODE_MAPPING = Map.of(
        "Critical", 2,
        "Major", 4,
        "Minor", 8);

    private Main() {
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
     * Initialize logger from logging.properties.
     */
    private static void initDebugLogger() {
        final ClassLoader classLoader = Main.class.getClassLoader();
        final InputStream stream = classLoader.getResourceAsStream("debug-logging.properties");
        try {
            LogManager.getLogManager().readConfiguration(stream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create the reporter.
     *
     * @param configuration Configuration.
     * @return Reporter.
     */
    private static Reporter createReporter(final Configuration configuration) {
        final String template = configuration.hasProperty(OPTION_MSG_TEMPLATE)
            ? configuration.getPropertyString(OPTION_MSG_TEMPLATE)
            : MessageFormatReporter.DEFAULT_FORMAT;

        final String columnOffsetStr = configuration.getPropertyString(OPTION_COLUMN_OFFSET);
        final Long columnOffset = configuration.hasProperty(OPTION_COLUMN_OFFSET)
            ? Long.parseLong(columnOffsetStr)
            : null;

        return new MessageFormatReporter(System.out, template, columnOffset);
    }

    /**
     * Main entry point.
     *
     * @param args Arguments.
     * @throws IOException -
     * @throws ParseException -
     * @throws ReflectiveOperationException -
     */
    public static void main(final String[] args) throws ParseException, IOException, ReflectiveOperationException {
        final CommandLine commandLine;
        try {
            commandLine = Main.parseCommandline(args);
        } catch (UnrecognizedOptionException exception) {
            System.out.println("Unrecognized option: " + exception.getMessage());

            System.exit(1);
            return;  // Keep inferer happy.
        }

        if (commandLine.hasOption(OPTION_DEBUG)) {
            Main.initDebugLogger();
        }

        // Read configuration.
        final Configuration config;
        if (commandLine.hasOption(OPTION_RCFILE)) {
            final File rcfile = (File) commandLine.getParsedOptionValue(OPTION_RCFILE);
            final Path path = rcfile.toPath();
            config = new Configuration(path);
        } else {
            final Path path = ConfigurationLocator.locateConfiguration();
            config = path != null
                ? new Configuration(path)
                : new Configuration();
        }

        // Fill configuration from command line.
        Main.copyOptionToConfig(commandLine, config, OPTION_UNTABIFY);
        Main.copyOptionToConfig(commandLine, config, OPTION_MAX_INFRACTIONS);
        Main.copyOptionToConfig(commandLine, config, OPTION_COLUMN_OFFSET);
        Main.copyOptionToConfig(commandLine, config, OPTION_MSG_TEMPLATE);

        // Show checks.
        if (commandLine.hasOption(OPTION_SHOW_CHECKS)) {
            final Reporter reporter = new NullReporter();
            final MagikLint lint = new MagikLint(config, reporter);
            final Writer writer = new PrintWriter(System.out);
            lint.showChecks(writer);
            writer.flush();
            System.exit(0);
        }

        // Help.
        if (commandLine.hasOption(OPTION_HELP)
            || commandLine.getArgs().length == 0) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("magik-lint", Main.OPTIONS);

            System.exit(0);
        }

        // Actual linting.
        final Reporter reporter = Main.createReporter(config);
        final MagikLint lint = new MagikLint(config, reporter);
        final String[] leftOverArgs = commandLine.getArgs();
        final Collection<Path> paths = MagikFileScanner.getFilesFromArgs(leftOverArgs);
        lint.run(paths);

        final int exitCode = reporter.reportedSeverities().stream()
            .map(severity -> SEVERITY_EXIT_CODE_MAPPING.get(severity))
            .reduce(0, (partial, sum) -> sum | partial);
        System.exit(exitCode);
    }

    private static void copyOptionToConfig(
            final CommandLine commandLine,
            final Configuration config,
            final String key) {
        if (commandLine.hasOption(key)) {
            final String value = commandLine.getOptionValue(key);
            config.setProperty(key, value);
        }
    }

}
