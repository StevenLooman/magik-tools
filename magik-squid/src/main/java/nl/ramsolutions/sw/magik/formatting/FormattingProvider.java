package nl.ramsolutions.sw.magik.formatting;

import com.sonar.sslr.api.AstNode;
import java.lang.reflect.Constructor;
import java.util.List;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.TokenTriviaEditor;
import nl.ramsolutions.sw.magik.TextEdit;
import nl.ramsolutions.sw.magik.TextEditGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides formatting capabilities for Magik code. */
public class FormattingProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(FormattingProvider.class);

  private static final List<Class<? extends FormattingWalker>> PRE_WALKERS =
      List.of(
          // Base walkers.
          BaseFormattingWalker.class,

          // Specific walkers.
          SpecificsFormattingWalker.class,
          PragmaWalker.class);

  private static final List<Class<? extends FormattingWalker>> POST_WALKERS =
      List.of(
          // Newlines etc.
          MinMaxNewlinesWalker.class, TrailingWhitespaceWalker.class, FinalNewlineWalker.class);

  private final FormattingOptions options;
  private final Class<? extends FormattingWalker> indentWalker;

  /**
   * Constructor for the formatting provider.
   *
   * @param options The {@link FormattingOptions} to use.
   * @param indentWalker Indent walker to apply.
   */
  public FormattingProvider(
      final FormattingOptions options, final Class<? extends FormattingWalker> indentWalker) {
    this.options = options;
    this.indentWalker = indentWalker;
  }

  /**
   * Formats the given AST node.
   *
   * @param node The root {@link AstNode} of the AST to format.
   * @return A list of {@link TextEdit}s representing the formatting changes.
   */
  public List<TextEdit> format(final AstNode node) {
    LOGGER.debug("Formatting node: {}", node);
    LOGGER.debug("  tab size: {}", this.options.getTabSize());
    LOGGER.debug("  insert spaces: {}", this.options.isInsertSpaces());
    LOGGER.debug("  trim trailing whitespace: {}", this.options.isTrimTrailingWhitespace());
    LOGGER.debug("  insert final newline: {}", this.options.isInsertFinalNewline());
    LOGGER.debug("  trim final newlines: {}", this.options.isTrimFinalNewlines());

    final AstNode nodeCopy = AstNodeHelper.clone(node);
    final TokenTriviaEditor tokenEditor = new TokenTriviaEditor(nodeCopy);

    // Apply pre walkers.
    FormattingProvider.PRE_WALKERS.stream()
        .map(
            clazz -> {
              try {
                final Constructor<? extends FormattingWalker> constructor =
                    clazz.getDeclaredConstructor(FormattingOptions.class, TokenTriviaEditor.class);
                return (FormattingWalker) constructor.newInstance(this.options, tokenEditor);
              } catch (final ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
              }
            })
        .forEach(walker -> walker.walkAst(nodeCopy));

    // Apply indent walker.
    try {
      final Constructor<? extends FormattingWalker> constructor =
          this.indentWalker.getDeclaredConstructor(
              FormattingOptions.class, TokenTriviaEditor.class);
      final FormattingWalker walker =
          (FormattingWalker) constructor.newInstance(this.options, tokenEditor);
      walker.walkAst(nodeCopy);
    } catch (final ReflectiveOperationException exception) {
      throw new RuntimeException(exception);
    }

    // Apply post walkers.
    FormattingProvider.POST_WALKERS.stream()
        .map(
            clazz -> {
              try {
                final Constructor<? extends FormattingWalker> constructor =
                    clazz.getDeclaredConstructor(FormattingOptions.class, TokenTriviaEditor.class);
                return (FormattingWalker) constructor.newInstance(this.options, tokenEditor);
              } catch (final ReflectiveOperationException exception) {
                throw new RuntimeException(exception);
              }
            })
        .forEach(walker -> walker.walkAst(nodeCopy));

    // Return the edits.
    final TextEditGenerator textEditGenerator = new TextEditGenerator();
    return textEditGenerator.generateEdits(node, nodeCopy);
  }
}
