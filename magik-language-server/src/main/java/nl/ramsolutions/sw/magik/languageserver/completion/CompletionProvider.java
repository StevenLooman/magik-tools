package nl.ramsolutions.sw.magik.languageserver.completion;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nl.ramsolutions.sw.magik.MagikTypedFile;
import nl.ramsolutions.sw.magik.analysis.AstQuery;
import nl.ramsolutions.sw.magik.analysis.definitions.IDefinitionKeeper;
import nl.ramsolutions.sw.magik.api.MagikGrammar;
import nl.ramsolutions.sw.magik.api.MagikOperator;
import nl.ramsolutions.sw.magik.api.MagikPunctuator;
import nl.ramsolutions.sw.magik.languageserver.Lsp4jConversion;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ServerCapabilities;

/** Completion provider. Runs a set of {@link CompletionModule}s and aggregates their items. */
public class CompletionProvider {

  private static final Set<Character> REMOVAL_STOP_CHARS = new HashSet<>();

  private final List<CompletionModule> modules;
  private final CompletionModule fallbackModule;

  static {
    REMOVAL_STOP_CHARS.add(' ');
    REMOVAL_STOP_CHARS.add('\t');

    @SuppressWarnings("java:S1612")
    final Set<Character> punctuatorChars =
        Arrays.stream(MagikPunctuator.values())
            .map(MagikPunctuator::getValue)
            .flatMap(value -> value.chars().mapToObj(i -> (char) i))
            .collect(Collectors.toSet());
    REMOVAL_STOP_CHARS.addAll(punctuatorChars);

    @SuppressWarnings("java:S1612")
    final Set<Character> operatorChars =
        Arrays.stream(MagikOperator.values())
            .map(MagikOperator::getValue)
            .flatMap(value -> value.chars().mapToObj(i -> (char) i))
            .collect(Collectors.toSet());
    REMOVAL_STOP_CHARS.addAll(operatorChars);
  }

  /** Constructor. */
  public CompletionProvider() {
    this.modules = this.createModules();
    this.fallbackModule = this.createFallbackModule();
  }

  /**
   * Create the ordered contributor modules. Override to add or remove completion modules.
   *
   * @return Ordered contributor modules.
   */
  protected List<CompletionModule> createModules() {
    return List.of(new KeywordCompletionModule(), new MethodInvocationCompletionModule());
  }

  /**
   * Create the fallback module, used when no contributor module claims the context.
   *
   * @return Fallback module.
   */
  protected CompletionModule createFallbackModule() {
    return new GlobalCompletionModule();
  }

  /**
   * Set server capabilities.
   *
   * @param capabilities Server capabilities.
   */
  public void setCapabilities(final ServerCapabilities capabilities) {
    final List<String> triggerCharacters =
        Stream.concat(this.modules.stream(), Stream.of(this.fallbackModule))
            .flatMap(module -> module.getTriggerCharacters().stream())
            .distinct()
            .sorted()
            .toList();
    final CompletionOptions completionOptions = new CompletionOptions();
    completionOptions.setTriggerCharacters(triggerCharacters);
    capabilities.setCompletionProvider(completionOptions);
  }

  /**
   * Get a list of completions.
   *
   * @param magikFile Magik file.
   * @param position Position in file.
   * @return List of completions.
   */
  public List<CompletionItem> provideCompletions(
      final MagikTypedFile magikFile, final Position position) {
    // Do our best to get a token value, and clean up the source while we're at it.
    final Map.Entry<MagikTypedFile, String> usables = this.getUsableMagikFile(magikFile, position);
    final MagikTypedFile newMagikFile = usables.getKey();
    final AstNode newNode = newMagikFile.getTopNode();
    final String removedPart = usables.getValue();

    // Find token node at position in cleaned source.
    final Position newPositionLsp4j =
        new Position(position.getLine(), position.getCharacter() - removedPart.length());
    final nl.ramsolutions.sw.magik.Position newPosition =
        Lsp4jConversion.positionFromLsp4j(newPositionLsp4j);
    final AstNode tokenNode = this.getTokenNode(newNode, newPosition);

    // The range of the identifier being completed, from the ORIGINAL (un-cleaned) source, so that
    // completions can replace a package prefix like 'sw:' instead of duplicating it.
    final String[] sourceLines = magikFile.getSource().split("\n", -1);
    final String cursorLine =
        position.getLine() < sourceLines.length ? sourceLines[position.getLine()] : "";
    final CompletionContext context =
        new CompletionContext(
            newMagikFile,
            position,
            removedPart,
            tokenNode,
            CompletionUtils.identifierReplaceRange(cursorLine, position));

    // Run every contributor module; aggregate the items of those that claim the context.
    final List<CompletionItem> items = new ArrayList<>();
    boolean claimed = false;
    for (final CompletionModule module : this.modules) {
      final Optional<List<CompletionItem>> result = module.tryComplete(context);
      if (result.isPresent()) {
        claimed = true;
        items.addAll(result.get());
      }
    }
    if (claimed) {
      return items;
    }

    // Nothing claimed: fall back to the default (global) completion.
    return this.fallbackModule.tryComplete(context).orElseGet(List::of);
  }

