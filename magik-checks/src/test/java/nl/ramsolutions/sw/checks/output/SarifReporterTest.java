package nl.ramsolutions.sw.checks.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.Issue;
import nl.ramsolutions.sw.checks.magik.ForbiddenCallCheck;
import nl.ramsolutions.sw.checks.magik.LineLengthCheck;
import nl.ramsolutions.sw.magik.Location;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import org.junit.jupiter.api.Test;

/** Tests for {@link SarifReporter}. */
class SarifReporterTest {

  @Test
  void testEmptyReport() {
    final JsonObject sarif = runReporter(List.of(), List.of());

    assertThat(sarif.get("version").getAsString()).isEqualTo("2.1.0");
    assertThat(sarif.has("$schema")).isTrue();

    final JsonObject run = sarif.getAsJsonArray("runs").get(0).getAsJsonObject();
    final JsonObject driver = run.getAsJsonObject("tool").getAsJsonObject("driver");
    assertThat(driver.get("name").getAsString()).isEqualTo("magik-lint");
    assertThat(driver.get("version").getAsString()).isEqualTo("test");

    final JsonArray results = run.getAsJsonArray("results");
    assertThat(results).isEmpty();

    final JsonArray rules = driver.getAsJsonArray("rules");
    assertThat(rules).isNotNull();
  }

  @Test
  void testSingleIssue() throws ReflectiveOperationException {
    final CheckHolder holder =
        new CheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final Check check = holder.createCheck();

    final URI uri = URI.create("file:///test/src/example.magik");
    final Range range = new Range(new Position(10, 4), new Position(10, 20));
    final Location location = new Location(uri, range);
    final Issue issue = new Issue(location, "Forbidden call detected", check);

    final JsonObject sarif = runReporter(List.of(holder), List.of(issue));

    final JsonObject run = sarif.getAsJsonArray("runs").get(0).getAsJsonObject();
    final JsonArray results = run.getAsJsonArray("results");
    assertThat(results).hasSize(1);

    final JsonObject result = results.get(0).getAsJsonObject();
    assertThat(result.get("ruleId").getAsString()).isEqualTo("forbidden-call");
    assertThat(result.get("ruleIndex").getAsInt()).isEqualTo(0);
    assertThat(result.get("message").getAsJsonObject().get("text").getAsString())
        .isEqualTo("Forbidden call detected");

    final JsonObject region =
        result
            .getAsJsonArray("locations")
            .get(0)
            .getAsJsonObject()
            .getAsJsonObject("physicalLocation")
            .getAsJsonObject("region");
    assertThat(region.get("startLine").getAsInt()).isEqualTo(10);
    assertThat(region.get("startColumn").getAsInt()).isEqualTo(5); // 0-based + 1
    assertThat(region.get("endLine").getAsInt()).isEqualTo(10);
    assertThat(region.get("endColumn").getAsInt()).isEqualTo(21); // 0-based + 1
  }

  @Test
  void testSeverityMapping() throws ReflectiveOperationException {
    final CheckHolder holder = new CheckHolder(LineLengthCheck.class, Collections.emptySet(), true);
    final Check check = holder.createCheck();

    final URI uri = URI.create("file:///test/src/example.magik");
    final Location location = new Location(uri, new Range(new Position(1, 0), new Position(1, 0)));
    final Issue issue = new Issue(location, "Line too long", check);

    final JsonObject sarif = runReporter(List.of(holder), List.of(issue));

    final JsonObject run = sarif.getAsJsonArray("runs").get(0).getAsJsonObject();
    final JsonObject result = run.getAsJsonArray("results").get(0).getAsJsonObject();
    // LineLengthCheck has Minor severity -> "warning"
    assertThat(result.get("level").getAsString()).isEqualTo("warning");
  }

  @Test
  void testRulesPopulated() {
    final CheckHolder holder1 =
        new CheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final CheckHolder holder2 =
        new CheckHolder(LineLengthCheck.class, Collections.emptySet(), true);

    final JsonObject sarif = runReporter(List.of(holder1, holder2), List.of());

    final JsonObject run = sarif.getAsJsonArray("runs").get(0).getAsJsonObject();
    final JsonArray rules =
        run.getAsJsonObject("tool").getAsJsonObject("driver").getAsJsonArray("rules");
    assertThat(rules).hasSize(2);

    final JsonObject rule0 = rules.get(0).getAsJsonObject();
    assertThat(rule0.get("id").getAsString()).isEqualTo("forbidden-call");
    assertThat(rule0.has("shortDescription")).isTrue();
    assertThat(rule0.has("defaultConfiguration")).isTrue();
  }

  @Test
  void testReportedSeverities() throws ReflectiveOperationException {
    final CheckHolder holder =
        new CheckHolder(ForbiddenCallCheck.class, Collections.emptySet(), true);
    final Check check = holder.createCheck();

    final URI uri = URI.create("file:///test/src/example.magik");
    final Location location = new Location(uri, new Range(new Position(1, 0), new Position(1, 0)));
    final Issue issue = new Issue(location, "test", check);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
    final SarifReporter reporter = new SarifReporter(ps, "magik-lint", "test", List.of(holder));
    reporter.reportIssue(issue);

    assertThat(reporter.reportedSeverities()).isNotEmpty();
  }

  private JsonObject runReporter(final List<CheckHolder> holders, final List<Issue> issues) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
    final SarifReporter reporter = new SarifReporter(ps, "magik-lint", "test", holders);

    for (final Issue issue : issues) {
      reporter.reportIssue(issue);
    }
    reporter.finish();

    final String json = baos.toString(StandardCharsets.UTF_8);
    return JsonParser.parseString(json).getAsJsonObject();
  }
}
