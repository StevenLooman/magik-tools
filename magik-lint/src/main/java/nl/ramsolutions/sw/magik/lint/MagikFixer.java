package nl.ramsolutions.sw.magik.lint;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import nl.ramsolutions.sw.ConfigurationLocator;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.magik.MagikFile;
import nl.ramsolutions.sw.magik.Position;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.checks.CheckList;
import nl.ramsolutions.sw.magik.checks.MagikCheck;
import nl.ramsolutions.sw.magik.checks.MagikCheckFixer;
import nl.ramsolutions.sw.magik.checks.MagikCheckHolder;
import nl.ramsolutions.sw.magik.checks.MagikChecksConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Apply any fixes using the registered {@link MagikCheckFixer}s. */
public class MagikFixer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MagikFixer.class);

  private final MagikLintConfiguration config;

  public MagikFixer(final MagikLintConfiguration config) {
    this.config = config;
  }

  /**
   * Run on all the files.
   *
   * @param paths Paths to run on.
   * @throws IOException -
   */
  public void run(final Collection<Path> paths) throws IOException {
    for (final Path path : paths) {
      if (this.isFileIgnored(path)) {
        continue;
      }

      this.runOnFile(path);
    }
  }

  private void runOnFile(final Path path) throws IOException {
    final MagikFile originalMagikFile = this.buildMagikFile(path);
    MagikFile magikFile = originalMagikFile;
    final URI uri = magikFile.getUri();
    for (final MagikCheckFixer fixer : this.getFixers(magikFile)) {
      final String newSource = this.applyFixer(fixer, magikFile);
      magikFile = new MagikFile(uri, newSource);
    }

    // Write file, if changed.
    final String newSource = magikFile.getSource();
    if (originalMagikFile.getSource().equals(newSource)) {
      return;
    }

    final Charset charset = FileCharsetDeterminer.determineCharset(newSource);
    Files.writeString(path, newSource, charset);
  }

  private String applyFixer(final MagikCheckFixer fixer, final MagikFile magikFile) {
    final String source = magikFile.getSource();
    final CodeActionApplier applier = new CodeActionApplier(source);
    final Range range =
        new Range(new Position(1, 0), new Position(Integer.MAX_VALUE, Integer.MAX_VALUE));
    fixer.provideCodeActions(magikFile, range).stream().forEach(applier::applyCodeAction);
    return applier.getSource();
  }

  private List<MagikCheckFixer> getFixers(final MagikFile magikFile) {
    final List<Class<? extends MagikCheck>> enabledChecks = this.getEnabledChecks(magikFile);
    return CheckList.getFixers().entrySet().stream()
        .filter(entry -> enabledChecks.contains(entry.getKey()))
        .flatMap(entry -> entry.getValue().stream())
        .map(this::instantiateFixer)
        .toList();
  }

  private MagikCheckFixer instantiateFixer(final Class<? extends MagikCheckFixer> fixerClass) {
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
  private MagikFile buildMagikFile(final Path path) {
    final Charset charset = FileCharsetDeterminer.determineCharset(path);

    byte[] encoded = null;
    try {
      encoded = Files.readAllBytes(path);
    } catch (final IOException exception) {
      LOGGER.error(exception.getMessage(), exception);
    }

    final URI uri = path.toUri();
    final String fileContents = new String(encoded, charset);
    final MagikAnalysisConfiguration configuration;
    try {
      configuration = new MagikAnalysisConfiguration();
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }

    return new MagikFile(configuration, uri, fileContents);
  }

  private List<Class<? extends MagikCheck>> getEnabledChecks(final MagikFile magikFile) {
    final URI uri = magikFile.getUri();
    final Path path = Path.of(uri);
    final MagikChecksConfiguration checksConfig = this.getChecksConfig(path);
    return checksConfig.getAllChecks().stream()
        .filter(MagikCheckHolder::isEnabled)
        .map(MagikCheckHolder::getCheckClass)
        .collect(Collectors.toUnmodifiableList()); // NOSONAR: Keep VSCode/Java plugin sane.
  }

  private boolean isFileIgnored(final Path path) {
    final MagikChecksConfiguration checksConfig = this.getChecksConfig(path);
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

  private MagikChecksConfiguration getChecksConfig(final Path path) {
    final Path configPath =
        this.config.getPath() != null
            ? this.config.getPath()
            : ConfigurationLocator.locateConfiguration(path);
    try {
      return configPath != null
          ? new MagikChecksConfiguration(CheckList.getChecks(), configPath)
          : new MagikChecksConfiguration(CheckList.getChecks());
    } catch (final IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
