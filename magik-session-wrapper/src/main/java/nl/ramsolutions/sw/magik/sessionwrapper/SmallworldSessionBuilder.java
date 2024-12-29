package nl.ramsolutions.sw.magik.sessionwrapper;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builder for {@link SmallworldSession}. */
class SmallworldSessionBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmallworldSessionBuilder.class);

  private final List<String> arguments = new ArrayList<>();
  private PrintWriter outputWriter = new PrintWriter(System.out); // NOSONAR: SystemOut

  private @Nullable Path smallworldProductDir;
  private @Nullable String aliases = null;
  private @Nullable String aliasesEntry = null;
  private @Nullable String environment = null;
  private Pattern promptPattern = SmallworldSession.DEFAULT_PROMPT_PATTERN;

  SmallworldSessionBuilder withSmallworldProductDir(final Path smallworldProductDir) {
    this.smallworldProductDir = smallworldProductDir;
    return this;
  }

  SmallworldSessionBuilder withAliases(final String aliases) {
    this.aliases = aliases;
    return this;
  }

  SmallworldSessionBuilder withAliasesEntry(final String aliasesEntry) {
    this.aliasesEntry = aliasesEntry;
    return this;
  }

  SmallworldSessionBuilder withEnvironment(final String environment) {
    this.environment = environment;
    return this;
  }

  SmallworldSessionBuilder withJavaArgument(final String argument) {
    this.arguments.add("-j");
    this.arguments.add(argument);
    return this;
  }

  SmallworldSessionBuilder withOutputWriter(final PrintWriter outputWriter) {
    this.outputWriter = outputWriter;
    return this;
  }

  private Path getRunaliasPath() {
    final String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) {
      return smallworldProductDir.resolve("bin/x86/runalias.exe");
    }

    return smallworldProductDir.resolve("bin/share/runalias");
  }

  private List<String> buildCommand() {
    final List<String> command = new ArrayList<>();

    final Path runAliasPath = this.getRunaliasPath();
    command.add(runAliasPath.toString());

    if (this.environment != null) {
      command.add("-e");
      command.add(this.environment);
    }

    if (this.aliases != null) {
      command.add("-a");
      command.add(this.aliases);
    }

    command.addAll(this.arguments);

    command.add(this.aliasesEntry);

    return command;
  }

  SmallworldSession build() throws IOException {
    if (this.smallworldProductDir == null) {
      throw new IllegalStateException("Smallworld product directory is required");
    }

    if (this.aliasesEntry == null) {
      throw new IllegalStateException("Aliases entry is required");
    }

    final List<String> command = this.buildCommand();
    LOGGER.debug("Starting Smallworld session with command: {}", command);

    final ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    final Process process = processBuilder.start();
    LOGGER.debug("Started Smallworld session, pid: {}", process.pid());

    return new SmallworldSession(process, this.promptPattern, this.outputWriter);
  }
}
