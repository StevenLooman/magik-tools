package nl.ramsolutions.sw.magik.debugadapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;
import java.util.logging.LogManager;
import org.apache.commons.cli.*;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.jsonrpc.Launcher;

/** Main entry point. */
public final class Main {

  private static final Options OPTIONS;
  private static final Option OPTION_DEBUG =
      Option.builder().longOpt("debug").desc("Show debug messages").build();
  private static final Option OPTION_STDIO =
      Option.builder().longOpt("stdio").desc("Use STDIO (default)").build();
  private static final Option OPTION_NET =
      Option.builder()
          .longOpt("net")
          .desc("Open the debug adapter on port 5008 instead of STDIN")
          .build();

  static {
    OPTIONS = new Options();
    OPTIONS.addOption(OPTION_DEBUG);
    OPTIONS.addOption(OPTION_STDIO);
    OPTIONS.addOption(OPTION_NET);
  }

  private Main() {}

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

    final MagikDebugAdapter server = new MagikDebugAdapter();
    Launcher<IDebugProtocolClient> launcher;
    if (commandLine.hasOption(OPTION_NET)) {
      launcher = createSocketLauncher(server, new InetSocketAddress("localhost", 5008));
    } else {
      launcher = DSPLauncher.createServerLauncher(server, System.in, System.out); // NOSONAR
    }

    assert launcher != null;
    final IDebugProtocolClient remoteProxy = launcher.getRemoteProxy();
    server.connect(remoteProxy);

    launcher.startListening();
  }

  static Launcher<IDebugProtocolClient> createSocketLauncher(
      MagikDebugAdapter debugAdapter, SocketAddress socketAddress) throws IOException {
    try (AsynchronousServerSocketChannel serverSocket =
        AsynchronousServerSocketChannel.open().bind(socketAddress)) {
      AsynchronousSocketChannel socketChannel;
      try {
        socketChannel = serverSocket.accept().get();
        return DSPLauncher.createServerLauncher(
            debugAdapter,
            Channels.newInputStream(socketChannel),
            Channels.newOutputStream(socketChannel));
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
