package nl.ramsolutions.sw.checks.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.CheckMetadata;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.magik.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reporter that outputs results in SARIF 2.1.0 format. */
public class SarifReporter implements Reporter {

  private static final String SARIF_VERSION = "2.1.0";
  private static final String SARIF_SCHEMA =
      "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json";
  private static final String INFORMATION_URI = "https://github.com/StevenLooman/magik-tools";

  private static final Logger LOGGER = LoggerFactory.getLogger(SarifReporter.class);

  private final PrintStream outStream;
  private final String toolName;
  private final String toolVersion;
  private final List<CheckHolder> checkHolders;
  private final Map<String, Integer> ruleIndexMap;
  private final List<Issue> bufferedIssues;
  private final Set<String> reportedSeverities;

  /**
   * Constructor.
   *
   * @param outStream Output stream to write SARIF JSON to.
   * @param toolName Name of the tool (e.g., "magik-lint").
   * @param toolVersion Version of the tool.
   * @param checkHolders All check holders (for rules array).
   */
  public SarifReporter(
      final PrintStream outStream,
      final String toolName,
      final String toolVersion,
      final List<CheckHolder> checkHolders) {
    this.outStream = outStream;
    this.toolName = toolName;
    this.toolVersion = toolVersion;
    this.checkHolders = checkHolders;
    this.ruleIndexMap = new HashMap<>();
    this.bufferedIssues = new ArrayList<>();
    this.reportedSeverities = new HashSet<>();

    for (int i = 0; i < checkHolders.size(); i++) {
      final CheckHolder holder = checkHolders.get(i);
      this.ruleIndexMap.put(holder.getCheckKeyKebabCase(), i);
    }
  }

  @Override
  public void reportIssue(final Issue issue) {
    this.bufferedIssues.add(issue);

    final CheckHolder holder = issue.check().getHolder();
    if (holder != null) {
      try {
        final CheckMetadata metadata = holder.getMetadata();
        this.reportedSeverities.add(metadata.getDefaultSeverity());
      } catch (final IOException exception) {
        LOGGER.error("Could not read metadata: {}", exception.getMessage(), exception);
      }
    }
  }

  @Override
  public Set<String> reportedSeverities() {
    return Collections.unmodifiableSet(this.reportedSeverities);
  }

  @Override
  public void finish() {
    final JsonObject sarif = new JsonObject();
    sarif.addProperty("$schema", SARIF_SCHEMA);
    sarif.addProperty("version", SARIF_VERSION);

    final JsonArray runs = new JsonArray();
    runs.add(this.buildRun());
    sarif.add("runs", runs);

    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    this.outStream.println(gson.toJson(sarif));
  }

  private JsonObject buildRun() {
    final JsonObject run = new JsonObject();
    run.add("tool", this.buildTool());
    run.add("results", this.buildResults());
    return run;
  }

  private JsonObject buildTool() {
    final JsonObject tool = new JsonObject();
    tool.add("driver", this.buildDriver());
    return tool;
  }

  private JsonObject buildDriver() {
    final JsonObject driver = new JsonObject();
    driver.addProperty("name", this.toolName);
    driver.addProperty("version", this.toolVersion);
    driver.addProperty("informationUri", INFORMATION_URI);
    driver.add("rules", this.buildRules());
    return driver;
  }

  private JsonArray buildRules() {
    final JsonArray rules = new JsonArray();
    for (final CheckHolder holder : this.checkHolders) {
      try {
        rules.add(this.buildRule(holder));
      } catch (final IOException exception) {
        LOGGER.error("Could not read metadata for rule: {}", exception.getMessage(), exception);
      }
    }
    return rules;
  }

  private JsonObject buildRule(final CheckHolder holder) throws IOException {
    final CheckMetadata metadata = holder.getMetadata();
    final JsonObject rule = new JsonObject();
    rule.addProperty("id", metadata.getSqKey());
    rule.addProperty("name", metadata.getRuleSpecification());

    final JsonObject shortDescription = new JsonObject();
    shortDescription.addProperty("text", metadata.getTitle());
    rule.add("shortDescription", shortDescription);

    final JsonObject defaultConfiguration = new JsonObject();
    defaultConfiguration.addProperty("level", mapSeverityToLevel(metadata.getDefaultSeverity()));
    rule.add("defaultConfiguration", defaultConfiguration);

    final List<String> tags = metadata.getTags();
    if (tags != null && !tags.isEmpty()) {
      final JsonObject properties = new JsonObject();
      final JsonArray tagsArray = new JsonArray();
      tags.forEach(tagsArray::add);
      properties.add("tags", tagsArray);
      rule.add("properties", properties);
    }

    return rule;
  }

  private JsonArray buildResults() {
    final Path cwd = Path.of("").toAbsolutePath();
    final JsonArray results = new JsonArray();
    for (final Issue issue : this.bufferedIssues) {
      results.add(this.buildResult(issue, cwd));
    }
    return results;
  }

  private JsonObject buildResult(final Issue issue, final Path cwd) {
    final JsonObject result = new JsonObject();

    final CheckHolder holder = issue.check().getHolder();
    if (holder != null) {
      final String ruleId = holder.getCheckKeyKebabCase();
      result.addProperty("ruleId", ruleId);

      final Integer ruleIndex = this.ruleIndexMap.get(ruleId);
      if (ruleIndex != null) {
        result.addProperty("ruleIndex", ruleIndex);
      }

      try {
        final CheckMetadata metadata = holder.getMetadata();
        result.addProperty("level", mapSeverityToLevel(metadata.getDefaultSeverity()));
      } catch (final IOException exception) {
        LOGGER.error("Could not read metadata: {}", exception.getMessage(), exception);
        result.addProperty("level", "warning");
      }
    }

    final JsonObject message = new JsonObject();
    message.addProperty("text", issue.message());
    result.add("message", message);

    result.add("locations", this.buildLocations(issue, cwd));

    return result;
  }

  private JsonArray buildLocations(final Issue issue, final Path cwd) {
    final JsonArray locations = new JsonArray();
    final JsonObject location = new JsonObject();
    final JsonObject physicalLocation = new JsonObject();

    // Artifact location with relative URI.
    final JsonObject artifactLocation = new JsonObject();
    final Path filePath = issue.location().getPath();
    final Path relativePath = cwd.relativize(filePath.toAbsolutePath());
    artifactLocation.addProperty("uri", relativePath.toString().replace('\\', '/'));
    artifactLocation.addProperty("uriBaseId", "%SRCROOT%");
    physicalLocation.add("artifactLocation", artifactLocation);

    // Region with 1-based lines and columns.
    final Range range = issue.range();
    final JsonObject region = new JsonObject();
    region.addProperty("startLine", range.getStartPosition().getLine());
    region.addProperty("startColumn", range.getStartPosition().getColumn() + 1);
    region.addProperty("endLine", range.getEndPosition().getLine());
    region.addProperty("endColumn", range.getEndPosition().getColumn() + 1);
    physicalLocation.add("region", region);

    location.add("physicalLocation", physicalLocation);
    locations.add(location);
    return locations;
  }

  private static String mapSeverityToLevel(final String severity) {
    return switch (severity) {
      case "Critical", "Major" -> "error";
      case "Minor" -> "warning";
      default -> "note";
    };
  }
}
