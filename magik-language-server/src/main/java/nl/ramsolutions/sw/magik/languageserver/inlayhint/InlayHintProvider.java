package nl.ramsolutions.sw.magik.languageserver.inlayhint;

import java.util.List;
import nl.ramsolutions.sw.MagikToolsProperties;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.Range;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.ServerCapabilities;

/**
 * Inlay hint provider. Runs a set of {@link InlayHintModule}s and combines all of their results,
 * unlike {@code HoverProvider}/{@code DefinitionsProvider}/{@code ReferencesProvider}, which use
 * only the first module that claims the context.
 */
public class InlayHintProvider {

  private final MagikToolsProperties properties;
  private final List<InlayHintModule> modules;

  /**
   * Constructor.
   *
   * @param properties Properties.
   */
  public InlayHintProvider(final MagikToolsProperties properties) {
    this.properties = properties;
    this.modules = this.createModules();
  }

  /**
   * Create the ordered modules for Magik files. Override to add or remove inlay hint modules.
   *
   * @return Ordered modules.
   */
  protected List<InlayHintModule> createModules() {
    return List.of(
        new ArgumentNameInlayHintModule(this.properties),
        new AtomTypingInlayHintModule(this.properties),
        new InvocationTypingInlayHintModule(this.properties));
  }

  /**
   * Set capabilities for inlay hints.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    capabilities.setInlayHintProvider(true);
  }

  /**
   * Provide inlay hints for the given file.
   *
   * @param magikFile Magik file.
   * @param lsp4jrange Range in file.
   * @return List of inlay hints.
   */
  public List<InlayHint> provideInlayHints(
      final MagikTypedFile magikFile, final org.eclipse.lsp4j.Range lsp4jrange) {
    final Range range = Lsp4jConversion.rangeFromLsp4j(lsp4jrange);
    final InlayHintContext context = new InlayHintContext(magikFile, range);
    return this.modules.stream().flatMap(module -> module.provideInlayHints(context)).toList();
  }
}
