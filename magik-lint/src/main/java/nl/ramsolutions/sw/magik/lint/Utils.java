package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckList;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for {@link MagikLint}. */
final class Utils {

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  private Utils() {
    // Utility class
  }

  /**
   * Build {@link OpenedFile} for path.
   *
   * @param path Path to file
   * @return {@link OpenedFile} for path.
   * @throws IOException -
   */
  static OpenedFile buildOpenedFile(final Path path, final MagikToolsProperties properties) {
    try {
      final MagikToolsProperties fileProperties =
          ConfigurationReader.readProperties(path, properties);
      final URI uri = path.toUri();
      final Charset charset = FileCharsetDeterminer.determineCharset(path);
      final String fileContents = Files.readString(path, charset);

      if (SourceFileScanner.MAGIK_FILE_FILTER.test(path)) {
        return new MagikFile(fileProperties, uri, fileContents);
      } else if (SourceFileScanner.PRODUCT_DEF_FILE_FILTER.test(path)) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
        return new ProductDefFile(fileProperties, uri, fileContents, definitionKeeper, null);
      } else if (SourceFileScanner.MODULE_DEF_FILE_FILTER.test(path)) {
        final IDefinitionKeeper definitionKeeper = new DefinitionKeeper(false);
        return new ModuleDefFile(fileProperties, uri, fileContents, definitionKeeper, null);
      } else {
        throw new IllegalStateException("Unsupported file type: " + path);
      }
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  static boolean isFileIgnored(final OpenedFile openedFile) {
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final List<Class<? extends Check>> checks =
        Utils.getCheckListForOpenedFile(openedFile).getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final URI uri = openedFile.getUri();
    final Path path = Path.of(uri);
    final FileSystem fs = FileSystems.getDefault();
    final boolean isIgnored =
        checksConfig.getIgnores().stream()
            .map(fs::getPathMatcher)
            .anyMatch(matcher -> matcher.matches(path));
    if (isIgnored) {
      LOGGER.trace("Thread: {}, ignoring file: {}", Thread.currentThread().getName(), path);
    }
    return isIgnored;
  }

  static CheckList<?, ?> getCheckListForOpenedFile(final OpenedFile openedFile) {
    if (openedFile instanceof MagikFile) {
      return MagikCheckList.INSTANCE;
    } else if (openedFile instanceof ProductDefFile) {
      return ProductDefCheckList.INSTANCE;
    } else if (openedFile instanceof ModuleDefFile) {
      return ModuleDefCheckList.INSTANCE;
    } else {
      throw new IllegalStateException("Unsupported file type: " + openedFile.getClass());
    }
  }
}
