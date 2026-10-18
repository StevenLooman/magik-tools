package nl.ramsolutions.sw.translation.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.impl.Parser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import nl.ramsolutions.sw.AstNodeHelper;
import nl.ramsolutions.sw.FileCharsetDeterminer;
import nl.ramsolutions.sw.translation.api.TranslationGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

/** Smallworld translation (.trn) file parser. */
public class TranslationParser {

  private static final String TRANSLATIONS_DIRECTORY = "translations";
  private static final String BASE_DIRECTORY = "base";
  private static final int PATH_PARTS_COUNT = 3;
  private static final int KEY_FIELDS_COUNT = 3;
  private static final Set<String> TYPES = Set.of("enumerator", "external_name", "field_value");
  private static final int MAX_FIELDS_WITHOUT_LANGUAGE = 5;
  private static final List<TranslationGrammar> FIELD_TYPES =
      List.of(
          TranslationGrammar.LANGUAGE,
          TranslationGrammar.SOURCE_TEXT,
          TranslationGrammar.TRANSLATED_TEXT);

  private final Parser<LexerlessGrammar> parser;

  /** Constructor with default charset. */
  public TranslationParser() {
    final LexerlessGrammar translationGrammar = TranslationGrammar.create();
    this.parser = new ParserAdapter<>(StandardCharsets.ISO_8859_1, translationGrammar);
  }

  /**
   * Parse a file and return the AstNode.
   *
   * @param path Path to file
   * @return Tree
   * @throws IOException -
   */
  public AstNode parse(final Path path) throws IOException {
    final Charset charset = FileCharsetDeterminer.determineCharset(path);
    final String source = Files.readString(path, charset);
    final URI uri = path.toUri();
    return this.parse(source, uri);
  }

  /**
   * Parse safe and set {@link URI}.
   *
   * @param source Source to parse.
   * @param uri URI to set.
   * @return Parsed source.
   */
  public AstNode parse(final String source, final URI uri) {
    final AstNode node = this.parser.parse(source);

    AstNodeHelper.setUri(node, uri);

    final boolean languageSpecific = TranslationParser.isLanguageSpecific(uri);
    final String[] lines = source.split("\\r\\n|\\n|\\r", -1);
    for (final AstNode translationNode : node.getChildren(TranslationGrammar.TRANSLATION)) {
      final String line = lines[translationNode.getTokenLine() - 1];
      this.convertFields(translationNode, languageSpecific, line.endsWith("\t"));
    }

    return node;
  }

  /**
   * Test if the file is specific to a language, i.e., is in the directory {@code
   * resources/<language>/translations}, where the language is not {@code base}.
   *
   * @param uri URI of file.
   * @return True if specific to a language, false otherwise.
   */
  public static boolean isLanguageSpecific(final URI uri) {
    final String path = uri.getPath();
    if (path == null) {
      return false;
    }

    final String[] parts = path.split("/");
    final int length = parts.length;
    return length >= PATH_PARTS_COUNT
        && parts[length - 2].equalsIgnoreCase(TRANSLATIONS_DIRECTORY)
        && !parts[length - PATH_PARTS_COUNT].equalsIgnoreCase(BASE_DIRECTORY);
  }

  /**
   * Convert the {@link TranslationGrammar#FIELD}s of a translation to what Smallworld reads these
   * as.
   */
  private void convertFields(
      final AstNode translationNode, final boolean languageSpecific, final boolean endsWithTab) {
    final List<AstNode> fieldNodes =
        translationNode.getChildren(TranslationGrammar.FIELD, TranslationGrammar.EMPTY_FIELD);
    final int fieldCount = KEY_FIELDS_COUNT + fieldNodes.size() + (endsWithTab ? 1 : 0);
    final boolean hasLanguage = !languageSpecific || fieldCount > MAX_FIELDS_WITHOUT_LANGUAGE;
    TranslationParser.convertFieldNodes(fieldNodes, hasLanguage ? 0 : 1);

    if (TranslationParser.isUnreadable(translationNode, hasLanguage && fieldNodes.isEmpty())) {
      AstNodeHelper.setType(translationNode, TranslationGrammar.SYNTAX_ERROR);
      AstNodeHelper.setName(translationNode, TranslationGrammar.SYNTAX_ERROR.toString());
    }
  }

  private static void convertFieldNodes(final List<AstNode> fieldNodes, final int offset) {
    for (int i = 0; i < fieldNodes.size(); ++i) {
      final AstNode fieldNode = fieldNodes.get(i);
      if (fieldNode.is(TranslationGrammar.EMPTY_FIELD)) {
        continue;
      }

      final int index = i + offset;
      final TranslationGrammar type =
          index < FIELD_TYPES.size() ? FIELD_TYPES.get(index) : TranslationGrammar.IGNORED_FIELD;
      AstNodeHelper.setType(fieldNode, type);
      AstNodeHelper.setName(fieldNode, type.toString());
    }
  }

  private static boolean isUnreadable(
      final AstNode translationNode, final boolean missingLanguage) {
    if (missingLanguage) {
      return true;
    }

    final String type = translationNode.getFirstChild(TranslationGrammar.TYPE).getTokenValue();
    return translationNode.getFirstChild(TranslationGrammar.TRANSLATED_TEXT) != null
        && !TYPES.contains(type);
  }
}
