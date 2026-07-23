package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.SourceFileScanner;
import nl.ramsolutions.sw.checks.CheckList;
import nl.ramsolutions.sw.checks.LoadListCheckList;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.checks.ModuleDefCheckList;
import nl.ramsolutions.sw.checks.ProductDefCheckList;
import nl.ramsolutions.sw.loadlist.LoadListFile;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.analysis.definitions.DefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;

/** Utility class for {@link MagikLint}. */
final class Utils {

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
      final URI uri = path.toAbsolutePath().normalize().toUri();
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
      } else if (SourceFileScanner.LOAD_LIST_FILE_FILTER.test(path)) {
        return new LoadListFile(fileProperties, uri, fileContents);
      } else {
        throw new IllegalStateException("Unsupported file type: " + path);
      }
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }

  static CheckList<?, ?> getCheckListForOpenedFile(final OpenedFile openedFile) {
    if (openedFile instanceof MagikFile) {
      return MagikCheckList.INSTANCE;
    } else if (openedFile instanceof ProductDefFile) {
      return ProductDefCheckList.INSTANCE;
    } else if (openedFile instanceof ModuleDefFile) {
      return ModuleDefCheckList.INSTANCE;
    } else if (openedFile instanceof LoadListFile) {
      return LoadListCheckList.INSTANCE;
    }

    throw new IllegalStateException("Unsupported file type: " + openedFile.getClass());
  }
}
