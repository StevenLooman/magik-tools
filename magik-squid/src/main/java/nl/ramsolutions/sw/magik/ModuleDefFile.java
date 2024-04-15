package nl.ramsolutions.sw.magik;

import com.sonar.sslr.api.AstNode;
import java.net.URI;
import nl.ramsolutions.sw.OpenedFile;
import nl.ramsolutions.sw.definitions.parser.SwModuleDefParser;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;

public class ModuleDefFile extends OpenedFile {

  private static final URI DEFAULT_URI = URI.create("memory://module.def");
  public static final Location DEFAULT_LOCATION = new Location(DEFAULT_URI, Range.DEFAULT_RANGE);

  private final IDefinitionKeeper definitionKeeper;
  private AstNode astNode;

  /**
   * Constructor.
   *
   * @param uri URI.
   * @param source Source.
   */
  public ModuleDefFile(
      final URI uri, final String source, final IDefinitionKeeper definitionKeeper) {
    super(uri, source);
    this.definitionKeeper = definitionKeeper;
  }

  /**
   * Parse the text for this file and return the top level {@link AstNode}.
   *
   * @return Top level {@link AstNode}.
   */
  public synchronized AstNode getTopNode() {
    if (this.astNode == null) {
      final SwModuleDefParser parser = new SwModuleDefParser();
      final String source = this.getSource();
      final URI uri = this.getUri();
      this.astNode = parser.parse(source, uri);
    }

    return this.astNode;
  }

  /** Get the {@link IDefinitionKeeper}. */
  public IDefinitionKeeper getDefinitionKeeper() {
    return this.definitionKeeper;
  }

  @Override
  public String getLanguageId() {
    return "module.def";
  }
}
