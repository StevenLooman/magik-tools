package nl.ramsolutions.sw.checks.output;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import nl.ramsolutions.sw.checks.CheckHolder;

/** Context passed to reporter factories. */
public final class ReporterContext {

  private final PrintStream outStream;
  private final String toolName;
  private final String toolVersion;
  private final List<CheckHolder> checkHolders;

  /** Constructor. */
  public ReporterContext(
      final PrintStream outStream,
      final String toolName,
      final String toolVersion,
      final List<CheckHolder> checkHolders) {
    this.outStream = Objects.requireNonNull(outStream, "outStream");
    this.toolName = Objects.requireNonNull(toolName, "toolName");
    this.toolVersion = Objects.requireNonNull(toolVersion, "toolVersion");
    this.checkHolders = List.copyOf(Objects.requireNonNull(checkHolders, "checkHolders"));
  }

  public PrintStream getOutStream() {
    return this.outStream;
  }

  public String getToolName() {
    return this.toolName;
  }

  public String getToolVersion() {
    return this.toolVersion;
  }

  public List<CheckHolder> getCheckHolders() {
    return this.checkHolders;
  }
}
