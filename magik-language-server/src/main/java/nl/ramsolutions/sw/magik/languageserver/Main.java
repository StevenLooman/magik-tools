package nl.ramsolutions.sw.magik.languageserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.LogManager;
import org.apache.commons.cli.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

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
          .desc("Open the LSP on port 5007 instead of using STDIN for commands")
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
    final InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Initialize logger from debug-logging.properties.
   *
   * @throws IOException -
   */
  private static void initDebugLogger() throws IOException {
    final InputStream stream =
        Main.class.getClassLoader().getResourceAsStream("debug-logging.properties");
    LogManager.getLogManager().readConfiguration(stream); // NOSONAR: Own logging configuration.
  }

  /**
   * Parse the command line.
   *
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
    Launcher<MagikLanguageClient> launcher;
    Function<MessageConsumer, MessageConsumer> wrapper = consumer -> (MessageConsumer) consumer;
    if (commandLine.hasOption(OPTION_NET)) {
      launcher =
          createSocketLauncher(
              server,
              new InetSocketAddress("localhost", 5007),
              Executors.newCachedThreadPool(),
              wrapper);
    } else {
      launcher =
          Launcher.createIoLauncher(
              server,
              MagikLanguageClient.class,
              System.in,
              System.out,
              Executors.newCachedThreadPool(),
              wrapper);
    }

    assert launcher != null;
    final LanguageClient remoteProxy = launcher.getRemoteProxy();
    server.connect(remoteProxy);

    launcher.startListening();
  }

  static Launcher<MagikLanguageClient> createSocketLauncher(
      LanguageServer languageServer,
      SocketAddress socketAddress,
      ExecutorService executorService,
      Function<MessageConsumer, MessageConsumer> wrapper)
      throws IOException {
    try (AsynchronousServerSocketChannel serverSocket =
        AsynchronousServerSocketChannel.open().bind(socketAddress)) {
      AsynchronousSocketChannel socketChannel;
      try {
        socketChannel = serverSocket.accept().get();
        return Launcher.createIoLauncher(
            languageServer,
            MagikLanguageClient.class,
            Channels.newInputStream(socketChannel),
            Channels.newOutputStream(socketChannel),
            executorService,
            wrapper);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