  private AstNode getTokenNode(
      final AstNode node, final nl.ramsolutions.sw.magik.Position position) {
    final AstNode tokenNodeBefore = AstQuery.nodeBefore(node, position);
    final Token tokenBefore = tokenNodeBefore != null ? tokenNodeBefore.getToken() : null;
    final nl.ramsolutions.sw.magik.Position tokenBeforePosition =
        tokenBefore != null ? nl.ramsolutions.sw.magik.Position.fromTokenStart(tokenBefore) : null;
    final AstNode tokenNodeAt = AstQuery.nodeAt(node, position);
    final AstNode tokenNode;
    if (tokenNodeAt != null) {
      tokenNode = tokenNodeAt;
    } else if (tokenBeforePosition != null && tokenBeforePosition.getLine() == position.getLine()) {
      tokenNode = tokenNodeBefore;
    } else {
      tokenNode = null;
    }
    return tokenNode;
  }

  /**
   * Strip the current token at position.
   *
   * @param source Text to strip from.
   * @param position Position to strip.
   * @return Cleared source, removed token.
   */
  private String[] cleanSource(final String source, final Position position) {
    final int lineNo = position.getLine();
    final String[] lines = source.split("\n");
    final String line = lines[lineNo];

    // Replace current token.
    // Scan left up to, including: whitespace, MagikOperator, MagikPunctuator
    // Scan right up to, excluding: whitespace, MagikOperator, MagikPunctuator
    final int characterNo =
        position.getCharacter() >= line.length() ? line.length() - 1 : position.getCharacter();
    int beginIndex = characterNo;
    for (; beginIndex >= 0; --beginIndex) {
      final char chr = line.charAt(beginIndex);
      if (CompletionProvider.REMOVAL_STOP_CHARS.contains(chr)) {
        break;
      }
    }
    beginIndex = Math.max(beginIndex, 0);
    int endIndex = characterNo;
    for (; endIndex < line.length(); ++endIndex) {
      final char chr = line.charAt(endIndex);
      if (CompletionProvider.REMOVAL_STOP_CHARS.contains(chr)) {
        ++endIndex;
        break;
      }
    }

    // Clean up by replacing the scanned part with whitespace.
    final String stripped = line.substring(beginIndex, endIndex);
    lines[lineNo] =
        ""
            + line.substring(0, beginIndex)
            + " ".repeat(stripped.length())
            + line.substring(endIndex);
    return new String[] {Arrays.stream(lines).collect(Collectors.joining("\n")), stripped.trim()};
  }

  private Map.Entry<MagikTypedFile, String> getUsableMagikFile(
      final MagikTypedFile magikFile, final Position position) {
    MagikTypedFile newMagikFile = magikFile;

    final AstNode node = magikFile.getTopNode();
    final AstNode tokenNode = AstQuery.nodeAt(node, Lsp4jConversion.positionFromLsp4j(position));
    String cleanedToken = "";
    if (tokenNode != null && AstQuery.parentIs(tokenNode, MagikGrammar.SYNTAX_ERROR)) {
      // Clean it up a bit and try to re-parse.
      final String source = magikFile.getSource();
      final String[] items = this.cleanSource(source, position);
      final String cleanedSource = items[0];
      cleanedToken = items[1];
      final URI uri = magikFile.getUri();
      final IDefinitionKeeper definitionKeeper = magikFile.getDefinitionKeeper();
      newMagikFile = new MagikTypedFile(uri, cleanedSource, definitionKeeper);
    }

    return Map.entry(newMagikFile, cleanedToken);
  }
}
