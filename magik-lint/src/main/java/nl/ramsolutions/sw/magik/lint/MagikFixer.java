package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.ConfigurationReader;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.checks.Check;
import nl.ramsolutions.sw.checks.CheckFixer;
import nl.ramsolutions.sw.checks.CheckHolder;
import nl.ramsolutions.sw.checks.ChecksConfiguration;
import nl.ramsolutions.sw.checks.MagikCheckList;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apply any fixes using the registered {@link CheckFixer}s. */
public class MagikFixer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikFixer.class);

  private final MagikToolsProperties properties;

  public MagikFixer(final MagikToolsProperties properties) {
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
      final MagikFile magikFile = this.buildMagikFile(path);
      if (this.isFileIgnored(magikFile)) {
        continue;
      }

      this.runOnFile(magikFile);
    }
  }

  private void runOnFile(final MagikFile originalMagikFile) throws IOException {
    MagikFile magikFile = originalMagikFile;
    final URI uri = magikFile.getUri();
    for (final CheckFixer fixer : this.getFixers(magikFile)) {
      final String newSource = this.applyFixer(fixer, magikFile);
      magikFile = new MagikFile(uri, newSource);
    }

    // Write file, if changed.
    final String newSource = magikFile.getSource();
    if (originalMagikFile.getSource().equals(newSource)) {
      return;
    }

    final Charset charset = FileCharsetDeterminer.determineCharset(newSource);
    final Path path = Path.of(uri);
    Files.writeString(path, newSource, charset);
  }

  private String applyFixer(final CheckFixer fixer, final MagikFile magikFile) {
    final String source = magikFile.getSource();
    final CodeActionApplier applier = new CodeActionApplier(source);
    final Range range =
        new Range(new Position(1, 0), new Position(Integer.MAX_VALUE, Integer.MAX_VALUE));
    final Comparator<TextEdit> byEndPosition =
        Comparator.comparing(textEdit -> textEdit.getRange().getEndPosition());
    fixer.provideCodeActions(magikFile, range).stream()
        .flatMap(codeAction -> codeAction.getEdits().stream())
        .sorted(byEndPosition.reversed())
        .forEach(applier::apply);
    return applier.getSource();
  }

  private List<CheckFixer> getFixers(final MagikFile magikFile) {
    final List<Class<? extends Check>> enabledChecks = this.getEnabledChecks(magikFile);
    return MagikCheckList.getFixers().entrySet().stream()
        .filter(entry -> enabledChecks.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .map(this::instantiateFixer)
        .toList();
  }

  private CheckFixer instantiateFixer(final Class<? extends CheckFixer> fixerClass) {
    try {
      return fixerClass.getDeclaredConstructor().newInstance();
    } catch (final ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /**
   * Build context for a file.
   *
   * @param path Path to file
   * @return Visitor context for file.
   * @throws IOException -
   */
  private MagikFile buildMagikFile(final Path path) throws IOException {
    final MagikToolsProperties fileProperties =
        ConfigurationReader.readProperties(path, this.properties);
    final URI uri = path.toUri();
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String fileContents = Files.readString(path, charset);
    return new MagikFile(fileProperties, uri, fileContents);
  }

  private List<Class<? extends Check>> getEnabledChecks(final MagikFile magikFile) {
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final List<Class<? extends Check>> checks = MagikCheckList.getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    return checksConfig.getAllChecks().stream()
        .filter(CheckHolder::isEnabled)
        .map(CheckHolder::getCheckClass)
        .collect(Collectors.toUnmodifiableList()); // NOSONAR: Keep VSCode/Java plugin sane.
  }

  private boolean isFileIgnored(final MagikFile magikFile) {
    final MagikToolsProperties fileProperties = magikFile.getProperties();
    final List<Class<? extends Check>> checks = MagikCheckList.getBaseChecks();
    final ChecksConfiguration checksConfig = new ChecksConfiguration(checks, fileProperties);
    final URI uri = magikFile.getUri();
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
}
