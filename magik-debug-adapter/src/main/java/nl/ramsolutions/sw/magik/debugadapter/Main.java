package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

/**
 * Main entry point.
 */
public final class Main {

    private static final Options OPTIONS;
    private static final Option OPTION_DEBUG = Option.builder()
        .longOpt("debug")
        .desc("Show debug messages")
        .build();

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(OPTION_DEBUG);
    }

    private Main() {
    }

    /**
     * Initialize logger from logging.properties.
     *
     * @throws IOException -
     */
    private static void initLogger() throws IOException {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(stream);  // NOSONAR: Own logging configuration.
        }
    }

    /**
     * Initialize logger from debug-logging.properties.
     *
     * @throws IOException -
     */
    private static void initDebugLogger() throws IOException {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("debug-logging.properties")) {
            LogManager.getLogManager().readConfiguration(stream);  // NOSONAR: Own logging configuration.
        }
    }

    /**
     * Parse the command line.
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

        final MagikDebugAdapter server = new MagikDebugAdapter();
        final Launcher<IDebugProtocolClient> launcher =
            DSPLauncher.createServerLauncher(server, System.in, System.out);  // NOSONAR

        final IDebugProtocolClient remoteProxy = launcher.getRemoteProxy();
        server.connect(remoteProxy);

        launcher.startListening();
    }

}
