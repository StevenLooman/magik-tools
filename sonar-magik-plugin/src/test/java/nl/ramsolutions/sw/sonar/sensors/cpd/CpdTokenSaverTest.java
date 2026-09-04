package nl.ramsolutions.sw.sonar.sensors.cpd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.sonar.RecordingCpdTokens;
import nl.ramsolutions.sw.sonar.RecordingCpdTokens.CpdToken;
import nl.ramsolutions.sw.sonar.StubInputFile;
import nl.ramsolutions.sw.sonar.StubSensorContext;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;

/** Test {@link CpdTokenSaver}. */
class CpdTokenSaverTest {

  private static final Path TEST_PRODUCT_PATH = Path.of("src/test/resources/test_product");

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testSyntaxError() throws IOException {
    final Path filePath = TEST_PRODUCT_PATH.resolve("test_module/test.magik");
    final String fileContents = Files.readString(filePath, StandardCharsets.ISO_8859_1);
    final InputFile inputFile =
        new StubInputFile(
            "moduleKey:test.magik", filePath, StandardCharsets.ISO_8859_1, fileContents);

    final StubSensorContext context = new StubSensorContext();
    final CpdTokenSaver tokenSaver = new CpdTokenSaver(context);
    final MagikFile magikFile = new MagikFile(inputFile.uri(), fileContents);
    tokenSaver.saveCpdTokens(inputFile, magikFile);

    final RecordingCpdTokens cpdTokens = context.getCpdTokens();
    assertThat(cpdTokens.getInputFile()).isSameAs(inputFile);
    assertThat(cpdTokens.isSaved()).isTrue();

    final List<CpdToken> tokens = cpdTokens.getTokens();
    assertThat(tokens)
        .isSortedAccordingTo(
            Comparator.comparingInt(CpdToken::startLine)
                .thenComparingInt(CpdToken::startLineOffset));
    assertThat(tokens).extracting(CpdToken::value).noneMatch(String::isBlank);

    final List<Integer> lines = tokens.stream().map(CpdToken::startLine).distinct().toList();
    assertThat(lines).hasSize(16);
  }
}
