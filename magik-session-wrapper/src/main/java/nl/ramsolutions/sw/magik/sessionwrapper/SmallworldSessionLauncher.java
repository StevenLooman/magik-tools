package nl.ramsolutions.sw.magik.sessionwrapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builder for {@link SmallworldSession}. */
class SmallworldSessionLauncher {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmallworldSessionLauncher.class);

  private final List<String> runAliasCommand;
  private final PrintWriter outputWriter;
  private final Pattern promptPattern;

  /**
   * Constructor.
   *
   * @param runAliasCommand Run alias command.
   * @param outputWriter Output writer.
   * @param promptPattern Prompt pattern.
   */
  SmallworldSessionLauncher(
      final List<String> runAliasCommand,
      final PrintWriter outputWriter,
      final Pattern promptPattern) {
    this.runAliasCommand = runAliasCommand;
    this.outputWriter = outputWriter;
    this.promptPattern = promptPattern;
  }

  SmallworldSession launch() throws IOException {
    LOGGER.debug("Starting Smallworld session with command: {}", this.runAliasCommand);

    final ProcessBuilder processBuilder = new ProcessBuilder(this.runAliasCommand);
    processBuilder.redirectErrorStream(true);
    final Process process = processBuilder.start();
    LOGGER.debug("Started Smallworld session, pid: {}", process.pid());

    return new SmallworldSession(process, this.promptPattern, this.outputWriter);
  }
}
