package nl.ramsolutions.sw.magik;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import nl.ramsolutions.sw.magik.analysis.MagikAnalysisConfiguration;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.analysis.typing.TypeStringResolver;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasoner;
import nl.ramsolutions.sw.magik.analysis.typing.reasoner.LocalTypeReasonerState;

/** Typed magik file. */
public class MagikTypedFile extends MagikFile {

  private final IDefinitionKeeper definitionKeeper;
  private final TypeStringResolver typeStringResolver;
  private LocalTypeReasonerState reasonerState;

  /**
   * Constructor.
   *
   * @param configuration Magik analysis configuration.
   * @param uri URI.
   * @param text Text.
   * @param definitionKeeper {@link IDefinitionKeeper}.
   */
  public MagikTypedFile(
      final MagikAnalysisConfiguration configuration,
      final URI uri,
      final String text,
      final IDefinitionKeeper definitionKeeper) {
    super(configuration, uri, text);
    this.definitionKeeper = definitionKeeper;
    this.typeStringResolver = new TypeStringResolver(definitionKeeper);
  }

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param text Text.
   * @param definitionKeeper {@link IDefinitionKeeper}.
   */
  public MagikTypedFile(
      final URI uri, final String text, final IDefinitionKeeper definitionKeeper) {
    super(uri, text);
    this.definitionKeeper = definitionKeeper;
    this.typeStringResolver = new TypeStringResolver(definitionKeeper);
  }

  /**
   * Constructor.
   *
   * @param path Path.
   * @param definitionKeeper {@link IDefinitionKeeper}.
   * @throws IOException -
   */
  public MagikTypedFile(final Path path, final IDefinitionKeeper definitionKeeper)
      throws IOException {
    super(path);
    this.definitionKeeper = definitionKeeper;
    this.typeStringResolver = new TypeStringResolver(definitionKeeper);
  }

  /** Get the {@link IDefinitionKeeper}. */
  public IDefinitionKeeper getDefinitionKeeper() {
    return this.definitionKeeper;
  }

  /**
   * Get the {@link TypeStringResolver}.
   *
   * @return
   */
  public TypeStringResolver getTypeStringResolver() {
    return this.typeStringResolver;
  }

  /**
   * Get the resulting state from the {@link LocalTypeReasoner}.
   *
   * @return The {@link LocalTypeReasonerState}.
   */
  public synchronized LocalTypeReasonerState getTypeReasonerState() {
    if (this.reasonerState == null) {
      final LocalTypeReasoner reasoner = new LocalTypeReasoner(this);
      reasoner.run();
      this.reasonerState = reasoner.getState();
    }

    return this.reasonerState;
  }

  @Override
  public String toString() {
    return String.format(
        "%s@%s(%s)",
        this.getClass().getName(), Integer.toHexString(this.hashCode()), this.getUri());
  }
}
