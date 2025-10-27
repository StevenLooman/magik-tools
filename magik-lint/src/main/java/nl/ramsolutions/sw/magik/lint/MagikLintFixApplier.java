package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.CodeActionSupplier;
import nl.ramsolutions.sw.magik.CodeAction;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.moduledef.ModuleDefFile;
import nl.ramsolutions.sw.productdef.ProductDefFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apply {@link CodeAction}s using the registered {@link CodeActionSupplier}s. */
public class MagikLintFixApplier {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikLint.class);

  private final MagikToolsProperties properties;

  public MagikLintFixApplier(final MagikToolsProperties properties) {
    this.properties = properties;
  }

  /**
   * Run on all the files.
   *
   * @param paths Paths to run on.
   * @throws IOException -
   */
  public void run(final Collection<Path> paths) throws IOException {
    for (final Path path : paths) {
      final OpenedFile openedFile = Utils.buildOpenedFile(path, this.properties);
      if (Utils.isFileIgnored(openedFile)) {
        continue;
      }

      this.runOnFile(openedFile);
    }
  }

  private void runOnFile(final OpenedFile originalOpenedFile) throws IOException {
    LOGGER.trace("Applying fixers to file: {}", originalOpenedFile);

    OpenedFile openedFile = originalOpenedFile;
    final URI uri = openedFile.getUri();
    for (final CodeActionSupplier codeActionSupplier : this.getCodeActionSuppliers(openedFile)) {
      LOGGER.trace("Applying code action supplier: {}", codeActionSupplier.getClass().getName());
      final String newSource = this.applyCodeActionSupplier(codeActionSupplier, openedFile);
      openedFile = this.createNewFile(originalOpenedFile, newSource);
    }

    // Write file, if changed.
    final String newSource = openedFile.getSource();
    if (originalOpenedFile.getSource().equals(newSource)) {
      return;
    }

    LOGGER.debug("Saving file: {}", openedFile);
    final Charset charset = FileCharsetDeterminer.determineCharset(newSource);
    final Path path = Path.of(uri);
    Files.writeString(path, newSource, charset);
  }

  private String applyCodeActionSupplier(
      final CodeActionSupplier codeActionSupplier, final OpenedFile openedFile) {
    final String source = openedFile.getSource();
    final CodeActionApplier applier = new CodeActionApplier(source);
    final Range range =
        new Range(new Position(1, 0), new Position(Integer.MAX_VALUE, Integer.MAX_VALUE));
    final Comparator<TextEdit> byEndPosition =
        Comparator.comparing(textEdit -> textEdit.getRange().getEndPosition());
    codeActionSupplier.provideCodeActions(openedFile, range).stream()
        .flatMap(codeAction -> codeAction.getEdits().stream())
        .sorted(byEndPosition.reversed())
        .forEach(applier::apply);
    return applier.getSource();
  }

  private List<CodeActionSupplier> getCodeActionSuppliers(final OpenedFile openedFile) {
    final List<Class<? extends Check>> enabledChecks = this.getEnabledChecks(openedFile);
    return Utils.getCheckListForOpenedFile(openedFile).getFixers().entrySet().stream()
        .filter(entry -> enabledChecks.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .map(this::instantiateCodeActionSupplier)
        .toList();
  }

  private CodeActionSupplier instantiateCodeActionSupplier(
      final Class<? extends CodeActionSupplier> fixerClass) {
    try {
      return fixerClass.getDeclaredConstructor().newInstance();
    } catch (final ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private List<Class<? extends Check>> getEnabledChecks(final OpenedFile openedFile) {
    final MagikToolsProperties fileProperties = openedFile.getProperties();
    final List<Class<? extends Check>> checks =
        Utils.getCheckListForOpenedFile(openedFile).getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    return checksConfig.getAllChecks().stream()
        .filter(CheckHolder::isEnabled)
        .map(CheckHolder::getCheckClass)
        .collect(Collectors.toUnmodifiableList()); // NOSONAR: Keep VSCode/Java plugin sane.
  }

  private OpenedFile createNewFile(final OpenedFile openedFile, final String newSource) {
    final URI uri = openedFile.getUri();
    if (openedFile instanceof ProductDefFile productDefFile) {
      final IDefinitionKeeper definitionKeeper = productDefFile.getDefinitionKeeper();
      return new ProductDefFile(uri, newSource, definitionKeeper, null);
    } else if (openedFile instanceof ModuleDefFile moduleDefFile) {
      final IDefinitionKeeper definitionKeeper = moduleDefFile.getDefinitionKeeper();
      return new ModuleDefFile(uri, newSource, definitionKeeper, null);
    } else if (openedFile instanceof MagikFile) {
      return new MagikFile(uri, newSource);
    }

    throw new IllegalStateException("Unsupported file type: " + openedFile.getClass());
  }
}
