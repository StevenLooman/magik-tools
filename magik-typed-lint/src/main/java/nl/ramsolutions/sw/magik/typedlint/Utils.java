package nl.ramsolutions.sw.magik.typedlint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.CheckList;
import nl.ramsolutions.sw.checks.MagikTypedCheckList;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

/** Utility class for {@link MagikTypedLint}. */
final class Utils {

  private Utils() {
    // Utility class
  }

  /**
   * Build {@link MagikTypedFile} for path.
   *
   * @param path Path to file
   * @return {@link MagikTypedFile} for path.
   * @throws IOException -
   */
  static MagikTypedFile buildOpenedFile(
      final Path path,
      final MagikToolsProperties properties,
      final IDefinitionKeeper definitionKeeper) {
    try {
      final MagikToolsProperties fileProperties =
          ConfigurationReader.readProperties(path, properties);
      final URI uri = path.toAbsolutePath().normalize().toUri();
      final Charset charset = FileCharsetDeterminer.determineCharset(path);
      final String fileContents = Files.readString(path, charset);

      return new MagikTypedFile(fileProperties, uri, fileContents, definitionKeeper);
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  static CheckList<?, ?> getCheckListForOpenedFile(final OpenedFile openedFile) {
    if (openedFile instanceof MagikTypedFile) {
      return MagikTypedCheckList.INSTANCE;
    }

    throw new IllegalStateException("Unsupported file type: " + openedFile.getClass());
  }
}
