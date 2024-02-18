package nl.ramsolutions.sw.sonar.sensors.cpd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.sonar.language.Magik;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.cpd.internal.TokensLine;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

/** Tests for CpdTokenSaver. */
class CpdTokenSaverTest {

  private static final Path TEST_PRODUCT_PATH = Path.of("src/test/resources/test_product");

  @SuppressWarnings("checkstyle:MagicNumber")
  @Test
  void testSyntaxError() throws IOException {
    final SensorContextTester context = SensorContextTester.create(TEST_PRODUCT_PATH);
    final DefaultFileSystem fileSystem = context.fileSystem();

    final Path filePath = TEST_PRODUCT_PATH.resolve("test_module/test.magik");
    final String fileContents =
        Files.readString(
            TEST_PRODUCT_PATH.resolve("test_module/test.magik"), StandardCharsets.ISO_8859_1);
    @SuppressWarnings("deprecation")
    final InputFile inputFile =
        TestInputFileBuilder.create("moduleKey", "test.magik")
            .setModuleBaseDir(filePath)
            .setCharset(StandardCharsets.ISO_8859_1)
            .setType(Type.MAIN)
            .setLanguage(Magik.KEY)
            .setContents(fileContents)
            .setStatus(InputFile.Status.ADDED)
            .build();
    fileSystem.add(inputFile);

    final CpdTokenSaver tokenSaver = new CpdTokenSaver(context);
    final URI uri = inputFile.uri();
    final String fileContent = inputFile.contents();
    final MagikFile magikFile = new MagikFile(uri, fileContent);
    tokenSaver.saveCpdTokens(inputFile, magikFile);

    final List<TokensLine> cpdTokens = context.cpdTokens("moduleKey:test.magik");
    assertThat(cpdTokens).hasSize(16);
  }
}
