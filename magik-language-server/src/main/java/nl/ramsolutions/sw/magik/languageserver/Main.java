package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Main entry point.
 */
public final class Main {

    private static final Options OPTIONS;
    private static final Option OPTION_DEBUG = Option.builder()
        .longOpt("debug")
        .desc("Show debug messages")
        .build();
    private static final Option OPTION_STDIO = Option.builder()
        .longOpt("stdio")
        .desc("Use STDIO (default, no other option to interface with this language server)")
        .build();

    static {
        OPTIONS = new Options();
        OPTIONS.addOption(OPTION_DEBUG);
        OPTIONS.addOption(OPTION_STDIO);
    }

    private Main() {
    }

    /**
     * Initialize logger from logging.properties.
     *
     * @throws IOException -
     */
    private static void initLogger() throws IOException {
        final InputStream stream = Main.class.getClassLoader().getResourceAsStream("logging.properties");
        LogManager.getLogManager().readConfiguration(stream);
    }

    /**
     * Initialize logger from debug-logging.properties.
     *
     * @throws IOException -
     */
    private static void initDebugLogger() throws IOException {
        final InputStream stream = Main.class.getClassLoader().getResourceAsStream("debug-logging.properties");
        LogManager.getLogManager().readConfiguration(stream);
    }

    /**
     * Parse the command line.
     * @param args Command line arguments.
     * @return Parsed command line.
     * @throws ParseException -
     */
    private static CommandLine parseCommandline(final String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
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

        final MagikLanguageServer server = new MagikLanguageServer();
        final Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);

        final LanguageClient remoteProxy = launcher.getRemoteProxy();
        server.connect(remoteProxy);

        launcher.startListening();
    }

}
